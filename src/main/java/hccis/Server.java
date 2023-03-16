package hccis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable {

    private ArrayList<ConnectionHandler> connections;
    private ServerSocket server;
    private boolean done;
    private ExecutorService pool;

    public Server() {
        connections = new ArrayList<>();
        done = false;
    }

    @Override
    public void run() {
        try {
            server = new ServerSocket(9999);
            pool = Executors.newCachedThreadPool();
            while (!done) {
                Socket client = server.accept();
                ConnectionHandler handler = new ConnectionHandler(client);
                connections.add(handler);
                pool.execute(handler);
            }
        } catch (Exception e) {
            shutdown();
        }
    }

    public void broadcast(String message) {
        for (ConnectionHandler ch : connections) {
            if (ch != null)
                ch.sendMessage(message);
        }
    }

    public void shutdown() {
        try {
            done = true;
            pool.shutdown();
            if (!server.isClosed()) {
                server.close();
            }
            for (ConnectionHandler ch : connections) {
                ch.shutdown();
            }
        } catch (IOException e) {
            // ignore
        }
    }

    class ConnectionHandler implements Runnable {

        private Socket client;
        private BufferedReader in;
        private PrintWriter out;
        private String username;

        public ConnectionHandler(Socket client) {
            this.client = client;
        }

        @Override
        public void run() {
            try {

                in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                out = new PrintWriter(client.getOutputStream(), true);

                out.println("Please enter a username:");
                username = in.readLine();

                System.out.println("User, " + username + " has connected.");
                broadcast(username + " has joined the chat!");

                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("/nick ")) {
                        String[] messageSplit = message.split(" ", 2);
                        if (messageSplit.length == 2) {
                            broadcast(username + " has changed their name to " + messageSplit[1]);
                            System.out.println(username + " has changed their name to " + messageSplit[1]);
                            username = messageSplit[1];
                            out.println("Your username has been changed to " + username);
                        } else {
                            out.println("No nickname provided.");
                        }
                    } else if (message.startsWith("/quit ")) {
                        broadcast(username + " has left the chat.");
                        shutdown();
                    } else if (message.startsWith("/msg ")) {
                        String[] messageSplit = message.split(" ", 3);
                        if (messageSplit.length == 3) {
                            for (ConnectionHandler ch : connections) {
                                if (ch.username.equals(messageSplit[1])) {
                                    ch.sendMessage(username + " (private): " + messageSplit[2]);
                                    out.println(username + " (private): " + messageSplit[2]);
                                }
                            }
                        } else {
                            out.println("No message provided.");
                        }

                    } else {
                        broadcast(username + ": " + message);
                    }
                }
            } catch (IOException e) {
                shutdown();
            }
        }

        public void sendMessage(String message) {
            out.println(message);
        }

        public void shutdown() {
            try {
                in.close();
                out.close();
                if (!client.isClosed()) {
                    client.close();
                }
            } catch (IOException e) {
                // ignore
            }
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.run();
    }

}
