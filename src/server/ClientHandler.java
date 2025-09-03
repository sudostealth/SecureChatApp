package server;

import utils.*;
import java.io.*;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Map;
import javax.crypto.SecretKey;

/**
 * ClientHandler handles individual client connections in separate threads
 */
public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private ObjectInputStream input;
    private ObjectOutputStream output;
    private String username;
    private boolean isConnected;
    private SecretKey sessionKey;
    
    // Static map to keep track of all connected clients
    private static final Map<String, ClientHandler> connectedClients = new ConcurrentHashMap<>();
    
    // Typing status tracking
    private static final Map<String, Boolean> typingStatus = new ConcurrentHashMap<>();
    private static final Map<String, Long> lastTypingTime = new ConcurrentHashMap<>();
    
    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
        this.isConnected = true;
        
        try {
            // Initialize streams
            this.output = new ObjectOutputStream(clientSocket.getOutputStream());
            this.input = new ObjectInputStream(clientSocket.getInputStream());
            
            // Generate session key for this client
            this.sessionKey = EncryptionUtil.generateKey();
            
            // Send session key to client (in real implementation, use key exchange protocol)
            String keyString = EncryptionUtil.keyToString(sessionKey);
            Message keyMessage = new Message("SERVER", "", keyString, Message.MessageType.SYSTEM);
            sendMessage(keyMessage);
            
        } catch (Exception e) {
            System.err.println("Error initializing client handler: " + e.getMessage());
            disconnect();
        }
    }
    
    @Override
    public void run() {
        try {
            // Handle client authentication
            Message authMessage = (Message) input.readObject();
            if (authMessage.getType() == Message.MessageType.JOIN) {
                String requestedUsername = authMessage.getSender();
                
                // Check if username is already taken
                if (connectedClients.containsKey(requestedUsername)) {
                    // Username is already taken, send error and close connection
                    Message errorMessage = new Message("SERVER", requestedUsername, 
                        "Username '" + requestedUsername + "' is already taken. Please try another username.", 
                        Message.MessageType.SYSTEM);
                    sendMessage(errorMessage);
                    
                    System.out.println("Connection rejected: Username '" + requestedUsername + "' already in use");
                    
                    // Close connection after sending error
                    disconnect();
                    return;
                }
                
                // Username is available, proceed with connection
                this.username = requestedUsername;
                connectedClients.put(username, this);
                
                // Initialize digital signature keys for the user
                DigitalSignatureUtil.initializeUserKeys(username);
                
                System.out.println("User " + username + " connected");
                
                // Send acknowledgment
                Message ackMessage = new Message("SERVER", username, 
                    "Welcome to Secure Chat!", Message.MessageType.SYSTEM);
                sendMessage(ackMessage);
            }
            
            // Main message handling loop
            while (isConnected && !clientSocket.isClosed()) {
                try {
                    Message message = (Message) input.readObject();
                    handleMessage(message);
                } catch (Exception e) {
                    System.err.println("Error reading message from " + username + ": " + e.getMessage());
                    break;
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error in client handler: " + e.getMessage());
        } finally {
            disconnect();
        }
    }
    
    /**
     * Handle different types of messages
     */
    private void handleMessage(Message message) {
        try {
            switch (message.getType()) {
                case TEXT:
                    handleTextMessage(message);
                    break;
                case FILE:
                    handleFileMessage(message);
                    break;
                case CLEAR_CHAT:
                    handleClearChatMessage(message);
                    break;
                case CLEAR_LOCAL_CHAT:
                    handleClearLocalChatMessage(message);
                    break;
                case DESTROY_CHAT:
                    handleDestroyChatMessage(message);
                    break;
                case SET_TIMER:
                    handleSetTimerMessage(message);
                    break;
                case CONNECT_REQUEST:
                    handleConnectRequest(message);
                    break;
                case CONNECT_ACCEPT:
                    handleConnectAccept(message);
                    break;
                case CONNECT_REJECT:
                    handleConnectReject(message);
                    break;
                case DISCONNECT_REQUEST:
                    handleDisconnectRequest(message);
                    break;
                case TIMER_UPDATE:
                    // Timer updates are handled server-side, no client action needed
                    break;
                case TIMER_EXPIRED:
                    // Timer expiration is handled server-side, no client action needed
                    break;
                case HEARTBEAT:
                    handleHeartbeat(message);
                    break;
                case TYPING_START:
                    handleTypingStart(message);
                    break;
                case TYPING_STOP:
                    handleTypingStop(message);
                    break;
                case DELIVERY_RECEIPT:
                    handleDeliveryReceipt(message);
                    break;
                case READ_RECEIPT:
                    handleReadReceipt(message);
                    break;
                default:
                    System.out.println("Unknown message type: " + message.getType());
            }
        } catch (Exception e) {
            System.err.println("Error handling message: " + e.getMessage());
        }
    }
    
    /**
     * Handle connection request
     */
    private void handleConnectRequest(Message message) {
        ChatSessionManager sessionManager = ChatSessionManager.getInstance();
        String requester = message.getSender();
        String target = message.getReceiver();
        
        // Check if requester is already connected
        if (sessionManager.isUserConnected(requester)) {
            Message errorMsg = new Message("SERVER", requester,
                "You are already connected to " + sessionManager.getConnectedUser(requester) + 
                ". Disconnect first to connect to someone else.", Message.MessageType.SYSTEM);
            sendMessage(errorMsg);
            return;
        }
        
        // Check if target is already connected
        if (sessionManager.isUserConnected(target)) {
            Message errorMsg = new Message("SERVER", requester,
                target + " is already in a chat with someone else.", Message.MessageType.SYSTEM);
            sendMessage(errorMsg);
            return;
        }
        
        // Send connection request to target user
        ClientHandler targetClient = connectedClients.get(target);
        if (targetClient != null) {
            Message requestMsg = new Message("SERVER", target,
                requester + " wants to start a chat with you. Accept?", 
                Message.MessageType.CONNECT_REQUEST);
            requestMsg.setSender(requester); // Set the actual requester
            targetClient.sendMessage(requestMsg);
            
            // Notify requester
            Message waitMsg = new Message("SERVER", requester,
                "Connection request sent to " + target + ". Waiting for response...", 
                Message.MessageType.SYSTEM);
            sendMessage(waitMsg);
        } else {
            Message offlineMsg = new Message("SERVER", requester,
                target + " is not online.", Message.MessageType.SYSTEM);
            sendMessage(offlineMsg);
        }
    }
    
    /**
     * Handle connection accept
     */
    private void handleConnectAccept(Message message) {
        ChatSessionManager sessionManager = ChatSessionManager.getInstance();
        String accepter = message.getSender();
        String requester = message.getReceiver();
        
        // Try to connect the users
        if (sessionManager.connectUsers(accepter, requester)) {
            // Notify both users of successful connection
            Message acceptMsg = new Message("SERVER", accepter,
                "Connected to " + requester + ". You can now start chatting!", 
                Message.MessageType.SYSTEM);
            sendMessage(acceptMsg);
            
            ClientHandler requesterClient = connectedClients.get(requester);
            if (requesterClient != null) {
                Message connectedMsg = new Message("SERVER", requester,
                    accepter + " accepted your request. You can now start chatting!", 
                    Message.MessageType.SYSTEM);
                requesterClient.sendMessage(connectedMsg);
            }
        } else {
            Message errorMsg = new Message("SERVER", accepter,
                "Failed to establish connection. One of you may already be connected to someone else.", 
                Message.MessageType.SYSTEM);
            sendMessage(errorMsg);
        }
    }
    
    /**
     * Handle connection reject
     */
    private void handleConnectReject(Message message) {
        String rejecter = message.getSender();
        String requester = message.getReceiver();
        
        // Notify the requester of rejection
        ClientHandler requesterClient = connectedClients.get(requester);
        if (requesterClient != null) {
            Message rejectMsg = new Message("SERVER", requester,
                rejecter + " declined your connection request.", 
                Message.MessageType.SYSTEM);
            requesterClient.sendMessage(rejectMsg);
        }
        
        // Notify the rejecter
        Message confirmMsg = new Message("SERVER", rejecter,
            "You declined the connection request from " + requester + ".", 
            Message.MessageType.SYSTEM);
        sendMessage(confirmMsg);
    }
    
    /**
     * Handle disconnect request
     */
    private void handleDisconnectRequest(Message message) {
        ChatSessionManager sessionManager = ChatSessionManager.getInstance();
        String user = message.getSender();
        String connectedTo = sessionManager.getConnectedUser(user);
        
        if (connectedTo != null) {
            // Disconnect the users
            sessionManager.disconnectUser(user);
            
            // Notify both users
            Message disconnectMsg = new Message("SERVER", user,
                "You have disconnected from " + connectedTo + ".", 
                Message.MessageType.SYSTEM);
            sendMessage(disconnectMsg);
            
            ClientHandler otherClient = connectedClients.get(connectedTo);
            if (otherClient != null) {
                Message otherDisconnectMsg = new Message("SERVER", connectedTo,
                    user + " has disconnected from the chat.", 
                    Message.MessageType.SYSTEM);
                otherClient.sendMessage(otherDisconnectMsg);
            }
        } else {
            Message errorMsg = new Message("SERVER", user,
                "You are not currently connected to anyone.", 
                Message.MessageType.SYSTEM);
            sendMessage(errorMsg);
        }
    }

    /**
     * Handle text messages with digital signature verification and delivery receipts
     */
    private void handleTextMessage(Message message) throws Exception {
        ChatSessionManager sessionManager = ChatSessionManager.getInstance();
        
        // Check if sender is connected to the receiver
        String connectedTo = sessionManager.getConnectedUser(message.getSender());
        if (connectedTo == null || !connectedTo.equals(message.getReceiver())) {
            Message errorMsg = new Message("SERVER", message.getSender(),
                "You are not connected to " + message.getReceiver() + 
                ". Send a connection request first.", Message.MessageType.SYSTEM);
            sendMessage(errorMsg);
            return;
        }
        
        // Decrypt message content
        String decryptedContent = EncryptionUtil.decrypt(message.getContent(), sessionKey);
        
        // Verify digital signature if present
        if (message.getDigitalSignature() != null && message.getSignerPublicKey() != null) {
            boolean signatureValid = DigitalSignatureUtil.verifySignature(
                decryptedContent, message.getDigitalSignature(), message.getSignerPublicKey());
            
            if (!signatureValid) {
                System.err.println("⚠️  Digital signature verification failed for message from " + message.getSender());
                Message warningMsg = new Message("SERVER", message.getSender(),
                    "⚠️  Message signature verification failed. Message may have been tampered with.", 
                    Message.MessageType.SYSTEM);
                sendMessage(warningMsg);
                return;
            } else {
                System.out.println("✅ Digital signature verified for message from " + message.getSender());
            }
        }
        
        message.setContent(decryptedContent);
        
        // Store message in session
        String sessionId = sessionManager.getOrCreateSession(
            message.getSender(), message.getReceiver()).getSessionId();
        sessionManager.addMessage(sessionId, message);
        
        // Forward to recipient
        ClientHandler recipient = connectedClients.get(message.getReceiver());
        if (recipient != null) {
            // Re-encrypt for recipient
            String encryptedForRecipient = EncryptionUtil.encrypt(
                decryptedContent, recipient.sessionKey);
            message.setContent(encryptedForRecipient);
            
            // Set delivery status and send message
            message.setDeliveryStatus(Message.DeliveryStatus.DELIVERED);
            message.setDeliveredAt(java.time.LocalDateTime.now());
            recipient.sendMessage(message);
            
            // Send delivery receipt back to sender
            Message deliveryReceipt = new Message("SERVER", message.getSender(),
                "DELIVERED:" + message.getMessageId(), Message.MessageType.DELIVERY_RECEIPT);
            sendMessage(deliveryReceipt);
            
        } else {
            // Send message that recipient is offline
            Message offlineMsg = new Message("SERVER", message.getSender(),
                "User " + message.getReceiver() + " is offline", 
                Message.MessageType.SYSTEM);
            sendMessage(offlineMsg);
        }
        
        System.out.println("Text message: " + message.getSender() + 
            " -> " + message.getReceiver() + ": " + decryptedContent);
    }
    
    /**
     * Handle file transfer messages
     */
    private void handleFileMessage(Message message) throws Exception {
        // Decrypt file data
        byte[] decryptedFile = EncryptionUtil.decryptBytes(message.getFileData(), sessionKey);
        message.setFileData(decryptedFile);
        
        // Store message in session
        ChatSessionManager sessionManager = ChatSessionManager.getInstance();
        String sessionId = sessionManager.getOrCreateSession(
            message.getSender(), message.getReceiver()).getSessionId();
        sessionManager.addMessage(sessionId, message);
        
        // Forward to recipient
        ClientHandler recipient = connectedClients.get(message.getReceiver());
        if (recipient != null) {
            // Re-encrypt file for recipient
            byte[] encryptedForRecipient = EncryptionUtil.encryptBytes(
                decryptedFile, recipient.sessionKey);
            message.setFileData(encryptedForRecipient);
            recipient.sendMessage(message);
            
            // Save file to server's file directory
            saveFile(message.getFileName(), decryptedFile);
        } else {
            Message offlineMsg = new Message("SERVER", message.getSender(),
                "User " + message.getReceiver() + " is offline", 
                Message.MessageType.SYSTEM);
            sendMessage(offlineMsg);
        }
        
        System.out.println("File transfer: " + message.getSender() + 
            " -> " + message.getReceiver() + ": " + message.getFileName());
    }
    
    /**
     * Handle clear chat request (only clear messages, keep session)
     */
    private void handleClearChatMessage(Message message) {
        ChatSessionManager sessionManager = ChatSessionManager.getInstance();
        String sessionId = sessionManager.getOrCreateSession(
            message.getSender(), message.getReceiver()).getSessionId();
        
        if (sessionManager.clearSession(sessionId)) {
            // Notify both users that chat was cleared
            Message clearNotification = new Message("SERVER", message.getSender(),
                "Chat history cleared for both users", Message.MessageType.SYSTEM);
            sendMessage(clearNotification);
            
            ClientHandler otherUser = connectedClients.get(message.getReceiver());
            if (otherUser != null) {
                Message otherNotification = new Message("SERVER", message.getReceiver(),
                    "Chat history cleared by " + message.getSender(), 
                    Message.MessageType.SYSTEM);
                otherUser.sendMessage(otherNotification);
                
                // Send signal to other user to clear their local chat area
                Message clearSignal = new Message("SERVER", message.getReceiver(),
                    "CLEAR_CHAT_AREA", Message.MessageType.CLEAR_LOCAL_CHAT);
                otherUser.sendMessage(clearSignal);
            }
        }
    }
    
    /**
     * Handle clear local chat request (only clear sender's chat area)
     */
    private void handleClearLocalChatMessage(Message message) {
        // Just send acknowledgment - no server-side clearing needed
        Message clearNotification = new Message("SERVER", message.getSender(),
            "Your local chat history cleared", Message.MessageType.SYSTEM);
        sendMessage(clearNotification);
    }
    
    /**
     * Handle chat destruction request (destroy session and exit clients)
     */
    private void handleDestroyChatMessage(Message message) {
        ChatSessionManager sessionManager = ChatSessionManager.getInstance();
        String sessionId = sessionManager.getOrCreateSession(
            message.getSender(), message.getReceiver()).getSessionId();
        
        if (sessionManager.destroySession(sessionId)) {
            // Notify both users and signal them to exit
            Message destroyNotification = new Message("SERVER", message.getSender(),
                "CLOSE_APPLICATION", Message.MessageType.DESTROY_CHAT);
            sendMessage(destroyNotification);
            
            ClientHandler otherUser = connectedClients.get(message.getReceiver());
            if (otherUser != null) {
                Message otherNotification = new Message("SERVER", message.getReceiver(),
                    "CLOSE_APPLICATION", Message.MessageType.DESTROY_CHAT);
                otherUser.sendMessage(otherNotification);
            }
            
            // Disconnect both users after a short delay
            new Thread(() -> {
                try {
                    Thread.sleep(1000); // Give time for messages to be sent
                    disconnect();
                    if (otherUser != null) {
                        otherUser.disconnect();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
    }
    
    /**
     * Handle timer setting request with countdown
     */
    private void handleSetTimerMessage(Message message) {
        ChatSessionManager sessionManager = ChatSessionManager.getInstance();
        String sessionId = sessionManager.getOrCreateSession(
            message.getSender(), message.getReceiver()).getSessionId();
        
        long timerSeconds = message.getTimerDuration(); // Now in seconds
        
        // Notify both users of timer start
        long minutes = timerSeconds / 60;
        long seconds = timerSeconds % 60;
        String timeDisplay = String.format("%02d:%02d", minutes, seconds);
        
        String timerMsg = "⏱️ DESTRUCTION TIMER SET: " + timeDisplay + " - Chat will auto-destroy!";
        Message timerNotification = new Message("SERVER", message.getSender(),
            timerMsg, Message.MessageType.SYSTEM);
        sendMessage(timerNotification);
        
        ClientHandler otherUser = connectedClients.get(message.getReceiver());
        if (otherUser != null) {
            Message otherNotification = new Message("SERVER", message.getReceiver(),
                timerMsg + " (set by " + message.getSender() + ")", 
                Message.MessageType.SYSTEM);
            otherUser.sendMessage(otherNotification);
        }
        
        // Start countdown timer
        startCountdownTimer(sessionId, timerSeconds, message.getSender(), message.getReceiver());
    }
    
    /**
     * Start countdown timer with regular updates
     */
    private void startCountdownTimer(String sessionId, long totalSeconds, String user1, String user2) {
        ScheduledExecutorService countdownExecutor = Executors.newSingleThreadScheduledExecutor();
        
        // Create a runnable for countdown updates
        Runnable countdownTask = new Runnable() {
            private long remainingSeconds = totalSeconds;
            
            @Override
            public void run() {
                try {
                    if (remainingSeconds > 0) {
                        // Send timer update to both users
                        ClientHandler user1Handler = connectedClients.get(user1);
                        ClientHandler user2Handler = connectedClients.get(user2);
                        
                        if (user1Handler != null && user2Handler != null) {
                            Message timerUpdate = new Message("SERVER", user1,
                                "TIMER_UPDATE", Message.MessageType.TIMER_UPDATE);
                            timerUpdate.setTimerDuration(remainingSeconds);
                            user1Handler.sendMessage(timerUpdate);
                            
                            Message timerUpdate2 = new Message("SERVER", user2,
                                "TIMER_UPDATE", Message.MessageType.TIMER_UPDATE);
                            timerUpdate2.setTimerDuration(remainingSeconds);
                            user2Handler.sendMessage(timerUpdate2);
                            
                            remainingSeconds--;
                        } else {
                            // One or both users disconnected, cancel timer
                            countdownExecutor.shutdown();
                        }
                    } else {
                        // Timer expired - destroy the chat
                        handleTimerExpiration(sessionId, user1, user2);
                        countdownExecutor.shutdown();
                    }
                } catch (Exception e) {
                    System.err.println("Error in countdown timer: " + e.getMessage());
                    countdownExecutor.shutdown();
                }
            }
        };
        
        // Schedule countdown updates every second
        countdownExecutor.scheduleAtFixedRate(countdownTask, 0, 1, TimeUnit.SECONDS);
    }
    
    /**
     * Handle timer expiration and destroy chat
     */
    private void handleTimerExpiration(String sessionId, String user1, String user2) {
        try {
            System.out.println("Timer expired for session: " + sessionId);
            
            // Send expiration message to both users
            ClientHandler user1Handler = connectedClients.get(user1);
            ClientHandler user2Handler = connectedClients.get(user2);
            
            if (user1Handler != null) {
                Message expiredMsg = new Message("SERVER", user1,
                    "TIMER_EXPIRED", Message.MessageType.TIMER_EXPIRED);
                user1Handler.sendMessage(expiredMsg);
            }
            
            if (user2Handler != null) {
                Message expiredMsg = new Message("SERVER", user2,
                    "TIMER_EXPIRED", Message.MessageType.TIMER_EXPIRED);
                user2Handler.sendMessage(expiredMsg);
            }
            
            // Destroy the session
            ChatSessionManager sessionManager = ChatSessionManager.getInstance();
            sessionManager.destroySession(sessionId);
            
            // Disconnect both users after a delay
            Thread.sleep(1000);
            
            if (user1Handler != null) {
                user1Handler.disconnect();
            }
            if (user2Handler != null) {
                user2Handler.disconnect();
            }
            
        } catch (Exception e) {
            System.err.println("Error handling timer expiration: " + e.getMessage());
        }
    }
    
    /**
     * Handle heartbeat messages
     */
    private void handleHeartbeat(Message message) {
        // Simply respond with heartbeat
        Message heartbeatResponse = new Message("SERVER", message.getSender(),
            "HEARTBEAT_ACK", Message.MessageType.HEARTBEAT);
        sendMessage(heartbeatResponse);
    }
    
    /**
     * Handle typing start notification
     */
    private void handleTypingStart(Message message) {
        String sender = message.getSender();
        String receiver = message.getReceiver();
        
        // Update typing status
        typingStatus.put(sender, true);
        lastTypingTime.put(sender, System.currentTimeMillis());
        
        // Forward typing notification to the receiver
        ClientHandler recipient = connectedClients.get(receiver);
        if (recipient != null) {
            Message typingMsg = new Message("SERVER", receiver,
                sender + " is typing...", Message.MessageType.TYPING_START);
            typingMsg.setSender(sender);
            recipient.sendMessage(typingMsg);
        }
        
        // Start typing timeout (stop typing after 3 seconds of inactivity)
        new Thread(() -> {
            try {
                Thread.sleep(3000);
                Long lastTime = lastTypingTime.get(sender);
                if (lastTime != null && System.currentTimeMillis() - lastTime >= 3000) {
                    // Auto-stop typing after timeout
                    handleTypingStop(new Message(sender, receiver, "TYPING_STOP", Message.MessageType.TYPING_STOP));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
    
    /**
     * Handle typing stop notification
     */
    private void handleTypingStop(Message message) {
        String sender = message.getSender();
        String receiver = message.getReceiver();
        
        // Update typing status
        typingStatus.put(sender, false);
        lastTypingTime.remove(sender);
        
        // Forward typing stop notification to the receiver
        ClientHandler recipient = connectedClients.get(receiver);
        if (recipient != null) {
            Message typingStopMsg = new Message("SERVER", receiver,
                "TYPING_STOP", Message.MessageType.TYPING_STOP);
            typingStopMsg.setSender(sender);
            recipient.sendMessage(typingStopMsg);
        }
    }
    
    /**
     * Handle delivery receipt
     */
    private void handleDeliveryReceipt(Message message) {
        // Extract message ID from content
        String content = message.getContent();
        if (content.startsWith("DELIVERED:")) {
            String messageId = content.substring(10);
            System.out.println("📬 Delivery receipt: Message " + messageId + " delivered to " + message.getSender());
        }
    }
    
    /**
     * Handle read receipt
     */
    private void handleReadReceipt(Message message) {
        String sender = message.getSender();
        String originalSender = message.getReceiver();
        String messageId = message.getContent().replace("READ:", "");
        
        // Forward read receipt to original sender
        ClientHandler originalSenderClient = connectedClients.get(originalSender);
        if (originalSenderClient != null) {
            Message readReceiptMsg = new Message("SERVER", originalSender,
                "READ:" + messageId, Message.MessageType.READ_RECEIPT);
            readReceiptMsg.setSender(sender);
            originalSenderClient.sendMessage(readReceiptMsg);
        }
        
        System.out.println("👁️  Read receipt: Message " + messageId + " read by " + sender);
    }
    
    /**
     * Send message to this client
     */
    public void sendMessage(Message message) {
        try {
            output.writeObject(message);
            output.flush();
        } catch (IOException e) {
            System.err.println("Error sending message to " + username + ": " + e.getMessage());
            disconnect();
        }
    }
    
    /**
     * Save file to server directory
     */
    private void saveFile(String fileName, byte[] fileData) {
        try {
            String filePath = "../files/" + System.currentTimeMillis() + "_" + fileName;
            FileOutputStream fos = new FileOutputStream(filePath);
            fos.write(fileData);
            fos.close();
            System.out.println("File saved: " + filePath);
        } catch (IOException e) {
            System.err.println("Error saving file: " + e.getMessage());
        }
    }
    
    /**
     * Disconnect client
     */
    public void disconnect() {
        isConnected = false;
        
        if (username != null) {
            // Handle disconnection from current chat partner
            ChatSessionManager sessionManager = ChatSessionManager.getInstance();
            String connectedTo = sessionManager.getConnectedUser(username);
            if (connectedTo != null) {
                sessionManager.disconnectUser(username);
                
                // Notify the other user
                ClientHandler otherClient = connectedClients.get(connectedTo);
                if (otherClient != null) {
                    Message disconnectMsg = new Message("SERVER", connectedTo,
                        username + " has left the chat.", Message.MessageType.SYSTEM);
                    otherClient.sendMessage(disconnectMsg);
                }
            }
            
            connectedClients.remove(username);
            System.out.println("User " + username + " disconnected");
        }
        
        try {
            if (input != null) input.close();
            if (output != null) output.close();
            if (clientSocket != null) clientSocket.close();
        } catch (IOException e) {
            System.err.println("Error closing connections: " + e.getMessage());
        }
    }
    
    // Getters
    public String getUsername() { return username; }
    public boolean isConnected() { return isConnected; }
    
    /**
     * Get all connected clients
     */
    public static Map<String, ClientHandler> getConnectedClients() {
        return new ConcurrentHashMap<>(connectedClients);
    }
}
