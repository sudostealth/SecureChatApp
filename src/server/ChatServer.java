package server;

import utils.ChatSessionManager;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ChatServer is the main server class that handles client connections
 */
public class ChatServer {
    private static final int SOCKET_PORT = 12345;
    
    private ServerSocket serverSocket;
    private ExecutorService clientThreadPool;
    private boolean isRunning;
    
    public ChatServer() {
        this.clientThreadPool = Executors.newCachedThreadPool();
        this.isRunning = false;
    }
    
    /**
     * Start the chat server
     */
    public void start() {
        try {
            // Start socket server
            serverSocket = new ServerSocket(SOCKET_PORT);
            isRunning = true;
            
            System.out.println("=== Secure Chat Server Started ===");
            System.out.println("Socket Server listening on port: " + SOCKET_PORT);
            System.out.println("Server is ready to accept connections...");
            System.out.println("=====================================");
            
            // Accept client connections
            while (isRunning && !serverSocket.isClosed()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("New client connection from: " + 
                        clientSocket.getInetAddress().getHostAddress());
                    
                    // Create new client handler thread
                    ClientHandler clientHandler = new ClientHandler(clientSocket);
                    clientThreadPool.execute(clientHandler);
                    
                } catch (IOException e) {
                    if (isRunning) {
                        System.err.println("Error accepting client connection: " + e.getMessage());
                    }
                }
            }
            
        } catch (IOException e) {
            System.err.println("Error starting server: " + e.getMessage());
        } finally {
            shutdown();
        }
    }
    
    /**
     * Shutdown the server gracefully
     */
    public void shutdown() {
        System.out.println("Shutting down server...");
        isRunning = false;
        
        try {
            // Close server socket
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            
            // Shutdown thread pool
            if (clientThreadPool != null) {
                clientThreadPool.shutdown();
            }
            
            // Shutdown session manager
            ChatSessionManager.getInstance().shutdown();
            
            System.out.println("Server shutdown complete");
            
        } catch (IOException e) {
            System.err.println("Error during server shutdown: " + e.getMessage());
        }
    }
    
    /**
     * Main method to start the server
     */
    public static void main(String[] args) {
        ChatServer server = new ChatServer();
        
        // Add shutdown hook for graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nReceived shutdown signal...");
            server.shutdown();
        }));
        
        // Start the server
        server.start();
    }
    
    // Getters
    public boolean isRunning() { return isRunning; }
    public int getSocketPort() { return SOCKET_PORT; }
}
