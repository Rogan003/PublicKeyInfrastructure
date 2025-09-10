package org.example.publickeyinfrastructure.Entities.EmailVerification;

import jakarta.persistence.*;

import org.example.publickeyinfrastructure.Entities.User.RegularUser;
import org.hibernate.annotations.CreationTimestamp;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.time.Duration;


@Entity
@Getter
@Setter
@Table(name = "email_verification_tokens")
public class EmailVerificationToken {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String token;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private RegularUser user;
    
    @Column(nullable = false)
    private LocalDateTime expiresAt;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column
    private LocalDateTime usedAt;
    
    @Column(nullable = false)
    private boolean used = false;
    
    @Column(nullable = false)
    private boolean expired = false;
    
    public EmailVerificationToken() {}
    
    public EmailVerificationToken(String token, RegularUser user, int expiryTimeInMinutes) {
        this.token = token;
        this.user = user;
        this.expiresAt = LocalDateTime.now().plusMinutes(expiryTimeInMinutes);
        this.used = false;
        this.expired = false;
    }
    
    public void markAsUsed() {
        this.used = true;
        this.usedAt = LocalDateTime.now();
    }
     
    public long getMinutesUntilExpiry() {
        if (LocalDateTime.now().isAfter(expiresAt)) {
            return 0;
        }
        return Duration.between(LocalDateTime.now(), expiresAt).toMinutes();
    }
} 
