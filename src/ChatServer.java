import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

// Chat server responsible for receiving messages from clients and broadcasting them to all clients connected to the server.
public class ChatServer {
    static List<Socket> clients = new ArrayList<>(); // Connected clients to the server

    public static void main(String[] args) {
        try {
            // Add a unique identifier for each client on connection
            System.out.println("------The server is up and running to receive clients------");
            ServerSocket server = new ServerSocket(4242);
            while (true) {
                Socket client = server.accept();
                clients.add(client);
                ClientConnection clientConnection = new ClientConnection(client);
                Thread clientThread = new Thread(clientConnection);
                clientThread.start();
                printClients();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // Print out the connected clients to the server.
    static void printClients() {
		System.out.println("***Connected Clients***");
        for (Socket connection : clients) {
            System.out.println(connection.toString());
        }
		System.out.println("");
    }
}

// The runnable class dedicated for each client thus making it multi-threaded.
class ClientConnection implements Runnable {
    Socket client = null;

    ClientConnection (Socket client) {
        this.client = client;
    }

    // Start listening to the client to receive messages.
    static void listenToClient(Socket client) {
        try {
            InputStream messageReader = client.getInputStream();
            receiveMessage(messageReader, client);
            System.out.println("Client closed connection (" + client.toString() + ")");
            removeClient(client);
        } catch (SocketException socketException) {
            System.out.println("Lost connection with client! (" + client.toString() + ")");
            removeClient(client);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    // Get the messages from the client (chunks of 32KB).
    static void receiveMessage(InputStream messageReader, Socket client) throws IOException {
        int messageMaxSize = 32 * 1024;
        byte[] messageBuffer = new byte[messageMaxSize];
        int responseLen = -1;

        while ((responseLen = messageReader.read(messageBuffer)) != -1) {
            String message = new String(messageBuffer, 0, responseLen, StandardCharsets.UTF_8);
            System.out.println(message);
            broadcastMessage(message, client);
        }
    }

    // Remove the client form the list of connected clients.
    static void removeClient(Socket client) {
        try {
            ChatServer.clients.remove(client);
            client.close();
            ChatServer.printClients();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    // Send the incoming message from any of the clients to the other clients connected.
    static void broadcastMessage(String message, Socket client) throws IOException {
        for (Socket connection:ChatServer.clients) {
            if(!connection.equals(client)) {
                OutputStream messageWriter = connection.getOutputStream();
                messageWriter.write(message.getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    @Override
    public void run() {
        listenToClient(client);
    }
}