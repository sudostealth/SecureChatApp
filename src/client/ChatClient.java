package client;

import utils.*;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.Socket;
import javax.crypto.SecretKey;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * ChatClient provides GUI interface for the secure chat application
 */
public class ChatClient extends JFrame {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 12345;
    
    // Modern cybersecurity color scheme
    private static final Color DARK_MATRIX = new Color(0, 20, 20);           // Very dark teal
    private static final Color MATRIX_GREEN = new Color(0, 255, 65);         // Bright matrix green
    private static final Color NEON_CYAN = new Color(0, 255, 255);           // Bright cyan
    private static final Color DEEP_BLUE = new Color(13, 17, 23);            // GitHub dark
    private static final Color CARD_DARK = new Color(22, 27, 34);            // Dark card background
    private static final Color BORDER_NEON = new Color(48, 164, 108);        // Neon green border
    private static final Color TEXT_GREEN = new Color(201, 209, 217);        // Light text
    private static final Color WARNING_AMBER = new Color(255, 149, 0);       // Warning orange
    private static final Color SUCCESS_GREEN = new Color(40, 167, 69);       // Success green
    private static final Color DANGER_RED = new Color(220, 53, 69);          // Danger red
    private static final Color GLASS_OVERLAY = new Color(255, 255, 255, 30); // Glass effect
    
    // Network components
    private Socket socket;
    private ObjectInputStream input;
    private ObjectOutputStream output;
    private SecretKey sessionKey;
    
    // GUI components
    private JTextField usernameField;
    private JTextField recipientField;
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton connectButton;
    private JButton sendButton;
    private JButton sendFileButton;
    private JButton clearChatButton;
    private JButton destroyChatButton;
    private JButton setTimerButton;
    private JButton connectToUserButton;
    private JButton disconnectButton;
    private JLabel statusLabel;
    private JLabel connectionStatusLabel;
    private JPanel chatPanel;
    private JPanel connectionPanel;
    
    // Client state
    private String username;
    private boolean isConnected;
    private ScheduledExecutorService heartbeatScheduler;
    private JLabel timerLabel; // For showing countdown
    
    // New features
    private JLabel typingIndicatorLabel; // For showing typing status
    private java.util.Map<String, String> deliveryStatus; // Track message delivery status
    private javax.swing.Timer typingTimer; // Timer for typing indicator
    
    // File download history
    private java.util.List<ReceivedFile> receivedFiles; // Track received files
    private JButton downloadHistoryButton; // Button to show download history
    
    // Inner class for received file tracking
    private static class ReceivedFile {
        String fileName;
        String sender;
        byte[] fileData;
        java.time.LocalDateTime receivedTime;
        boolean downloaded;
        
        ReceivedFile(String fileName, String sender, byte[] fileData) {
            this.fileName = fileName;
            this.sender = sender;
            this.fileData = fileData;
            this.receivedTime = java.time.LocalDateTime.now();
            this.downloaded = false;
        }
    }
    
    public ChatClient() {
        // Set custom theme
        customizeUIDefaults();
        
        initializeGUI();
        this.isConnected = false;
        this.heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();
        this.deliveryStatus = new java.util.concurrent.ConcurrentHashMap<>();
        this.receivedFiles = new java.util.ArrayList<>();
    }
    
    /**
     * Customize UI defaults for modern cybersecurity theme
     */
    private void customizeUIDefaults() {
        // Set dark theme with neon accents
        UIManager.put("Button.background", CARD_DARK);
        UIManager.put("Button.foreground", MATRIX_GREEN);
        UIManager.put("Button.font", new Font("Consolas", Font.BOLD, 12));
        UIManager.put("TextField.font", new Font("Consolas", Font.PLAIN, 12));
        UIManager.put("TextArea.font", new Font("Consolas", Font.PLAIN, 12));
        UIManager.put("Label.font", new Font("Consolas", Font.PLAIN, 12));
        UIManager.put("Panel.background", DEEP_BLUE);
        
        // Fix popup message colors for better visibility
        UIManager.put("OptionPane.background", DARK_MATRIX);
        UIManager.put("OptionPane.messageForeground", MATRIX_GREEN);
        UIManager.put("OptionPane.foreground", MATRIX_GREEN);
        UIManager.put("Panel.background", DARK_MATRIX);
        UIManager.put("Label.foreground", MATRIX_GREEN);
        UIManager.put("Button.background", CARD_DARK);
        UIManager.put("Button.foreground", NEON_CYAN);
        
        // Set system look and feel to dark
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Metal".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("Could not set look and feel: " + e.getMessage());
        }
    }
    
    /**
     * Initialize the GUI components with modern cybersecurity styling
     */
    private void initializeGUI() {
        setTitle("🔒 SecureChat - Encrypted and Self-Destructive Communication");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 750);
        setLocationRelativeTo(null);
        setResizable(true);
        
        // Set modern dark background
        getContentPane().setBackground(DARK_MATRIX);
        
        // Create main panels with cybersecurity styling
        connectionPanel = createCyberConnectionPanel();
        chatPanel = createCyberChatPanel();
        
        // Initially show connection panel
        add(connectionPanel, BorderLayout.CENTER);
        
        // Add window closing listener
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                disconnect();
                System.exit(0);
            }
        });
    }
    
    /**
     * Create modern cybersecurity connection panel with futuristic styling
     */
    private JPanel createCyberConnectionPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(DARK_MATRIX);
        
        // Create centered card panel with glass effect and responsive sizing
        JPanel cardPanel = new JPanel();
        cardPanel.setLayout(new BoxLayout(cardPanel, BoxLayout.Y_AXIS));
        cardPanel.setBackground(CARD_DARK);
        cardPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_NEON, 2),
            BorderFactory.createEmptyBorder(50, 50, 50, 50)
        ));
        // Make responsive - adapt to screen size
        int screenWidth = Toolkit.getDefaultToolkit().getScreenSize().width;
        int cardWidth = Math.min(500, Math.max(400, screenWidth / 3));
        cardPanel.setMaximumSize(new Dimension(cardWidth, 500));
        cardPanel.setPreferredSize(new Dimension(cardWidth, 400));
        
        // Add glass overlay effect
        JPanel glassOverlay = new JPanel();
        glassOverlay.setBackground(GLASS_OVERLAY);
        glassOverlay.setOpaque(true);
        
        // Cybersecurity header with matrix-style icon
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        headerPanel.setBackground(CARD_DARK);
        
        JLabel securityIcon = new JLabel("⚡");
        securityIcon.setFont(new Font("Segoe UI Emoji", Font.BOLD, 36));
        securityIcon.setForeground(MATRIX_GREEN);
        headerPanel.add(securityIcon);
        
        JLabel titleLabel = new JLabel("SECURECHAT");
        titleLabel.setFont(new Font("Consolas", Font.BOLD, 32));
        titleLabel.setForeground(MATRIX_GREEN);
        headerPanel.add(titleLabel);
        
        cardPanel.add(headerPanel);
        cardPanel.add(Box.createVerticalStrut(15));
        
        // Animated subtitle with cyber effect
        JLabel subtitleLabel = new JLabel(">> SELF-DESTRUCTIVE SECURITY <<");
        subtitleLabel.setFont(new Font("Consolas", Font.BOLD, 14));
        subtitleLabel.setForeground(NEON_CYAN);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        cardPanel.add(subtitleLabel);
        cardPanel.add(Box.createVerticalStrut(35));
        
        // Username input section with cyber styling
        JLabel usernameLabel = new JLabel("[ ENTER_IDENTITY ]");
        usernameLabel.setFont(new Font("Consolas", Font.BOLD, 13));
        usernameLabel.setForeground(TEXT_GREEN);
        usernameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        cardPanel.add(usernameLabel);
        cardPanel.add(Box.createVerticalStrut(10));
        
        usernameField = new JTextField(20);
        usernameField.setFont(new Font("Consolas", Font.BOLD, 16));
        usernameField.setMaximumSize(new Dimension(350, 40));
        usernameField.setBackground(DEEP_BLUE);
        usernameField.setForeground(MATRIX_GREEN);
        usernameField.setCaretColor(NEON_CYAN);
        usernameField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_NEON, 2),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        usernameField.setAlignmentX(Component.CENTER_ALIGNMENT);
        cardPanel.add(usernameField);
        cardPanel.add(Box.createVerticalStrut(30));
        
        // Connect button with cyberpunk styling and responsive sizing
        connectButton = new JButton("🔐 INITIATE_SECURE_CONNECTION");
        connectButton.setFont(new Font("Consolas", Font.BOLD, 14));
        connectButton.setBackground(CARD_DARK);
        connectButton.setForeground(MATRIX_GREEN);
        connectButton.setFocusPainted(false);
        connectButton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_NEON, 2),
            BorderFactory.createEmptyBorder(12, 20, 12, 20)
        ));
        // Make button responsive to card width
        int buttonWidth = Math.min(400, cardWidth - 100);
        connectButton.setPreferredSize(new Dimension(buttonWidth, 45));
        connectButton.setMaximumSize(new Dimension(buttonWidth, 45));
        connectButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        connectButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        connectButton.addActionListener(e -> connect());
        
        // Hover effect with neon glow
        connectButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                connectButton.setBackground(DEEP_BLUE);
                connectButton.setForeground(NEON_CYAN);
                connectButton.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(NEON_CYAN, 3),
                    BorderFactory.createEmptyBorder(11, 19, 11, 19)
                ));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                connectButton.setBackground(CARD_DARK);
                connectButton.setForeground(MATRIX_GREEN);
                connectButton.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER_NEON, 2),
                    BorderFactory.createEmptyBorder(12, 20, 12, 20)
                ));
            }
        });
        
        cardPanel.add(connectButton);
        cardPanel.add(Box.createVerticalStrut(25));
        
        // Status label with terminal styling
        statusLabel = new JLabel(">> Ready for secure connection...");
        statusLabel.setFont(new Font("Consolas", Font.PLAIN, 12));
        statusLabel.setForeground(TEXT_GREEN);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        cardPanel.add(statusLabel);
        
        // Add security info footer with matrix theme
        cardPanel.add(Box.createVerticalStrut(20));
        JLabel securityInfo = new JLabel("⚡ AES-256 | RSA-2048 | SELF-DESTRUCTIVE ⚡");
        securityInfo.setFont(new Font("Consolas", Font.BOLD, 11));
        securityInfo.setForeground(SUCCESS_GREEN);
        securityInfo.setAlignmentX(Component.CENTER_ALIGNMENT);
        cardPanel.add(securityInfo);
        
        // Center the card panel with matrix background
        JPanel wrapperPanel = new JPanel(new GridBagLayout());
        wrapperPanel.setBackground(DARK_MATRIX);
        wrapperPanel.add(cardPanel);
        
        mainPanel.add(wrapperPanel, BorderLayout.CENTER);
        
        return mainPanel;
    }
    
    /**
     * Create cyberpunk chat panel with modern dark theme
     */
    private JPanel createCyberChatPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(DARK_MATRIX);
        
        // Top header panel with cybersecurity indicators
        JPanel headerPanel = createCyberHeaderPanel();
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        
        // Chat area with cyber styling
        JPanel chatAreaPanel = createCyberChatArea();
        mainPanel.add(chatAreaPanel, BorderLayout.CENTER);
        
        // Bottom input panel with neon styling
        JPanel inputPanel = createCyberInputPanel();
        mainPanel.add(inputPanel, BorderLayout.SOUTH);
        
        return mainPanel;
    }
    
    /**
     * Create cyberpunk header panel with status and controls - Fixed responsive layout
     */
    private JPanel createCyberHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(DEEP_BLUE);
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_NEON, 2),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        
        // Top status row - responsive layout
        JPanel statusRowPanel = new JPanel(new BorderLayout());
        statusRowPanel.setBackground(DEEP_BLUE);
        
        // Left side - connection status with matrix styling
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        statusPanel.setBackground(DEEP_BLUE);
        
        JLabel lockIcon = new JLabel("⚡");
        lockIcon.setFont(new Font("Segoe UI Emoji", Font.BOLD, 16));
        lockIcon.setForeground(MATRIX_GREEN);
        statusPanel.add(lockIcon);
        
        connectionStatusLabel = new JLabel("DISCONNECTED - AWAITING TARGET");
        connectionStatusLabel.setFont(new Font("Consolas", Font.BOLD, 12));
        connectionStatusLabel.setForeground(DANGER_RED);
        statusPanel.add(connectionStatusLabel);
        
        statusRowPanel.add(statusPanel, BorderLayout.WEST);
        
        // Right side - security info and timer with neon styling
        JPanel securityPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        securityPanel.setBackground(DEEP_BLUE);
        
        // Timer countdown label with cyberpunk styling
        timerLabel = new JLabel("");
        timerLabel.setFont(new Font("Consolas", Font.BOLD, 12));
        timerLabel.setForeground(WARNING_AMBER);
        timerLabel.setVisible(false);
        securityPanel.add(timerLabel);
        
        JLabel encryptionLabel = new JLabel("🛡️ SELF-DESTRUCTIVE_SECURITY");
        encryptionLabel.setFont(new Font("Consolas", Font.BOLD, 10));
        encryptionLabel.setForeground(SUCCESS_GREEN);
        securityPanel.add(encryptionLabel);
        
        statusRowPanel.add(securityPanel, BorderLayout.EAST);
        
        headerPanel.add(statusRowPanel, BorderLayout.NORTH);
        
        // Connection controls panel with cyber theme - Improved responsive design
        JPanel controlsPanel = new JPanel();
        controlsPanel.setLayout(new BoxLayout(controlsPanel, BoxLayout.Y_AXIS));
        controlsPanel.setBackground(CARD_DARK);
        controlsPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_NEON, 1),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        
        // Top row - Connection controls
        JPanel topRowPanel = new JPanel(new BorderLayout());
        topRowPanel.setBackground(CARD_DARK);
        
        // Connection request section with responsive layout
        JPanel requestPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        requestPanel.setBackground(CARD_DARK);
        
        JLabel connectLabel = new JLabel(">> TARGET_ID:");
        connectLabel.setFont(new Font("Consolas", Font.BOLD, 11));
        connectLabel.setForeground(TEXT_GREEN);
        requestPanel.add(connectLabel);
        
        recipientField = new JTextField(10);
        recipientField.setFont(new Font("Consolas", Font.BOLD, 11));
        recipientField.setBackground(DEEP_BLUE);
        recipientField.setForeground(MATRIX_GREEN);
        recipientField.setCaretColor(NEON_CYAN);
        recipientField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_NEON, 1),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        requestPanel.add(recipientField);
        
        connectToUserButton = createCyberButton("⚡ CONNECT", SUCCESS_GREEN);
        connectToUserButton.addActionListener(e -> sendConnectionRequest());
        requestPanel.add(connectToUserButton);
        
        disconnectButton = createCyberButton("⚡ DISCONNECT", DANGER_RED);
        disconnectButton.addActionListener(e -> disconnectFromUser());
        disconnectButton.setEnabled(false);
        requestPanel.add(disconnectButton);
        
        topRowPanel.add(requestPanel, BorderLayout.WEST);
        
        // Bottom row - Management controls  
        JPanel bottomRowPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        bottomRowPanel.setBackground(CARD_DARK);
        
        clearChatButton = createCyberButton("🧹 PURGE", NEON_CYAN);
        clearChatButton.addActionListener(e -> clearChat());
        clearChatButton.setEnabled(false);
        bottomRowPanel.add(clearChatButton);
        
        setTimerButton = createCyberButton("⏱️ TIMER", WARNING_AMBER);
        setTimerButton.addActionListener(e -> setTimer());
        setTimerButton.setEnabled(false);
        bottomRowPanel.add(setTimerButton);
        
        downloadHistoryButton = createCyberButton("📥 FILES", NEON_CYAN);
        downloadHistoryButton.addActionListener(e -> showDownloadHistory());
        downloadHistoryButton.setEnabled(false);
        bottomRowPanel.add(downloadHistoryButton);
        
        destroyChatButton = createCyberButton("💥 DESTROY", DANGER_RED);
        destroyChatButton.addActionListener(e -> destroyChat());
        destroyChatButton.setEnabled(false);
        bottomRowPanel.add(destroyChatButton);
        
        controlsPanel.add(topRowPanel);
        controlsPanel.add(bottomRowPanel);
        
        headerPanel.add(controlsPanel, BorderLayout.SOUTH);
        
        return headerPanel;
    }
    
    /**
     * Create cyberpunk chat area with terminal-style messages
     */
    private JPanel createCyberChatArea() {
        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.setBackground(CARD_DARK);
        chatPanel.setBorder(BorderFactory.createLineBorder(BORDER_NEON, 2));
        
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        chatArea.setBackground(DARK_MATRIX);
        chatArea.setForeground(MATRIX_GREEN);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setCaretColor(NEON_CYAN);
        chatArea.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        JScrollPane scrollPane = new JScrollPane(chatArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getViewport().setBackground(DARK_MATRIX);
        scrollPane.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(BORDER_NEON, 1), 
            ">> ENCRYPTED_CHANNEL <<", 
            TitledBorder.LEFT, 
            TitledBorder.TOP,
            new Font("Consolas", Font.BOLD, 12),
            MATRIX_GREEN
        ));
        
        chatPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Add typing indicator at the bottom with cyber styling
        typingIndicatorLabel = new JLabel(" ");
        typingIndicatorLabel.setFont(new Font("Consolas", Font.ITALIC, 11));
        typingIndicatorLabel.setForeground(NEON_CYAN);
        typingIndicatorLabel.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        typingIndicatorLabel.setVisible(false);
        chatPanel.add(typingIndicatorLabel, BorderLayout.SOUTH);
        
        return chatPanel;
    }
    
    /**
     * Create cyberpunk input panel for messages and files
     */
    private JPanel createCyberInputPanel() {
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBackground(CARD_DARK);
        inputPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_NEON, 2),
            BorderFactory.createEmptyBorder(20, 25, 20, 25)
        ));
        
        // Message input field with cyber styling
        messageField = new JTextField();
        messageField.setFont(new Font("Consolas", Font.BOLD, 14));
        messageField.setBackground(DARK_MATRIX);
        messageField.setForeground(MATRIX_GREEN);
        messageField.setCaretColor(NEON_CYAN);
        messageField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_NEON, 2),
            BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));
        messageField.addActionListener(e -> sendMessage());
        messageField.setEnabled(false);
        
        // Add typing indicator functionality
        messageField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) { onTyping(); }
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) { onTyping(); }
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) { onTyping(); }
        });
        
        inputPanel.add(messageField, BorderLayout.CENTER);
        
        // Button panel with cyber styling
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setBackground(CARD_DARK);
        
        sendFileButton = createCyberButton("📎 FILE", NEON_CYAN);
        sendFileButton.addActionListener(e -> sendFile());
        sendFileButton.setEnabled(false);
        buttonPanel.add(sendFileButton);
        
        sendButton = createCyberButton("🚀 TRANSMIT", SUCCESS_GREEN);
        sendButton.addActionListener(e -> sendMessage());
        sendButton.setEnabled(false);
        buttonPanel.add(sendButton);
        
        inputPanel.add(buttonPanel, BorderLayout.EAST);
        
        return inputPanel;
    }
    
    /**
     * Create a cyberpunk-styled button with neon effects and responsive sizing
     */
    private JButton createCyberButton(String text, Color accentColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Consolas", Font.BOLD, 10));
        button.setBackground(CARD_DARK);
        button.setForeground(accentColor);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(accentColor, 2),
            BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));
        // Responsive button sizing - adapt to content
        FontMetrics fm = button.getFontMetrics(button.getFont());
        int textWidth = fm.stringWidth(text);
        int buttonWidth = Math.max(90, textWidth + 30);
        button.setPreferredSize(new Dimension(buttonWidth, 32));
        button.setMinimumSize(new Dimension(90, 32));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Add neon glow hover effect
        Color originalColor = accentColor;
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(DEEP_BLUE);
                button.setForeground(NEON_CYAN);
                button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(NEON_CYAN, 3),
                    BorderFactory.createEmptyBorder(5, 11, 5, 11)
                ));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(CARD_DARK);
                button.setForeground(originalColor);
                button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(originalColor, 2),
                    BorderFactory.createEmptyBorder(6, 12, 6, 12)
                ));
            }
        });
        
        return button;
    }
    
    /**
     * Show cyberpunk-styled message dialog with proper color visibility
     */
    private void showCyberMessageDialog(String message, String title, int messageType) {
        // Create custom dialog with cyberpunk styling
        JDialog dialog = new JDialog(this, title, true);
        dialog.setSize(400, 200);
        dialog.setLocationRelativeTo(this);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(DARK_MATRIX);
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_NEON, 2),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        
        // Message label with proper colors
        JLabel messageLabel = new JLabel("<html><div style='text-align:center;'>" + message + "</div></html>");
        messageLabel.setFont(new Font("Consolas", Font.BOLD, 14));
        messageLabel.setForeground(MATRIX_GREEN);
        messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        mainPanel.add(messageLabel, BorderLayout.CENTER);
        
        // OK button
        JButton okButton = createCyberButton("OK", NEON_CYAN);
        okButton.addActionListener(e -> dialog.dispose());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBackground(DARK_MATRIX);
        buttonPanel.add(okButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.add(mainPanel);
        dialog.setVisible(true);
    }
    
    /**
     * Show cyberpunk-styled confirmation dialog
     */
    private boolean showCyberConfirmDialog(String message, String title) {
        JDialog dialog = new JDialog(this, title, true);
        dialog.setSize(450, 220);
        dialog.setLocationRelativeTo(this);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(DARK_MATRIX);
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_NEON, 2),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        
        // Message label
        JLabel messageLabel = new JLabel("<html><div style='text-align:center;'>" + message + "</div></html>");
        messageLabel.setFont(new Font("Consolas", Font.BOLD, 14));
        messageLabel.setForeground(MATRIX_GREEN);
        messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        mainPanel.add(messageLabel, BorderLayout.CENTER);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        buttonPanel.setBackground(DARK_MATRIX);
        
        final boolean[] result = {false};
        
        JButton yesButton = createCyberButton("YES", SUCCESS_GREEN);
        yesButton.addActionListener(e -> {
            result[0] = true;
            dialog.dispose();
        });
        buttonPanel.add(yesButton);
        
        JButton noButton = createCyberButton("NO", DANGER_RED);
        noButton.addActionListener(e -> {
            result[0] = false;
            dialog.dispose();
        });
        buttonPanel.add(noButton);
        
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        dialog.add(mainPanel);
        dialog.setVisible(true);
        
        return result[0];
    }
    
    /**
     * Connect to the chat server
     */
    private void connect() {
        String username = usernameField.getText().trim();
        if (username.isEmpty()) {
            statusLabel.setText("❌ IDENTITY_REQUIRED");
            statusLabel.setForeground(DANGER_RED);
            return;
        }
        
        try {
            statusLabel.setText("🔄 ESTABLISHING_QUANTUM_TUNNEL...");
            statusLabel.setForeground(NEON_CYAN);
            
            // Create socket connection
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            output = new ObjectOutputStream(socket.getOutputStream());
            input = new ObjectInputStream(socket.getInputStream());
            
            // Receive session key from server
            Message keyMessage = (Message) input.readObject();
            if (keyMessage.getType() == Message.MessageType.SYSTEM) {
                String keyString = keyMessage.getContent();
                sessionKey = EncryptionUtil.stringToKey(keyString);
            }
            
            // Send join message
            Message joinMessage = new Message(username, "", "JOIN", Message.MessageType.JOIN);
            output.writeObject(joinMessage);
            output.flush();
            
            // Wait for acknowledgment
            Message ackMessage = (Message) input.readObject();
            if (ackMessage.getType() == Message.MessageType.SYSTEM) {
                String response = ackMessage.getContent();
                
                // Check if it's an error message (username already taken)
                if (response.contains("already taken")) {
                    // Show error message to user
                    JOptionPane.showMessageDialog(this, 
                        response, 
                        "Username Error", 
                        JOptionPane.ERROR_MESSAGE);
                    
                    // Close connection and return to login
                    disconnect();
                    return;
                }
                
                // Positive acknowledgment - proceed with connection
                this.username = username;
                this.isConnected = true;
                
                // Update window title with username
                setTitle("SecureChat - " + username + " [SELF-DESTRUCTIVE]");
                
                // Switch to chat interface
                getContentPane().removeAll();
                add(chatPanel, BorderLayout.CENTER);
                revalidate();
                repaint();
                
                // Start message listener thread
                new Thread(this::listenForMessages).start();
                
                // Start heartbeat
                startHeartbeat();
                
                appendToChatArea(">> CONNECTED_TO_SECURE_NETWORK: " + username);
                
            } else {
                statusLabel.setText("❌ CONNECTION_FAILED");
                statusLabel.setForeground(DANGER_RED);
            }
            
        } catch (Exception e) {
            statusLabel.setText("❌ Connection error: " + e.getMessage());
            statusLabel.setForeground(DANGER_RED);
            e.printStackTrace();
        }
    }
    
    /**
     * Listen for incoming messages
     */
    private void listenForMessages() {
        try {
            while (isConnected && !socket.isClosed()) {
                Message message = (Message) input.readObject();
                handleIncomingMessage(message);
            }
        } catch (Exception e) {
            if (isConnected) {
                appendToChatArea("Connection lost: " + e.getMessage());
                disconnect();
            }
        }
    }
    
    /**
     * Handle incoming messages from server
     */
    private void handleIncomingMessage(Message message) {
        SwingUtilities.invokeLater(() -> {
            try {
                switch (message.getType()) {
                    case TEXT:
                        String decryptedText = EncryptionUtil.decrypt(message.getContent(), sessionKey);
                        
                        // Verify digital signature if present
                        String signatureStatus = "";
                        if (message.getDigitalSignature() != null && message.getSignerPublicKey() != null) {
                            boolean signatureValid = DigitalSignatureUtil.verifySignature(
                                decryptedText, message.getDigitalSignature(), message.getSignerPublicKey());
                            signatureStatus = signatureValid ? " ✅" : " ⚠️";
                        }
                        
                        appendToChatArea(message.getSender() + ": " + decryptedText + signatureStatus);
                        
                        // Send read receipt
                        sendReadReceipt(message.getMessageId(), message.getSender());
                        break;
                        
                    case FILE:
                        byte[] decryptedFile = EncryptionUtil.decryptBytes(message.getFileData(), sessionKey);
                        handleIncomingFile(message.getSender(), message.getFileName(), decryptedFile);
                        break;
                        
                    case SYSTEM:
                        String systemMessage = message.getContent();
                        appendToChatArea("[SYSTEM] " + systemMessage);
                        
                        // Check for connection status changes
                        if (systemMessage.contains("You can now start chatting!")) {
                            // Extract the connected user's name
                            String connectedUser = "";
                            if (systemMessage.contains("Connected to ")) {
                                connectedUser = systemMessage.substring(systemMessage.indexOf("Connected to ") + 13, 
                                    systemMessage.indexOf(". You can now start chatting!"));
                            } else if (systemMessage.contains(" accepted your request")) {
                                connectedUser = systemMessage.substring(0, systemMessage.indexOf(" accepted your request"));
                            }
                            setConnectionStatus(true, connectedUser);
                        } else if (systemMessage.contains("disconnected") || systemMessage.contains("has left")) {
                            setConnectionStatus(false, "");
                        }
                        break;
                        
                    case CONNECT_REQUEST:
                        handleConnectionRequest(message);
                        break;
                        
                    case CLEAR_LOCAL_CHAT:
                        if ("CLEAR_CHAT_AREA".equals(message.getContent())) {
                            // Clear local chat area when other user clears for both
                            chatArea.setText("");
                            appendToChatArea("Chat history cleared by " + message.getSender());
                        } else {
                            appendToChatArea("[SYSTEM] " + message.getContent());
                        }
                        break;
                        
                    case DESTROY_CHAT:
                        if ("CLOSE_APPLICATION".equals(message.getContent())) {
                            appendToChatArea("[SYSTEM] Chat session destroyed. Application will close.");
                            // Close the application after a short delay
                            Timer timer = new Timer(2000, e -> {
                                disconnect();
                                System.exit(0);
                            });
                            timer.setRepeats(false);
                            timer.start();
                        }
                        break;
                        
                    case TIMER_UPDATE:
                        handleTimerUpdate(message);
                        break;
                        
                    case TIMER_EXPIRED:
                        handleTimerExpired(message);
                        break;
                        
                    case HEARTBEAT:
                        // Heartbeat acknowledged
                        break;
                        
                    case TYPING_START:
                        showTypingIndicator(message.getSender());
                        break;
                        
                    case TYPING_STOP:
                        hideTypingIndicator();
                        break;
                        
                    case DELIVERY_RECEIPT:
                        handleDeliveryReceipt(message);
                        break;
                        
                    case READ_RECEIPT:
                        handleReadReceipt(message);
                        break;
                        
                    default:
                        appendToChatArea("[UNKNOWN] " + message.getContent());
                }
            } catch (Exception e) {
                appendToChatArea("[ERROR] Failed to process message: " + e.getMessage());
            }
        });
    }
    
    /**
     * Send connection request to another user
     */
    private void sendConnectionRequest() {
        String recipient = recipientField.getText().trim();
        if (recipient.isEmpty()) {
            showCyberMessageDialog("Please enter a username to connect to", "Connection Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (recipient.equals(username)) {
            showCyberMessageDialog("You cannot connect to yourself", "Connection Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        try {
            Message connectRequest = new Message(username, recipient, 
                "CONNECTION_REQUEST", Message.MessageType.CONNECT_REQUEST);
            output.writeObject(connectRequest);
            output.flush();
            
            appendToChatArea("Sending connection request to " + recipient + "...");
            
        } catch (Exception e) {
            appendToChatArea("[ERROR] Failed to send connection request: " + e.getMessage());
        }
    }
    
    /**
     * Disconnect from current user
     */
    private void disconnectFromUser() {
        try {
            Message disconnectRequest = new Message(username, "", 
                "DISCONNECT_REQUEST", Message.MessageType.DISCONNECT_REQUEST);
            output.writeObject(disconnectRequest);
            output.flush();
            
            // Update UI
            setConnectionStatus(false, "");
            
        } catch (Exception e) {
            appendToChatArea("[ERROR] Failed to disconnect: " + e.getMessage());
        }
    }
    
    /**
     * Handle incoming connection request
     */
    private void handleConnectionRequest(Message message) {
        String requester = message.getSender();
        
        int option = JOptionPane.showConfirmDialog(this,
            requester + " wants to start a chat with you.\n" +
            "Do you want to accept this connection request?",
            "Connection Request",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);
        
        try {
            if (option == JOptionPane.YES_OPTION) {
                // Accept the connection
                Message acceptMessage = new Message(username, requester,
                    "ACCEPT", Message.MessageType.CONNECT_ACCEPT);
                output.writeObject(acceptMessage);
                output.flush();
                
                // Update UI for connection
                setConnectionStatus(true, requester);
                appendToChatArea("Accepted connection request from " + requester);
                
            } else {
                // Reject the connection
                Message rejectMessage = new Message(username, requester,
                    "REJECT", Message.MessageType.CONNECT_REJECT);
                output.writeObject(rejectMessage);
                output.flush();
                
                appendToChatArea("Rejected connection request from " + requester);
            }
        } catch (Exception e) {
            appendToChatArea("[ERROR] Failed to respond to connection request: " + e.getMessage());
        }
    }
    
    /**
     * Update connection status in UI
     */
    private void setConnectionStatus(boolean connected, String connectedTo) {
        if (connected && !connectedTo.isEmpty()) {
            connectionStatusLabel.setText("🔐 Securely connected to " + connectedTo);
            connectionStatusLabel.setForeground(SUCCESS_GREEN);
            recipientField.setText(connectedTo);
            recipientField.setEditable(false);
            
            // Enable chat controls
            messageField.setEnabled(true);
            sendButton.setEnabled(true);
            sendFileButton.setEnabled(true);
            clearChatButton.setEnabled(true);
            destroyChatButton.setEnabled(true);
            setTimerButton.setEnabled(true);
            downloadHistoryButton.setEnabled(true);
            
            // Update connection buttons
            connectToUserButton.setEnabled(false);
            disconnectButton.setEnabled(true);
            
        } else {
            connectionStatusLabel.setText("🔓 Not connected to anyone");
            connectionStatusLabel.setForeground(DANGER_RED);
            recipientField.setText("");
            recipientField.setEditable(true);
            
            // Disable chat controls
            messageField.setEnabled(false);
            sendButton.setEnabled(false);
            sendFileButton.setEnabled(false);
            clearChatButton.setEnabled(false);
            destroyChatButton.setEnabled(false);
            setTimerButton.setEnabled(false);
            downloadHistoryButton.setEnabled(false);
            
            // Update connection buttons
            connectToUserButton.setEnabled(true);
            disconnectButton.setEnabled(false);
            
            // Clear chat area
            chatArea.setText("");
        }
    }

    /**
     * Send text message
     */
    /**
     * Send text message with digital signature and delivery tracking
     */
    private void sendMessage() {
        String recipient = recipientField.getText().trim();
        String messageText = messageField.getText().trim();
        
        if (recipient.isEmpty() || messageText.isEmpty()) {
            return;
        }
        
        try {
            // Stop typing indicator
            sendTypingStop();
            
            // Encrypt message
            String encryptedText = EncryptionUtil.encrypt(messageText, sessionKey);
            
            // Create message with digital signature
            Message message = new Message(username, recipient, encryptedText, Message.MessageType.TEXT);
            
            // Add digital signature
            try {
                byte[] signature = DigitalSignatureUtil.signMessage(messageText, username);
                String publicKey = DigitalSignatureUtil.getUserPublicKeyString(username);
                message.setDigitalSignature(signature);
                message.setSignerPublicKey(publicKey);
            } catch (Exception e) {
                System.err.println("Warning: Could not sign message: " + e.getMessage());
            }
            
            // Track delivery status
            deliveryStatus.put(message.getMessageId(), "SENT");
            
            output.writeObject(message);
            output.flush();
            
            // Display in chat area with delivery status
            appendToChatArea("You: " + messageText + " ✓");
            messageField.setText("");
            
        } catch (Exception e) {
            appendToChatArea("[ERROR] Failed to send message: " + e.getMessage());
        }
    }
    
    /**
     * Send file
     */
    private void sendFile() {
        String recipient = recipientField.getText().trim();
        if (recipient.isEmpty()) {
            showCyberMessageDialog("Please specify a recipient", "Recipient Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);
        
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            
            try {
                // Read file data
                FileInputStream fis = new FileInputStream(selectedFile);
                byte[] fileData = fis.readAllBytes();
                fis.close();
                
                // Encrypt file data
                byte[] encryptedFile = EncryptionUtil.encryptBytes(fileData, sessionKey);
                
                // Create and send file message
                Message fileMessage = new Message(username, recipient, "FILE_TRANSFER", Message.MessageType.FILE);
                fileMessage.setFileName(selectedFile.getName());
                fileMessage.setFileData(encryptedFile);
                fileMessage.setFileSize(fileData.length);
                
                output.writeObject(fileMessage);
                output.flush();
                
                appendToChatArea("You sent file: " + selectedFile.getName() + 
                    " (" + fileData.length + " bytes)");
                
            } catch (Exception e) {
                appendToChatArea("[ERROR] Failed to send file: " + e.getMessage());
            }
        }
    }
    
    /**
     * Handle incoming file
     */
    private void handleIncomingFile(String sender, String fileName, byte[] fileData) {
        appendToChatArea(sender + " sent file: " + fileName + " (" + fileData.length + " bytes)");
        
        // Add to received files history
        ReceivedFile receivedFile = new ReceivedFile(fileName, sender, fileData);
        receivedFiles.add(receivedFile);
        
        int option = JOptionPane.showConfirmDialog(this, 
            "Save file '" + fileName + "' from " + sender + " now?\n(You can also download it later from File History)", 
            "File Received", JOptionPane.YES_NO_OPTION);
        
        if (option == JOptionPane.YES_OPTION) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setSelectedFile(new File(fileName));
            
            int result = fileChooser.showSaveDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                try {
                    File saveFile = fileChooser.getSelectedFile();
                    FileOutputStream fos = new FileOutputStream(saveFile);
                    fos.write(fileData);
                    fos.close();
                    
                    // Mark as downloaded
                    receivedFile.downloaded = true;
                    
                    appendToChatArea("File saved: " + saveFile.getAbsolutePath());
                } catch (Exception e) {
                    appendToChatArea("[ERROR] Failed to save file: " + e.getMessage());
                }
            }
        } else {
            appendToChatArea("[INFO] File stored in download history - access via File History button");
        }
    }
    
    /**
     * Clear chat history (keep session active)
     */
    private void clearChat() {
        String recipient = recipientField.getText().trim();
        if (recipient.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please specify the chat recipient");
            return;
        }
        
        // Show choice dialog for clear options
        String[] options = {
            "Clear for both users", 
            "Clear only for me", 
            "Cancel"
        };
        
        int choice = JOptionPane.showOptionDialog(this,
            "Choose how to clear the chat history:",
            "Clear Chat Options",
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[2]); // Default to Cancel
        
        if (choice == 0) { // Clear for both users
            clearChatForBothUsers(recipient);
        } else if (choice == 1) { // Clear only for me
            clearChatForLocalUser(recipient);
        }
        // If choice == 2 or dialog closed, do nothing (cancel)
    }
    
    /**
     * Clear chat for both users (server-side clearing)
     */
    private void clearChatForBothUsers(String recipient) {
        int confirm = JOptionPane.showConfirmDialog(this,
            "This will clear chat history for both you and " + recipient + ".\n" +
            "Are you sure you want to continue?",
            "Clear Chat for Both Users", 
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                Message clearMessage = new Message(username, recipient, 
                    "CLEAR_CHAT", Message.MessageType.CLEAR_CHAT);
                output.writeObject(clearMessage);
                output.flush();
                
                // Clear local chat area
                chatArea.setText("");
                appendToChatArea("Chat history cleared for both users");
                
            } catch (Exception e) {
                appendToChatArea("[ERROR] Failed to clear chat: " + e.getMessage());
            }
        }
    }
    
    /**
     * Clear chat only for local user (client-side clearing)
     */
    private void clearChatForLocalUser(String recipient) {
        int confirm = JOptionPane.showConfirmDialog(this,
            "This will only clear your local chat history.\n" +
            recipient + " will still see all previous messages.\n" +
            "Are you sure you want to continue?",
            "Clear Local Chat Only", 
            JOptionPane.YES_NO_OPTION,
            JOptionPane.INFORMATION_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                // Send message to server for logging
                Message clearMessage = new Message(username, recipient, 
                    "CLEAR_LOCAL_CHAT", Message.MessageType.CLEAR_LOCAL_CHAT);
                output.writeObject(clearMessage);
                output.flush();
                
                // Clear only local chat area
                chatArea.setText("");
                appendToChatArea("Your local chat history cleared");
                
            } catch (Exception e) {
                appendToChatArea("[ERROR] Failed to clear local chat: " + e.getMessage());
            }
        }
    }

    /**
     * Destroy chat session (exit both clients)
     */
    /**
     * Destroy chat session (exit both clients)
     */
    private void destroyChat() {
        String recipient = recipientField.getText().trim();
        if (recipient.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please specify the chat recipient");
            return;
        }
        
        int option = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to destroy the chat session with " + recipient + "?\n" +
            "This will close both your applications and end the session completely.",
            "Destroy Chat Session", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        
        if (option == JOptionPane.YES_OPTION) {
            try {
                Message destroyMessage = new Message(username, recipient, 
                    "DESTROY_CHAT", Message.MessageType.DESTROY_CHAT);
                output.writeObject(destroyMessage);
                output.flush();
            } catch (Exception e) {
                appendToChatArea("[ERROR] Failed to destroy chat: " + e.getMessage());
            }
        }
    }
    
    /**
     * Set chat timer
     */
    private void setTimer() {
        String recipient = recipientField.getText().trim();
        if (recipient.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please specify the chat recipient", 
                "Timer Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Create custom dialog for timer input
        JDialog timerDialog = new JDialog(this, "⏱️ Set Destruction Timer", true);
        timerDialog.setSize(400, 250);
        timerDialog.setLocationRelativeTo(this);
        timerDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(DARK_MATRIX);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Header
        JLabel headerLabel = new JLabel("💥 Auto-Destruction Timer");
        headerLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        headerLabel.setForeground(DANGER_RED);
        headerLabel.setHorizontalAlignment(SwingConstants.CENTER);
        mainPanel.add(headerLabel, BorderLayout.NORTH);
        
        // Input panel
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBackground(DARK_MATRIX);
        GridBagConstraints gbc = new GridBagConstraints();
        
        // Minutes input
        gbc.gridx = 0; gbc.gridy = 0; gbc.insets = new Insets(10, 5, 5, 5);
        inputPanel.add(new JLabel("Minutes:"), gbc);
        
        JSpinner minutesSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 60, 1));
        minutesSpinner.setPreferredSize(new Dimension(80, 30));
        gbc.gridx = 1;
        inputPanel.add(minutesSpinner, gbc);
        
        // Seconds input
        gbc.gridx = 2; gbc.insets = new Insets(10, 15, 5, 5);
        inputPanel.add(new JLabel("Seconds:"), gbc);
        
        JSpinner secondsSpinner = new JSpinner(new SpinnerNumberModel(30, 0, 59, 1));
        secondsSpinner.setPreferredSize(new Dimension(80, 30));
        gbc.gridx = 3;
        inputPanel.add(secondsSpinner, gbc);
        
        // Warning message
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 4; gbc.insets = new Insets(15, 0, 10, 0);
        JLabel warningLabel = new JLabel("<html><center>⚠️ WARNING: Chat will be permanently destroyed<br>when timer expires!</center></html>");
        warningLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        warningLabel.setForeground(DANGER_RED);
        warningLabel.setHorizontalAlignment(SwingConstants.CENTER);
        inputPanel.add(warningLabel, gbc);
        
        mainPanel.add(inputPanel, BorderLayout.CENTER);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setBackground(DARK_MATRIX);
        
        JButton setButton = createCyberButton("⏱️ Start Timer", DANGER_RED);
        JButton cancelButton = createCyberButton("❌ Cancel", NEON_CYAN);
        
        setButton.addActionListener(e -> {
            int minutes = (Integer) minutesSpinner.getValue();
            int seconds = (Integer) secondsSpinner.getValue();
            
            if (minutes == 0 && seconds == 0) {
                JOptionPane.showMessageDialog(timerDialog, 
                    "Please set at least 1 second!", 
                    "Invalid Timer", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            try {
                // Convert to total seconds
                long totalSeconds = minutes * 60L + seconds;
                
                Message timerMessage = new Message(username, recipient,
                    "SET_TIMER", Message.MessageType.SET_TIMER);
                timerMessage.setTimerDuration(totalSeconds); // Store in seconds
                
                output.writeObject(timerMessage);
                output.flush();
                
                timerDialog.dispose();
                
            } catch (Exception ex) {
                appendToChatArea("[ERROR] Failed to set timer: " + ex.getMessage());
            }
        });
        
        cancelButton.addActionListener(e -> timerDialog.dispose());
        
        buttonPanel.add(setButton);
        buttonPanel.add(cancelButton);
        
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        timerDialog.add(mainPanel);
        timerDialog.setVisible(true);
    }
    
    /**
     * Show download history dialog with received files
     */
    private void showDownloadHistory() {
        if (receivedFiles.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "No files received yet in this session", 
                "File History Empty", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        // Create download history dialog
        JDialog historyDialog = new JDialog(this, "📥 File Download History", true);
        historyDialog.setSize(600, 400);
        historyDialog.setLocationRelativeTo(this);
        historyDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(DARK_MATRIX);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Header
        JLabel headerLabel = new JLabel("📁 Received Files History");
        headerLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        headerLabel.setForeground(MATRIX_GREEN);
        headerLabel.setHorizontalAlignment(SwingConstants.CENTER);
        mainPanel.add(headerLabel, BorderLayout.NORTH);
        
        // Files list panel
        JPanel filesPanel = new JPanel();
        filesPanel.setLayout(new BoxLayout(filesPanel, BoxLayout.Y_AXIS));
        filesPanel.setBackground(DARK_MATRIX);
        
        for (ReceivedFile file : receivedFiles) {
            JPanel fileRowPanel = new JPanel(new BorderLayout());
            fileRowPanel.setBackground(DARK_MATRIX);
            fileRowPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(MATRIX_GREEN, 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
            ));
            fileRowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
            
            // File info
            String statusIcon = file.downloaded ? "✅" : "📁";
            String statusText = file.downloaded ? " (Downloaded)" : " (Not Downloaded)";
            
            JLabel fileInfoLabel = new JLabel(statusIcon + " " + file.fileName + statusText);
            fileInfoLabel.setForeground(NEON_CYAN);
            fileInfoLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
            
            JLabel detailsLabel = new JLabel("From: " + file.sender + " | " + 
                file.receivedTime.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, HH:mm")));
            detailsLabel.setForeground(MATRIX_GREEN);
            detailsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            
            JPanel infoPanel = new JPanel(new BorderLayout());
            infoPanel.setBackground(DARK_MATRIX);
            infoPanel.add(fileInfoLabel, BorderLayout.NORTH);
            infoPanel.add(detailsLabel, BorderLayout.SOUTH);
            
            // Download button
            JButton downloadBtn = createCyberButton("💾 Download", SUCCESS_GREEN);
            downloadBtn.addActionListener(e -> downloadFile(file, historyDialog));
            
            fileRowPanel.add(infoPanel, BorderLayout.CENTER);
            fileRowPanel.add(downloadBtn, BorderLayout.EAST);
            
            filesPanel.add(fileRowPanel);
            filesPanel.add(Box.createVerticalStrut(5));
        }
        
        JScrollPane scrollPane = new JScrollPane(filesPanel);
        scrollPane.setBackground(DARK_MATRIX);
        scrollPane.getViewport().setBackground(DARK_MATRIX);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Close button
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setBackground(DARK_MATRIX);
        
        JButton closeButton = createCyberButton("❌ Close", DANGER_RED);
        closeButton.addActionListener(e -> historyDialog.dispose());
        buttonPanel.add(closeButton);
        
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        historyDialog.add(mainPanel);
        historyDialog.setVisible(true);
    }
    
    /**
     * Download a file from history
     */
    private void downloadFile(ReceivedFile file, JDialog parentDialog) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File(file.fileName));
        
        int result = fileChooser.showSaveDialog(parentDialog);
        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                File saveFile = fileChooser.getSelectedFile();
                FileOutputStream fos = new FileOutputStream(saveFile);
                fos.write(file.fileData);
                fos.close();
                
                // Mark as downloaded
                file.downloaded = true;
                
                JOptionPane.showMessageDialog(parentDialog, 
                    "File saved successfully to:\n" + saveFile.getAbsolutePath(),
                    "Download Complete", JOptionPane.INFORMATION_MESSAGE);
                
                // Refresh the dialog to show updated status
                parentDialog.dispose();
                showDownloadHistory();
                
            } catch (Exception e) {
                JOptionPane.showMessageDialog(parentDialog,
                    "Failed to save file: " + e.getMessage(),
                    "Download Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    /**
     * Start heartbeat to keep connection alive
     */
    private void startHeartbeat() {
        heartbeatScheduler.scheduleAtFixedRate(() -> {
            if (isConnected) {
                try {
                    Message heartbeat = new Message(username, "SERVER", 
                        "HEARTBEAT", Message.MessageType.HEARTBEAT);
                    output.writeObject(heartbeat);
                    output.flush();
                } catch (Exception e) {
                    appendToChatArea("[ERROR] Heartbeat failed: " + e.getMessage());
                }
            }
        }, 30, 30, TimeUnit.SECONDS);
    }
    
    /**
     * Append message to chat area
     */
    private void appendToChatArea(String message) {
        SwingUtilities.invokeLater(() -> {
            chatArea.append("[" + java.time.LocalTime.now().toString() + "] " + message + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }
    
    /**
     * Disconnect from server
     */
    private void disconnect() {
        isConnected = false;
        
        try {
            if (heartbeatScheduler != null) {
                heartbeatScheduler.shutdown();
            }
            
            if (input != null) input.close();
            if (output != null) output.close();
            if (socket != null) socket.close();
            
        } catch (IOException e) {
            System.err.println("Error during disconnect: " + e.getMessage());
        }
        
        System.out.println("Disconnected from server");
    }
    
    /**
     * Handle timer countdown updates
     */
    private void handleTimerUpdate(Message message) {
        SwingUtilities.invokeLater(() -> {
            try {
                long remainingSeconds = message.getTimerDuration();
                
                if (remainingSeconds > 0) {
                    // Format time display
                    long minutes = remainingSeconds / 60;
                    long seconds = remainingSeconds % 60;
                    String timeDisplay = String.format("⏱️ %02d:%02d", minutes, seconds);
                    
                    // Update timer label
                    timerLabel.setText(timeDisplay);
                    timerLabel.setVisible(true);
                    
                    // Add warning color based on remaining time
                    if (remainingSeconds <= 10) {
                        timerLabel.setForeground(DANGER_RED);
                        // Blink effect for last 10 seconds
                        Timer blinkTimer = new Timer(500, e -> {
                            timerLabel.setVisible(!timerLabel.isVisible());
                        });
                        blinkTimer.setRepeats(false);
                        blinkTimer.start();
                        
                        Timer showTimer = new Timer(1000, e -> {
                            timerLabel.setVisible(true);
                        });
                        showTimer.setRepeats(false);
                        showTimer.start();
                        
                    } else if (remainingSeconds <= 30) {
                        timerLabel.setForeground(WARNING_AMBER); // Orange
                    } else {
                        timerLabel.setForeground(DANGER_RED);
                    }
                    
                    // Show system message for significant time markers
                    if (remainingSeconds == 60 || remainingSeconds == 30 || remainingSeconds <= 10) {
                        String warningMsg = "⚠️ DESTRUCTION WARNING: Chat will be destroyed in " + timeDisplay.substring(2);
                        appendToChatArea("[SYSTEM] " + warningMsg);
                    }
                } else {
                    timerLabel.setVisible(false);
                }
                
            } catch (Exception e) {
                System.err.println("Error handling timer update: " + e.getMessage());
            }
        });
    }
    
    /**
     * Handle timer expiration
     */
    private void handleTimerExpired(Message message) {
        SwingUtilities.invokeLater(() -> {
            // Hide timer label
            timerLabel.setVisible(false);
            
            // Show dramatic expiration message
            appendToChatArea("[SYSTEM] 💥 TIMER EXPIRED! Chat session is being destroyed...");
            
            // Flash the screen red briefly
            Color originalBg = getContentPane().getBackground();
            getContentPane().setBackground(DANGER_RED);
            
            Timer flashTimer = new Timer(200, e -> {
                getContentPane().setBackground(originalBg);
            });
            flashTimer.setRepeats(false);
            flashTimer.start();
            
            // Close the application after showing the message
            Timer closeTimer = new Timer(3000, e -> {
                disconnect();
                System.exit(0);
            });
            closeTimer.setRepeats(false);
            closeTimer.start();
        });
    }
    
    /**
     * Handle typing event - send typing start notification
     */
    private void onTyping() {
        String recipient = recipientField.getText().trim();
        if (recipient.isEmpty() || !isConnected) {
            return;
        }
        
        try {
            // Cancel existing typing timer
            if (typingTimer != null && typingTimer.isRunning()) {
                typingTimer.stop();
            }
            
            // Send typing start notification
            Message typingMsg = new Message(username, recipient, "TYPING_START", Message.MessageType.TYPING_START);
            output.writeObject(typingMsg);
            output.flush();
            
            // Set timer to automatically stop typing after 3 seconds
            typingTimer = new Timer(3000, e -> sendTypingStop());
            typingTimer.setRepeats(false);
            typingTimer.start();
            
        } catch (Exception e) {
            System.err.println("Error sending typing notification: " + e.getMessage());
        }
    }
    
    /**
     * Send typing stop notification
     */
    private void sendTypingStop() {
        String recipient = recipientField.getText().trim();
        if (recipient.isEmpty() || !isConnected) {
            return;
        }
        
        try {
            Message typingStopMsg = new Message(username, recipient, "TYPING_STOP", Message.MessageType.TYPING_STOP);
            output.writeObject(typingStopMsg);
            output.flush();
        } catch (Exception e) {
            System.err.println("Error sending typing stop notification: " + e.getMessage());
        }
    }
    
    /**
     * Show typing indicator in the chat area
     */
    private void showTypingIndicator(String sender) {
        SwingUtilities.invokeLater(() -> {
            typingIndicatorLabel.setText("💬 " + sender + " is typing...");
            typingIndicatorLabel.setVisible(true);
        });
    }
    
    /**
     * Hide typing indicator
     */
    private void hideTypingIndicator() {
        SwingUtilities.invokeLater(() -> {
            typingIndicatorLabel.setVisible(false);
        });
    }
    
    /**
     * Send read receipt for received message
     */
    private void sendReadReceipt(String messageId, String originalSender) {
        try {
            Message readReceipt = new Message(username, originalSender, 
                "READ:" + messageId, Message.MessageType.READ_RECEIPT);
            output.writeObject(readReceipt);
            output.flush();
        } catch (Exception e) {
            System.err.println("Error sending read receipt: " + e.getMessage());
        }
    }
    
    /**
     * Handle delivery receipt from server
     */
    private void handleDeliveryReceipt(Message message) {
        String content = message.getContent();
        if (content.startsWith("DELIVERED:")) {
            String messageId = content.substring(10);
            deliveryStatus.put(messageId, "DELIVERED");
            
            // Update UI to show delivery status (could update specific message)
            SwingUtilities.invokeLater(() -> {
                // For simplicity, just show in chat area
                // In a full implementation, you'd update the specific message
                System.out.println("📬 Message delivered: " + messageId);
            });
        }
    }
    
    /**
     * Handle read receipt from server
     */
    private void handleReadReceipt(Message message) {
        String content = message.getContent();
        if (content.startsWith("READ:")) {
            String messageId = content.substring(5);
            deliveryStatus.put(messageId, "READ");
            
            // Update UI to show read status
            SwingUtilities.invokeLater(() -> {
                // For simplicity, just show in chat area
                System.out.println("👁️  Message read: " + messageId + " by " + message.getSender());
            });
        }
    }
    
    /**
     * Main method to start the client
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new ChatClient().setVisible(true);
        });
    }
}
