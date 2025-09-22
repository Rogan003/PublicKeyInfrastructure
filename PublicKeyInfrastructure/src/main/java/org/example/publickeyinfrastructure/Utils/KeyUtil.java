package org.example.publickeyinfrastructure.Utils;

import javax.crypto.KeyGenerator;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class KeyUtil {
    public static byte[] generateAes256Key() throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256, SecureRandom.getInstanceStrong());
        return keyGen.generateKey().getEncoded();
    }

    public static byte[] encryptPrivateKey(byte[] key, byte[] encryptionKey) {
        try {
            String base64Pkcs8 = Base64.getEncoder().encodeToString(key);
            String cipherB64 = AesGcmEncryptor.encrypt(base64Pkcs8, encryptionKey);
            return cipherB64.getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    public static byte[] decryptPrivateKey(byte[] key, byte[] encryptionKey) {
        try {
            if (key == null) return null;
            String cipherB64 = new String(key, StandardCharsets.UTF_8);
            String base64Pkcs8 = AesGcmEncryptor.decrypt(cipherB64, encryptionKey);
            return Base64.getDecoder().decode(base64Pkcs8);
        } catch (Exception e) {
            return null;
        }
    }

    public static byte[] getMasterKey() {
        String hex = System.getenv("MASTER");
        if (hex == null || hex.isEmpty()) {
            hex = System.getProperty("MASTER");
        }
        if (hex == null || hex.isEmpty()) {
            throw new IllegalStateException("MASTER key not configured in environment or system properties");
        }
        return hexStringToByteArray(hex);
    }

    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        if (len % 2 != 0) {
            throw new IllegalArgumentException("Invalid hex string length");
        }
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            int hi = Character.digit(s.charAt(i), 16);
            int lo = Character.digit(s.charAt(i + 1), 16);
            if (hi < 0 || lo < 0) {
                throw new IllegalArgumentException("Invalid hex character in MASTER key");
            }
            data[i / 2] = (byte) ((hi << 4) + lo);
        }
        return data;
    }
}