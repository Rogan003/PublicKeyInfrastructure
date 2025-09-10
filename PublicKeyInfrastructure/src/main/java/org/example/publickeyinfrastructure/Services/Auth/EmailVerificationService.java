package org.example.publickeyinfrastructure.Services.Auth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.example.publickeyinfrastructure.Repositories.EmailVerificationTokenRepository;
import org.example.publickeyinfrastructure.Repositories.UserRepository;
import org.example.publickeyinfrastructure.Entities.EmailVerification.EmailVerificationResult;
import org.example.publickeyinfrastructure.Entities.EmailVerification.EmailVerificationToken;
import org.example.publickeyinfrastructure.Entities.User.RegularUser;

import java.util.List;
import java.util.Optional;
import java.time.Duration;
import java.time.LocalDateTime;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.scheduling.annotation.Scheduled;



@Service
@Transactional
public class EmailVerificationService {
    
    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    
    @Value("${app.email-verification.expiry-minutes:1440}") 
    private int tokenExpiryMinutes;
    
    @Value("${app.email-verification.max-attempts:3}")
    private int maxVerificationAttempts;
    
    public EmailVerificationService(EmailVerificationTokenRepository tokenRepository,
                                   UserRepository userRepository,
                                   EmailService emailService) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }
    
    /**
     * Generate and send verification token
     */
    public EmailVerificationResult sendVerificationEmail(RegularUser user) {
        if (user.isEnabled()) {
            return EmailVerificationResult.failure("Email is already verified");
        }
        
        // Check if maximum number of attempts is reached
        List<EmailVerificationToken> existingTokens = tokenRepository.findByUser(user);
        if (existingTokens.size() >= maxVerificationAttempts) {
            return EmailVerificationResult.failure(
                "Maximum number of verification attempts reached (" + maxVerificationAttempts + ")");
        }
        
        // Deactivate existing tokens
        deactivateExistingTokens(user);
        
        // Generate new token
        String token = generateSecureToken();
        EmailVerificationToken verificationToken = new EmailVerificationToken(token, user, tokenExpiryMinutes);
        
        tokenRepository.save(verificationToken);
        
        // Send email
        try {
            emailService.sendVerificationEmail(user, token);
            return EmailVerificationResult.success("Verification email sent to " + user.getEmail());
        } catch (Exception e) {
            // Obriši token ako email nije poslat
            tokenRepository.delete(verificationToken);
            return EmailVerificationResult.failure("Error sending email: " + e.getMessage());
        }
    }
    
    /**
     * Verify email using token
     */
    public EmailVerificationResult verifyEmail(String token) {
        Optional<EmailVerificationToken> tokenOpt = tokenRepository.findByToken(token);
        
        if (tokenOpt.isEmpty()) {
            return EmailVerificationResult.failure("Invalid or non-existent token");
        }
        
        EmailVerificationToken verificationToken = tokenOpt.get();
        
        // Check if token is already used
        if (verificationToken.isUsed()) {
            return EmailVerificationResult.failure("Token is already used");
        }
         
        RegularUser user = verificationToken.getUser();
        
        // Check if email is already verified
        if (user.isEnabled()) {
            verificationToken.markAsUsed();
            tokenRepository.save(verificationToken);
            return EmailVerificationResult.success("Email is already verified");
        }
        
        // Verify email
        user.setEnabled(true);
        userRepository.save(user);
        
        // Mark token as used
        verificationToken.markAsUsed();
        tokenRepository.save(verificationToken);
        
        // Deactivate all other tokens for this user
        deactivateExistingTokens(user);
        
        // Pošalji welcome email
        try {
            emailService.sendWelcomeEmail(user);
        } catch (Exception e) {
            // Don't interrupt process because of welcome email
            System.err.println("Error sending welcome email: " + e.getMessage());
        }
        
        return EmailVerificationResult.success("Email is successfully verified! You can now login.");
    }
    
    /**
     * Send new verification link
     */
    public EmailVerificationResult resendVerificationEmail(String email) {
        Optional<RegularUser> userOpt = userRepository.findRegularByEmail(email);
        if (userOpt.isEmpty()) {
            return EmailVerificationResult.failure("User with this email address does not exist");
        }
        
        RegularUser user = userOpt.get();
        if (user.isEnabled()) {
            return EmailVerificationResult.failure("Email is already verified");
        }
        
        // Check if token was sent recently
        Optional<EmailVerificationToken> recentTokenOpt = tokenRepository
            .findByUserAndUsedFalseAndExpiredFalse(user);
        
        if (recentTokenOpt.isPresent()) {
            EmailVerificationToken recentToken = recentTokenOpt.get();
            long minutesSinceCreation = Duration.between(recentToken.getCreatedAt(), LocalDateTime.now()).toMinutes();
            
            if (minutesSinceCreation < 5) {
                return EmailVerificationResult.failure(
                    "Verification email was sent recently. Try again in " + (5 - minutesSinceCreation) + " minutes.");
            }
        }
        
        return sendVerificationEmail(user);
    }
    
    /**
     * Cleanup expired tokens (called by scheduler)
     */
    @Scheduled(fixedRate = 3600000) // every hour
    public void cleanupExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        
        // Mark expired tokens
        int markedAsExpired = tokenRepository.markExpiredTokens(now);
        
        // Delete old tokens (older than 7 days)
        LocalDateTime cutoff = now.minusDays(7);
        int deletedOldTokens = tokenRepository.deleteOldTokens(cutoff);
        
        if (markedAsExpired > 0 || deletedOldTokens > 0) {
            System.out.println("Cleanup: " + markedAsExpired + " tokens marked as expired, " + 
                             deletedOldTokens + " old tokens deleted");
        }
    }
    
    /**
     * Deactivate existing tokens for user
     */
    private void deactivateExistingTokens(RegularUser user) {
        List<EmailVerificationToken> existingTokens = tokenRepository.findByUser(user);
        for (EmailVerificationToken token : existingTokens) {
            if (!token.isUsed() && !token.isExpired()) {
                token.setExpired(true);
                tokenRepository.save(token);
            }
        }
    }
    
    /**
     * Generate secure token
     */
    private String generateSecureToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}