// ShihYu Gu, CheFu Chu
// EE 565
// Project1

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.*;

// The class that implements the Request Handler of the VodServer with thread to deal with concurrency.
public class RequestHandler extends Thread {
    Socket clientSocket;
    ServerSocket serverSocket;
    BufferedReader socketReader;
    PrintWriter socketWriter;

    // This is the constructor of the RequestHandler.
    // Initialize the handler.
    // Parameters:
    //   - clientSocket: the client side
    //   - serverSocket: the server side
    // Returns:
    //   - None
    // Exceptions:
    //   - None
    public RequestHandler(Socket clientSocket, ServerSocket serverSocket){
        this.clientSocket = clientSocket;
        this.serverSocket = serverSocket;
    }

    // This is the start method of the threaded request handler
    // that accepts connection and send respond to the client by getResource() method.
    // If the file is valid and has been found, this method read the get request sent by
    // the client, and parse the head and finally passed in to the getResource() method.
    // Parameters:
    //   - None
    // Returns:
    //   - None
    // Exceptions:
    //   - IOException: throw IOException when Internal Server Error at the server side.
    @Override
    public void run() {
        try {
            System.out.println("Connection made.");

            socketReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            socketWriter = new PrintWriter(clientSocket.getOutputStream(), true);

            String input;
            String output = "";
            Boolean rng = false;
            int hi = 0, lo = 0, i;

            // Parse get request header.
            while ((input = socketReader.readLine()) != null) {
                System.out.println(input);
                if (input.startsWith("GET")) {
                    output = input;
                } else if (input.startsWith("Range")) {
                    rng = true;
                    String[] temp;
                    temp = input.split("-");
                    hi = Integer.parseInt(temp[1]);
                    for (i = 0; i < temp[0].length(); i++) {
                        if (Character.isDigit(temp[0].charAt(i))) {
                            break;
                        }
                    }
                    lo = Integer.parseInt(temp[0].substring(i));
                    System.out.println(lo);
                    break;
                } else if (input.startsWith("Accept")) {
                    break;
                }
            }
            // Send header information to getResource() method
            getResource(output, rng, lo, hi);

        } catch(IOException e) {
            String errorText = """
                    <!DOCTYPE html>
                    <html>
                    <body>

                    <h1>500 Internal Server Error</h1>

                    </body>
                    </html>""";
            byte[] temp;

            try {
                temp = errorText.getBytes("UTF-8");
            } catch (UnsupportedEncodingException ex) {
                throw new RuntimeException(ex);
            }

            try {
                Response("500", "", "", temp);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            System.out.println("IOException!");
            e.printStackTrace();
        }
    }

    // This is the helper method that
    // takes in the file name and initialize a File object from the path provided in the header.
    // The content path is located in the "/content/" directory in the project.
    // The url should only be, for example: http://localhost:8000/video.ogg.
    // This should point to the file: "/content/video.ogg"
    // Parameters:
    //   - getRequest: the get request header from the client
    //   - rng: the request header include range or not
    //   - lo: the bottom range of the requested range
    //   - hi: the upper range of the requested range
    // Returns:
    //   - None
    // Exceptions:
    //   - IOException: throw IOException when sendResponse() method has an error.
    public void getResource(String getRequest, Boolean rng, int lo, int hi) throws IOException {
        String[] parts = getRequest.split("\\s+");
        String filename = parts[1].substring(1);

        System.out.println(filename);

        filename = "content/" + filename;
        File resource = new File(filename);
        sendResponse(resource, rng, lo, hi);
    }

    // This is the helper method that
    // takes in the File object and the range and read the file content in bytes.
    // This method also decide the status code of the response:
    // 200 OK, 206 Partial, 404 File Not Found.
    // Finally, send response by using the Response() method.
    // Parameters:
    //   - resource: the File object requested.
    //   - rng: the request header include range or not
    //   - lo: the bottom range of the requested range
    //   - hi: the upper range of the requested range
    // Returns:
    //   - None
    // Exceptions:
    //   - IOException: throw IOException when Response() method has an error.
    public void sendResponse(File resource, Boolean rng, int lo, int hi) throws IOException {
        System.out.println(resource.getAbsolutePath());
        String additionalHeader = "Date: " + new Date() + "\r\n" +
                "Last-Modified: " + new Date(resource.lastModified()) + "\r\n";
        if (!resource.exists()) {
            String errorText = """
                    <!DOCTYPE html>
                    <html>
                    <body>

                    <h1>404 File Not Found</h1>

                    </body>
                    </html>""";
            byte[] temp = errorText.getBytes("UTF-8");
            Response("404", "", "", temp);
        } else {
            byte[] fileContent = Files.readAllBytes(resource.toPath());
            System.out.println(fileContent.length);
            int originFileLength = fileContent.length;

            if (!rng){
                additionalHeader = "Content-Range: bytes " + 0 + "-" + originFileLength + "/" + originFileLength + "\r\n" + additionalHeader;
                Response("200", Files.probeContentType(resource.toPath()), additionalHeader, fileContent);
            } else {
                fileContent = Arrays.copyOfRange(fileContent,lo,hi);
                System.out.println(fileContent.length);
                additionalHeader = "Content-Range: bytes " + lo + "-" + hi + "/" + originFileLength + "\r\n" + additionalHeader;
                Response("206", Files.probeContentType(resource.toPath()), additionalHeader, fileContent);
            }

        }

        socketWriter.close();
    }

    // This method construct the response header and store in byte format.
    // Write the response to the OutputStream of the client socket.
    // Write the file content to the OutputStream of the client socket.
    // Parameters:
    //   - status: the status code.
    //   - contentType: the content type of the file.
    //   - additionalHeader: the additional response header to be added.
    //   - content: the file content in byte[].
    // Returns:
    //   - None
    // Exceptions:
    //   - IOException: throw IOException when clientSocket.getOutputStream().write() method has an error.
    private void Response(String status, String contentType, String additionalHeader, byte[] content) throws IOException {
        byte[] response;
        if (status.equals("404") || status.equals("500")) {
            response = (
                    "HTTP/1.1 " + status + "\r\n"
                            + additionalHeader + "\r\n\r\n"
            ).getBytes("UTF-8");
        } else {
            response = (
                    "HTTP/1.1 " + status + "\r\n"
                            + "Accept-Ranges: bytes" + "\r\n"
                            + "Connection: keep-alive" + "\r\n"
                            + "Content-Length: " + content.length + "\r\n"
                            + "Content-Type: " + contentType + "\r\n"
                            + additionalHeader + "\r\n\r\n"
            ).getBytes("UTF-8");
        }

        clientSocket.getOutputStream().write(response, 0, response.length);
        clientSocket.getOutputStream().write(content, 0, content.length);
        clientSocket.getOutputStream().write("\r\n\r\n".getBytes("UTF-8"));
    }
}