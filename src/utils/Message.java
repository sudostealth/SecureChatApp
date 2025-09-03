package utils;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Message class represents a chat message with various types and metadata
 */
public class Message implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public enum MessageType {
        TEXT, FILE, JOIN, LEAVE, CLEAR_CHAT, CLEAR_LOCAL_CHAT, DESTROY_CHAT, SET_TIMER, 
        TIMER_UPDATE, TIMER_EXPIRED,
        CONNECT_REQUEST, CONNECT_ACCEPT, CONNECT_REJECT, DISCONNECT_REQUEST,
        SYSTEM, HEARTBEAT,
        TYPING_START, TYPING_STOP, DELIVERY_RECEIPT, READ_RECEIPT
    }
    
    public enum DeliveryStatus {
        SENT, DELIVERED, READ
    }
    
    private String sender;
    private String receiver;
    private String content;
    private MessageType type;
    private LocalDateTime timestamp;
    private String fileName;
    private byte[] fileData;
    private long fileSize;
    private String messageId;
    private long timerDuration; // in milliseconds
    
    // Digital Signature fields
    private byte[] digitalSignature;
    private String signerPublicKey;
    
    // Delivery Status fields
    private DeliveryStatus deliveryStatus;
    private LocalDateTime deliveredAt;
    private LocalDateTime readAt;
    
    // Typing indicator fields
    private boolean isTyping;
    
    public Message() {
        this.timestamp = LocalDateTime.now();
        this.messageId = generateMessageId();
        this.deliveryStatus = DeliveryStatus.SENT;
        this.isTyping = false;
    }
    
    public Message(String sender, String receiver, String content, MessageType type) {
        this();
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
        this.type = type;
    }
    
    private String generateMessageId() {
        return System.currentTimeMillis() + "_" + Math.random();
    }
    
    // Getters and Setters
    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }
    
    public String getReceiver() { return receiver; }
    public void setReceiver(String receiver) { this.receiver = receiver; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    
    public byte[] getFileData() { return fileData; }
    public void setFileData(byte[] fileData) { this.fileData = fileData; }
    
    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }
    
    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }
    
    public long getTimerDuration() { return timerDuration; }
    public void setTimerDuration(long timerDuration) { this.timerDuration = timerDuration; }
    
    // Digital Signature getters and setters
    public byte[] getDigitalSignature() { return digitalSignature; }
    public void setDigitalSignature(byte[] digitalSignature) { this.digitalSignature = digitalSignature; }
    
    public String getSignerPublicKey() { return signerPublicKey; }
    public void setSignerPublicKey(String signerPublicKey) { this.signerPublicKey = signerPublicKey; }
    
    // Delivery Status getters and setters
    public DeliveryStatus getDeliveryStatus() { return deliveryStatus; }
    public void setDeliveryStatus(DeliveryStatus deliveryStatus) { this.deliveryStatus = deliveryStatus; }
    
    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; }
    
    public LocalDateTime getReadAt() { return readAt; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }
    
    // Typing indicator getters and setters
    public boolean isTyping() { return isTyping; }
    public void setTyping(boolean typing) { this.isTyping = typing; }
    
    @Override
    public String toString() {
        return String.format("[%s] %s -> %s: %s (%s)", 
            timestamp.toString(), sender, receiver, content, type);
    }
}
