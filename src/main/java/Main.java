import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
  public static void main(String[] args) {
    System.out.println("Logs from your program will appear here!");
    new Main().startServer();
  }

    private void startServer() {
        // Try-with-resources to automatically close the server socket
        try (ServerSocket serverSocket = new ServerSocket(4221)){
            serverSocket.setReuseAddress(true);

            while(true){
                Socket clientSocket = serverSocket.accept(); // Wait for connection from client
                System.out.println("Accepted new connection");
                handleClientConnection(clientSocket);
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }

    private void handleClientConnection(Socket clientSocket) {
      // Try-with-resources to automatically close the output stream
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
           OutputStream outputStream = clientSocket.getOutputStream()
      ) {
          String requestLine = reader.readLine();
          if(requestLine != null && !requestLine.isEmpty()){
              System.out.println("Request Line: " + requestLine);
              handleRequest(requestLine, outputStream);
          }
      } catch (IOException e){
          System.out.println("IOException while handling client connection: " + e.getMessage());
      } finally {
          try {
              clientSocket.close();
          } catch (IOException e){
              System.out.println("IOException while closing client socket: " + e.getMessage());
          }
      }
    }

    private void handleRequest(String requestLine, OutputStream outputStream) throws IOException{
      String[] requestParts = requestLine.split(" ");
      if (requestParts.length >= 2){
          String method = requestParts[0];
          String path = requestParts[1];
          String version = requestParts[2];

          if (method.equals("GET")){
              if (path.equals("/")){
                  sendResponse(outputStream, "HTTP/1.1 200 OK\r\n\r\n");
              } else {
                  sendResponse(outputStream, "HTTP/1.1 404 Not Found\r\n\r\n");
              }
          }
      }

    }

    private void sendResponse(OutputStream outputStream, String response) throws IOException{
      outputStream.write(response.getBytes("UTF-8"));
      outputStream.flush();
    }
}
