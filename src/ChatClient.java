import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

// Chat for the client to connect to the chat server and send/receive messages from the other clients.
public class ChatClient {
    public static void main(String[] args) throws IOException, InterruptedException {
        InetAddress inetAddress = InetAddress.getByName("localhost");
        int port = 4242;
        SocketAddress socketAddress = new InetSocketAddress(inetAddress, port);
        Socket socket = new Socket();

        while (!socket.isConnected()) {
            try {
                socket = new Socket();
                socket.connect(socketAddress);
            } catch (ConnectException ex) {
                System.out.println("Failed to connect to the server, retrying in 10 seconds!");
                Thread.sleep(10000);
            }
        }

        if (socket.isConnected()) {
            System.out.println("Successfully connected to the server!");
            Runnable RunnableMessageReceiver = new RunnableMessageReceiver(socket);
            Runnable RunnableMessageSender = new RunnableMessageSender(socket);
            Thread messageListenerThread = new Thread(RunnableMessageReceiver);
            Thread messageSenderThread = new Thread(RunnableMessageSender);
            messageListenerThread.start();
            messageSenderThread.start();
            messageListenerThread.join();
        }

        System.out.println("Server is down, closing chat application.");
        System.exit(0);
    }

    // Runnable responsible to send messages to the server.
    public static class RunnableMessageSender implements Runnable {
        private Socket socket;

        public RunnableMessageSender(Socket socket) {
            this.socket = socket;
        }

        // Send messages to the chat server.
        public void sendMessages(Socket tcpSocket) {
            try {
                OutputStream toServerOutputStream = tcpSocket.getOutputStream();
                Scanner scanner = new Scanner(System.in);

                while (!tcpSocket.isClosed()) {
                    String message = scanner.nextLine();
                    toServerOutputStream.write(message.getBytes(StandardCharsets.UTF_8));
                }

                toServerOutputStream.close();
                tcpSocket.close();
            } catch (IOException ex) {
                System.out.println("Info from message sender: Server connection closed.");
            }
        }

        public void run() {
            sendMessages(this.socket);
        }
    }

    // Runnable responsible to receive messages from the server.
    public static class RunnableMessageReceiver implements Runnable {
        private Socket socket;

        public RunnableMessageReceiver(Socket socket) {
            this.socket = socket;
        }

        // Lister to the incoming messages from the server (chunks 32KB).
        public static void listenToMessages(Socket tcpSocket)  {
            try {
                int maxBufferSizePerIteration = 32 * 1024;
                byte[] inputFromServerBuffer = new byte[maxBufferSizePerIteration];
                InputStream fromServerInputStream = tcpSocket.getInputStream();
                int responseLen = -1;

                while (!tcpSocket.isClosed()) {
                    while ((responseLen = fromServerInputStream.read(inputFromServerBuffer)) != -1) {
                        System.out.println(new String(inputFromServerBuffer, 0, responseLen, StandardCharsets.UTF_8));
                    }
                }
                fromServerInputStream.close();
                tcpSocket.close();
            } catch (IOException ex) {
                System.out.println("Info from the message receiver: Server connection closed.");
            }
        }

        public void run() {
            listenToMessages(this.socket);
        }
    }
}
