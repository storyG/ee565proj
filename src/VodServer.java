// ShihYu Gu, CheFu Chu
// EE 565
// Project1


import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

// The main class of the VodServer.
public class VodServer {

    // This is the main method of the VodServer,
    // Only accept get request.
    // The PortNumber is set to 8000 if not specified.
    // Initialize a RequestHandler if the connection is accepted,
    // Else throw an IOException error.
    public static void main(String[] args) throws IOException {

        ServerSocket serverSocket = null;
        int PortNumber;

        if (args.length > 0) {
            PortNumber = Integer.parseInt(args[0]);
        } else {
            PortNumber = 8000;
        }

        try {
            serverSocket = new ServerSocket(PortNumber);
        } catch (IOException e) {
            System.err.println("Could not listen on port: 8000.");
            System.exit(1);
        }


        System.out.println ("Waiting for connection on port: " + PortNumber + " ....");

        try {
            while(true) {
                Socket clientSocket = serverSocket.accept();
                RequestHandler handler = new RequestHandler(clientSocket, serverSocket);
                handler.start();
            }
        } catch (IOException e) {
            System.err.println("Accept failed.");
            System.exit(1);
        }
        serverSocket.close();
    }
}