package utils;

import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * DigitalSignatureUtil provides digital signature functionality for message authenticity
 */
public class DigitalSignatureUtil {
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
    private static final String KEY_ALGORITHM = "RSA";
    private static final int KEY_SIZE = 2048;
    
    // Store user key pairs (in production, these would be stored securely)
    private static final Map<String, KeyPair> userKeyPairs = new ConcurrentHashMap<>();
    
    /**
     * Generate RSA key pair for a user
     */
    public static KeyPair generateKeyPair(String username) throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(KEY_ALGORITHM);
        keyGen.initialize(KEY_SIZE);
        KeyPair keyPair = keyGen.generateKeyPair();
        
        // Store key pair for the user
        userKeyPairs.put(username, keyPair);
        
        return keyPair;
    }
    
    /**
     * Get user's key pair
     */
    public static KeyPair getUserKeyPair(String username) {
        return userKeyPairs.get(username);
    }
    
    /**
     * Sign a message with user's private key
     */
    public static byte[] signMessage(String message, String username) throws Exception {
        KeyPair keyPair = userKeyPairs.get(username);
        if (keyPair == null) {
            keyPair = generateKeyPair(username);
        }
        
        Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
        signature.initSign(keyPair.getPrivate());
        signature.update(message.getBytes());
        
        return signature.sign();
    }
    
    /**
     * Verify message signature
     */
    public static boolean verifySignature(String message, byte[] digitalSignature, String publicKeyString) {
        try {
            // Decode public key from string
            PublicKey publicKey = stringToPublicKey(publicKeyString);
            
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initVerify(publicKey);
            signature.update(message.getBytes());
            
            return signature.verify(digitalSignature);
        } catch (Exception e) {
            System.err.println("Error verifying signature: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Convert public key to string for transmission
     */
    public static String publicKeyToString(PublicKey publicKey) {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }
    
    /**
     * Convert string back to public key
     */
    public static PublicKey stringToPublicKey(String publicKeyString) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(publicKeyString);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
        return keyFactory.generatePublic(keySpec);
    }
    
    /**
     * Convert private key to string for storage
     */
    public static String privateKeyToString(PrivateKey privateKey) {
        return Base64.getEncoder().encodeToString(privateKey.getEncoded());
    }
    
    /**
     * Convert string back to private key
     */
    public static PrivateKey stringToPrivateKey(String privateKeyString) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(privateKeyString);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
        return keyFactory.generatePrivate(keySpec);
    }
    
    /**
     * Get user's public key as string
     */
    public static String getUserPublicKeyString(String username) {
        KeyPair keyPair = userKeyPairs.get(username);
        if (keyPair == null) {
            try {
                keyPair = generateKeyPair(username);
            } catch (Exception e) {
                System.err.println("Error generating key pair for " + username + ": " + e.getMessage());
                return null;
            }
        }
        return publicKeyToString(keyPair.getPublic());
    }
    
    /**
     * Initialize key pair for user if not exists
     */
    public static void initializeUserKeys(String username) {
        if (!userKeyPairs.containsKey(username)) {
            try {
                generateKeyPair(username);
                System.out.println("Generated digital signature keys for user: " + username);
            } catch (Exception e) {
                System.err.println("Error initializing keys for " + username + ": " + e.getMessage());
            }
        }
    }
}
