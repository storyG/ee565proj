import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.*;

public class RequestHandler extends Thread {
    Socket clientSocket;
    ServerSocket serverSocket;
    BufferedReader socketReader;
    PrintWriter socketWriter;

    public RequestHandler(Socket clientSocket, ServerSocket serverSocket){
        this.clientSocket = clientSocket;
        this.serverSocket = serverSocket;
    }

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
                Response("404", "", "", temp);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            System.out.println("IOException!");
            e.printStackTrace();
        }
    }

    public void getResource(String getRequest, Boolean rng, int lo, int hi) throws IOException {
        String[] parts = getRequest.split("\\s+");
        String filename = parts[1].substring(1);

        System.out.println(filename);

        filename = "content/" + filename;
        File resource = new File(filename);
        sendResponse(resource, rng, lo, hi);
    }

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
            Response("404", "", additionalHeader, temp);
        } else {
            byte[] fileContent = Files.readAllBytes(resource.toPath());
            System.out.println(fileContent.length);

            if (!rng){
                Response("200", Files.probeContentType(resource.toPath()), additionalHeader, fileContent);
            } else {
                fileContent = Arrays.copyOfRange(fileContent,lo,hi);
                System.out.println(fileContent.length);
                Response("206", Files.probeContentType(resource.toPath()), additionalHeader, fileContent);
            }

        }

        socketWriter.close();
    }

    private void Response(String status, String contentType, String additional_header, byte[] content) throws IOException {
        byte[] response;
        if (status.equals("404") || status.equals("500")) {
            response = (
                    "HTTP/1.1 " + status + "\r\n"
                            + additional_header + "\r\n"
            ).getBytes("UTF-8");
        } else {
            response = (
                    "HTTP/1.1 " + status + "\r\n"
                            + "Accept-Ranges: bytes" + "\r\n"
                            + "Connection: keep-alive" + "\r\n"
                            + "Content-Length: " + content.length + "\r\n"
                            + "Content-Type: " + contentType + "\r\n"
                            + additional_header + "\r\n"
            ).getBytes("UTF-8");
        }

        clientSocket.getOutputStream().write(response, 0, response.length);
        clientSocket.getOutputStream().write(content, 0, content.length);
        clientSocket.getOutputStream().write("\r\n\r\n".getBytes("UTF-8"));
    }
}