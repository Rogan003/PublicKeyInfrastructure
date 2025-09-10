package org.example.publickeyinfrastructure.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.example.publickeyinfrastructure.Entities.EmailVerification.EmailVerificationToken;
import org.example.publickeyinfrastructure.Entities.User.RegularUser;


@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    @Query("SELECT t FROM EmailVerificationToken t WHERE t.token = :token")
    Optional<EmailVerificationToken> findByToken(String token);
    
    @Query("SELECT t FROM EmailVerificationToken t WHERE t.user = :user AND t.used = false AND t.expired = false")
    Optional<EmailVerificationToken> findByUserAndUsedFalseAndExpiredFalse(org.example.publickeyinfrastructure.Entities.User.RegularUser user);
    
    @Query("SELECT t FROM EmailVerificationToken t WHERE t.user = :user")
    List<EmailVerificationToken> findByUser(RegularUser user);
    
    @Query("SELECT t FROM EmailVerificationToken t WHERE t.expiresAt < :now AND t.used = false AND t.expired = false")
    List<EmailVerificationToken> findExpiredTokens(@Param("now") LocalDateTime now);
    
    @Modifying
    @Query("UPDATE EmailVerificationToken t SET t.expired = true WHERE t.expiresAt < :now AND t.used = false AND t.expired = false")
    int markExpiredTokens(@Param("now") LocalDateTime now);
    
    @Modifying
    @Query("DELETE FROM EmailVerificationToken t WHERE t.createdAt < :cutoffTime")
    int deleteOldTokens(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    @Modifying
    @Query("DELETE FROM EmailVerificationToken t WHERE t.user = :user")
    void deleteByUser(RegularUser user);
}
