package utils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.time.LocalDateTime;

/**
 * ChatSessionManager manages chat sessions, timers, and message history
 */
public class ChatSessionManager {
    private static ChatSessionManager instance;
    private final Map<String, ChatSession> activeSessions;
    private final Map<String, String> userConnections; // user -> connected_to_user
    private final ScheduledExecutorService scheduler;
    
    private ChatSessionManager() {
        this.activeSessions = new ConcurrentHashMap<>();
        this.userConnections = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(5);
    }
    
    public static synchronized ChatSessionManager getInstance() {
        if (instance == null) {
            instance = new ChatSessionManager();
        }
        return instance;
    }
    
    /**
     * Check if user is currently connected to someone
     */
    public boolean isUserConnected(String username) {
        return userConnections.containsKey(username);
    }
    
    /**
     * Get who the user is connected to
     */
    public String getConnectedUser(String username) {
        return userConnections.get(username);
    }
    
    /**
     * Establish connection between two users
     */
    public boolean connectUsers(String user1, String user2) {
        // Check if either user is already connected
        if (isUserConnected(user1) || isUserConnected(user2)) {
            return false;
        }
        
        userConnections.put(user1, user2);
        userConnections.put(user2, user1);
        
        // Create session for these users
        createSession(user1, user2);
        
        System.out.println("Connected users: " + user1 + " <-> " + user2);
        return true;
    }
    
    /**
     * Disconnect a user from their current connection
     */
    public boolean disconnectUser(String username) {
        String connectedTo = userConnections.remove(username);
        if (connectedTo != null) {
            userConnections.remove(connectedTo);
            
            // Destroy the session
            String sessionId = generateSessionId(username, connectedTo);
            destroySession(sessionId);
            
            System.out.println("Disconnected users: " + username + " <-> " + connectedTo);
            return true;
        }
        return false;
    }
    
    /**
     * Get all connected users
     */
    public Map<String, String> getConnectedUsers() {
        return new HashMap<>(userConnections);
    }

    /**
     * Create a new chat session between two users
     */
    public String createSession(String user1, String user2) {
        String sessionId = generateSessionId(user1, user2);
        ChatSession session = new ChatSession(sessionId, user1, user2);
        activeSessions.put(sessionId, session);
        System.out.println("Created chat session: " + sessionId);
        return sessionId;
    }
    
    /**
     * Get existing session or create new one
     */
    public ChatSession getOrCreateSession(String user1, String user2) {
        String sessionId = generateSessionId(user1, user2);
        return activeSessions.computeIfAbsent(sessionId, 
            k -> new ChatSession(sessionId, user1, user2));
    }
    
    /**
     * Add message to session
     */
    public void addMessage(String sessionId, Message message) {
        ChatSession session = activeSessions.get(sessionId);
        if (session != null) {
            session.addMessage(message);
        }
    }
    
    /**
     * Set auto-destroy timer for a session
     */
    public void setSessionTimer(String sessionId, long durationInMinutes) {
        ChatSession session = activeSessions.get(sessionId);
        if (session != null) {
            session.setAutoDestroyTimer(durationInMinutes);
            
            // Schedule destruction
            scheduler.schedule(() -> {
                destroySession(sessionId);
            }, durationInMinutes, TimeUnit.MINUTES);
            
            System.out.println("Set timer for session " + sessionId + 
                ": " + durationInMinutes + " minutes");
        }
    }
    
    /**
     * Clear messages in a session without destroying it
     */
    public boolean clearSession(String sessionId) {
        ChatSession session = activeSessions.get(sessionId);
        if (session != null && !session.isDestroyed()) {
            session.clearMessages();
            System.out.println("Cleared messages in session: " + sessionId);
            return true;
        }
        return false;
    }

    /**
     * Manually destroy a chat session
     */
    public boolean destroySession(String sessionId) {
        ChatSession session = activeSessions.remove(sessionId);
        if (session != null) {
            session.destroy();
            System.out.println("Destroyed chat session: " + sessionId);
            return true;
        }
        return false;
    }
    
    /**
     * Get all messages in a session
     */
    public List<Message> getSessionMessages(String sessionId) {
        ChatSession session = activeSessions.get(sessionId);
        return session != null ? session.getMessages() : new ArrayList<>();
    }
    
    /**
     * Check if session exists and is active
     */
    public boolean isSessionActive(String sessionId) {
        ChatSession session = activeSessions.get(sessionId);
        return session != null && !session.isDestroyed();
    }
    
    /**
     * Generate consistent session ID for two users
     */
    private String generateSessionId(String user1, String user2) {
        String[] users = {user1, user2};
        Arrays.sort(users);
        return users[0] + "_" + users[1];
    }
    
    /**
     * Get all active sessions
     */
    public Set<String> getActiveSessions() {
        return new HashSet<>(activeSessions.keySet());
    }
    
    /**
     * Shutdown the session manager
     */
    public void shutdown() {
        scheduler.shutdown();
        activeSessions.clear();
    }
    
    /**
     * Inner class representing a chat session
     */
    public static class ChatSession {
        private final String sessionId;
        private final String user1;
        private final String user2;
        private final List<Message> messages;
        private final LocalDateTime createdAt;
        private LocalDateTime autoDestroyAt;
        private boolean destroyed;
        
        public ChatSession(String sessionId, String user1, String user2) {
            this.sessionId = sessionId;
            this.user1 = user1;
            this.user2 = user2;
            this.messages = Collections.synchronizedList(new ArrayList<>());
            this.createdAt = LocalDateTime.now();
            this.destroyed = false;
        }
        
        public void addMessage(Message message) {
            if (!destroyed) {
                messages.add(message);
            }
        }
        
        public void clearMessages() {
            if (!destroyed) {
                messages.clear();
                System.out.println("Messages cleared for session: " + sessionId);
            }
        }
        
        public void setAutoDestroyTimer(long durationInMinutes) {
            this.autoDestroyAt = LocalDateTime.now().plusMinutes(durationInMinutes);
        }
        
        public void destroy() {
            this.destroyed = true;
            this.messages.clear();
        }
        
        // Getters
        public String getSessionId() { return sessionId; }
        public String getUser1() { return user1; }
        public String getUser2() { return user2; }
        public List<Message> getMessages() { return new ArrayList<>(messages); }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public LocalDateTime getAutoDestroyAt() { return autoDestroyAt; }
        public boolean isDestroyed() { return destroyed; }
    }
}
