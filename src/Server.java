import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable{

    private ArrayList<ConnectionHandler> connections;
    private ServerSocket serverSocket;
    private boolean done;
    private ExecutorService pool;

    public Server() {
        connections = new ArrayList<>();
        done = false;
    }

    @Override
    public void run() {

        try {
            serverSocket = new ServerSocket(8888);
            pool = Executors.newCachedThreadPool();
            while (!done) {
                Socket client = serverSocket.accept();
                ConnectionHandler handler = new ConnectionHandler(client);
                connections.add(handler);
                pool.execute(handler);
            }

        } catch (IOException e) {
            System.out.println("An error occurred! -> "+e);
            shutDown();
        }
    }

    public void broadCast(String message) {
        for (ConnectionHandler ch : connections) {
            if (ch != null) {
                ch.sentMessage(message);
            }
        }
    }

    public void shutDown() {
        try {
            done = true;
            if (!serverSocket.isClosed()) {
                serverSocket.close();
            }
            for (ConnectionHandler ch : connections) {
                // TODO: shutdown for each
            }
        } catch (IOException e) {
            System.out.println(e);
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
            try{

                out = new PrintWriter(client.getOutputStream(),true);
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));

                out.println("Enter a username : ");
                username = in.readLine();
                /**
                 -> we can create a checker for the nickname (fe. does it equal null, does it contain smthng etc...)
                 */

                System.out.println(username+" connected!");
                broadCast(username+" joined the chat!");

                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("/nick ")) {
                        String[] messaegSplit = message.split(" ",2);
                        if (messaegSplit.length == 2) {
                            broadCast(username+" renamed themselves to "+ messaegSplit[1]);
                            System.out.println(username+" renamed themselves to "+ messaegSplit[1]);
                            username = messaegSplit[1];
                            out.println("Username has been changed to : "+username);
                        } else {
                            out.println("No username has been provided!");
                        }
                    } else if (message.startsWith("/quit ")) {
                        broadCast((username+" left the chat!"));
                        shutDown();
                    } else {
                        broadCast(username+ " : "+ message);
                    }
                }

            } catch (IOException e) {
                System.out.println(e);
                shutDown();
            }
        }

        public void sentMessage(String message) {
            out.println(message);
        }

        public void shutDown(){
            try {
                in.close();
                out.close();
                if (!client.isClosed()) {
                    client.close();
                }
            } catch (IOException e) {
                System.out.println(e);
            }
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.run();
    }
}
