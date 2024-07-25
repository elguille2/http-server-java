import java.io.IOException;
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
            // Since the tester restarts your program quite often, setting SO_REUSEADDR
            // ensures that we don't run into 'Address already in use' errors
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
      try (OutputStream outputStream = clientSocket.getOutputStream()) {
          String httpResponse = "HTTP/1.1 200 OK\r\n\r\n";
          outputStream.write(httpResponse.getBytes("UTF-8"));
          // Ensure all data is sent by flushing the output stream
          outputStream.flush();
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
}
