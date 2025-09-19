package org.example.publickeyinfrastructure.Utils;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

public class AesGcmEncryptor {
    private static final int GCM_TAG_LENGTH = 128; // 128-bit authentication tag
    private static final int IV_LENGTH = 12; // 96-bit nonce/IV as required by AES-GCM best practices
    private static final int AES_256_KEY_LENGTH_BYTES = 32; // 256-bit key

    public static String encrypt(String plainText, byte[] masterKey) throws Exception {
        validateKey(masterKey);
        SecretKey key = new SecretKeySpec(masterKey, "AES");

        byte[] iv = new byte[IV_LENGTH];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);

        byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        byte[] ivAndCiphertext = new byte[iv.length + cipherText.length];
        System.arraycopy(iv, 0, ivAndCiphertext, 0, iv.length);
        System.arraycopy(cipherText, 0, ivAndCiphertext, iv.length, cipherText.length);

        return Base64.getEncoder().encodeToString(ivAndCiphertext);
    }

    public static String decrypt(String base64IvAndCiphertext, byte[] masterKey) throws Exception {
        validateKey(masterKey);
        byte[] ivAndCiphertext = Base64.getDecoder().decode(base64IvAndCiphertext);

        if (ivAndCiphertext.length < IV_LENGTH + 1) {
            throw new IllegalArgumentException("Ciphertext too short");
        }

        byte[] iv = new byte[IV_LENGTH];
        byte[] cipherText = new byte[ivAndCiphertext.length - IV_LENGTH];

        System.arraycopy(ivAndCiphertext, 0, iv, 0, IV_LENGTH);
        System.arraycopy(ivAndCiphertext, IV_LENGTH, cipherText, 0, cipherText.length);

        SecretKey key = new SecretKeySpec(masterKey, "AES");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, spec);

        byte[] plainText = cipher.doFinal(cipherText);
        return new String(plainText, StandardCharsets.UTF_8);
    }

    private static void validateKey(byte[] masterKey) {
        if (masterKey == null || masterKey.length != AES_256_KEY_LENGTH_BYTES) {
            throw new IllegalArgumentException("Master key must be 32 bytes (256 bits) for AES-256-GCM");
        }
    }
}
