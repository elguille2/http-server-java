import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {

    private static String filesDirectory = "./";

    public static void main(String[] args) {
        // Check for the --directory flag and set the files directory
        if (args.length == 2 && args[0].equals("--directory")) {
            filesDirectory = args[1];
        }
        System.out.println("Logs from your program will appear here!");
        startServer();
    }

    private static void startServer() {
        // Try-with-resources to automatically close the server socket
        try (ServerSocket serverSocket = new ServerSocket(4221)) {
            serverSocket.setReuseAddress(true);

            while (true) {
                Socket clientSocket = serverSocket.accept(); // Wait for connection from client
                System.out.println("Accepted new connection");
                // Handle each connection in a new thread to support concurrent connections
                new Thread(() -> handleClientConnection(clientSocket)).start();
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }

    private static void handleClientConnection(Socket clientSocket) {
        // Try-with-resources to automatically close the output stream
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             OutputStream outputStream = clientSocket.getOutputStream()) {
            HttpRequest request = parseRequest(reader);
            if (request != null)
                handleRequest(request, outputStream);
        } catch (IOException e) {
            System.out.println("IOException while handling client connection: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.out.println("IOException while closing client socket: " + e.getMessage());
            }
        }
    }

    private static HttpRequest parseRequest(BufferedReader reader) throws IOException {
        HttpRequest request = new HttpRequest();

        // Read the request line
        String requestLine = reader.readLine();
        if (requestLine == null || requestLine.isEmpty())
            return null;
        String[] requestParts = requestLine.split(" ");
        if (requestParts.length < 3)
            return null;
        request.method = requestParts[0];
        request.path = requestParts[1];
        request.version = requestParts[2];

        // Read headers
        String headerLine;
        while (!(headerLine = reader.readLine()).isEmpty()) {
            String[] headerParts = headerLine.split(": ");
            if (headerParts.length == 2)
                request.headers.put(headerParts[0], headerParts[1]);
        }
        return request;
    }

    private static void handleRequest(HttpRequest request, OutputStream outputStream) throws IOException {
        if (request.method.equals("GET")) {
            if (request.path.equals("/")) {
                sendResponse(outputStream, "HTTP/1.1 200 OK\r\n\r\n");
            } else if (request.path.startsWith("/echo/")) {
                // Extract the message from the path
                String echoMessage = request.path.substring(6);
                String response = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: text/plain\r\n" +
                        "Content-Length: " + echoMessage.length() + "\r\n" +
                        "\r\n" +
                        echoMessage;
                sendResponse(outputStream, response);
            } else if (request.path.equals("/user-agent")) {
                String userAgent = request.headers.get("User-Agent");
                if (userAgent == null) userAgent = "";
                String response = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: text/plain\r\n" +
                        "Content-Length: " + userAgent.length() + "\r\n" +
                        "\r\n" +
                        userAgent;
                sendResponse(outputStream, response);
            } else if (request.path.startsWith("/files/")) {
                // Handle request for a file from the files directory
                handleFileRequest(request.path.substring(7), outputStream);
            } else {
                sendResponse(outputStream, "HTTP/1.1 404 Not Found\r\n\r\n");
            }
        }
    }

    private static void handleFileRequest(String fileName, OutputStream outputStream) throws IOException{
        File file = new File(filesDirectory, fileName);
        if (file.exists() && file.isFile()) {
            // Load the file content into memory to send it in the HTTP response.
            FileInputStream fis = new FileInputStream(file); // Open an input stream to read the file
            byte[] fileContent = new byte[(int) file.length()];
            fis.read(fileContent);

            String response = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: application/octet-stream\r\n" +
                    "Content-Length: " + fileContent.length + "\r\n" +
                    "\r\n";

            outputStream.write(response.getBytes("UTF-8")); // Write the response headers
            outputStream.write(fileContent); // Write the file content
            outputStream.flush();
        } else {
            sendResponse(outputStream, "HTTP/1.1 404 Not Found\r\n\r\n");
        }
    }

    private static void sendResponse(OutputStream outputStream, String response) throws IOException {
        outputStream.write(response.getBytes("UTF-8"));
        outputStream.flush();
    }
}
