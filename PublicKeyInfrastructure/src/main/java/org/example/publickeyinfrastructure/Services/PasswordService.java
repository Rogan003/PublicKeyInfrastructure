package org.example.publickeyinfrastructure.Services;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class PasswordService {
  
  private static final String HIBP_API_URL = "https://api.pwnedpasswords.com/range/";
  private final PasswordEncoder passwordEncoder;
    private final WebClient webClient;
    
    public PasswordService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
        this.webClient = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
                .build();
    }
    
    public boolean isPasswordPwned(String password) {
        try {
            String sha1Hash = getSha1Hash(password);
            String hashPrefix = sha1Hash.substring(0, 5);
            String hashSuffix = sha1Hash.substring(5).toUpperCase();
            
            String response = webClient.get()
                    .uri(HIBP_API_URL + hashPrefix)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            if (response == null || response.isEmpty()) {
                return false;
            }
            
            return response.lines()
                    .anyMatch(line -> line.startsWith(hashSuffix + ":"));
                    
        } catch (Exception e) {
            return false;
        }
    }
    
    public int getPasswordBreachCount(String password) {
        try {
            String sha1Hash = getSha1Hash(password);
            String hashPrefix = sha1Hash.substring(0, 5);
            String hashSuffix = sha1Hash.substring(5).toUpperCase();
            
            String response = webClient.get()
                    .uri(HIBP_API_URL + hashPrefix)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            if (response == null || response.isEmpty()) {
                return 0;
            }
            
            return response.lines()
                    .filter(line -> line.startsWith(hashSuffix + ":"))
                    .findFirst()
                    .map(line -> Integer.parseInt(line.split(":")[1]))
                    .orElse(0);
                    
        } catch (Exception e) {
            return 0;
        }
    }
    
    private String getSha1Hash(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] hashBytes = md.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString().toUpperCase();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-1 algorithm not available", e);
        }
    }

    public String hashPassword(String password) {
        return passwordEncoder.encode(password);
    }

    public boolean verifyPassword(String password, String hashedPassword) {
        return passwordEncoder.matches(password, hashedPassword);
    }

   
}
