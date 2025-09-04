package org.example.publickeyinfrastructure.Services;

import org.example.publickeyinfrastructure.Entities.RefreshToken;
import org.example.publickeyinfrastructure.Entities.User;
import org.example.publickeyinfrastructure.Repositories.RefreshTokenRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {
    
    private final RefreshTokenRepository refreshTokenRepository;
    
    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }
    
    @org.springframework.transaction.annotation.Transactional
    public RefreshToken createRefreshToken(User user) {
        // Obriši postojeći refresh token za korisnika
        refreshTokenRepository.deleteByUser(user);
        
        // Kreiraj novi refresh token
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiryDate(Instant.now().plusSeconds(7 * 24 * 60 * 60)); // 7 dana
        refreshToken.setRevoked(false);
        
        return refreshTokenRepository.save(refreshToken);
    }
    
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }
    
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new RuntimeException("Refresh token was expired. Please make a new signin request");
        }
        
        if (token.isRevoked()) {
            throw new RuntimeException("Refresh token was revoked");
        }
        
        return token;
    }
    
    public void revokeRefreshToken(User user) {
        refreshTokenRepository.revokeByUser(user);
    }
    
    public void deleteRefreshToken(String token) {
        refreshTokenRepository.findByToken(token)
            .ifPresent(refreshTokenRepository::delete);
    }
}