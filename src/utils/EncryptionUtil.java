package utils;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * EncryptionUtil provides AES encryption/decryption for secure communication
 */
public class EncryptionUtil {
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";
    
    /**
     * Generate a new AES secret key
     */
    public static SecretKey generateKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
        keyGenerator.init(256, new SecureRandom());
        return keyGenerator.generateKey();
    }
    
    /**
     * Convert secret key to string for transmission
     */
    public static String keyToString(SecretKey key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }
    
    /**
     * Convert string back to secret key
     */
    public static SecretKey stringToKey(String keyString) {
        byte[] decodedKey = Base64.getDecoder().decode(keyString);
        return new SecretKeySpec(decodedKey, ALGORITHM);
    }
    
    /**
     * Encrypt plaintext using AES
     */
    public static String encrypt(String plaintext, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encryptedBytes = cipher.doFinal(plaintext.getBytes());
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }
    
    /**
     * Decrypt ciphertext using AES
     */
    public static String decrypt(String ciphertext, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(ciphertext));
        return new String(decryptedBytes);
    }
    
    /**
     * Encrypt file data
     */
    public static byte[] encryptBytes(byte[] data, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(data);
    }
    
    /**
     * Decrypt file data
     */
    public static byte[] decryptBytes(byte[] encryptedData, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, key);
        return cipher.doFinal(encryptedData);
    }
}
