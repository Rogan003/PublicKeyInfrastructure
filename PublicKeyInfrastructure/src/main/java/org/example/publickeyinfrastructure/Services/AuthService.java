package org.example.publickeyinfrastructure.Services;



import org.example.publickeyinfrastructure.DTOs.AuthResponseDTO;
import org.example.publickeyinfrastructure.DTOs.LoginDTO;
import org.example.publickeyinfrastructure.DTOs.RegistrationDTO;
import org.example.publickeyinfrastructure.DTOs.TokenRefreshDTO;
import org.example.publickeyinfrastructure.Entities.RegularUser;
import org.example.publickeyinfrastructure.Entities.User;
import org.example.publickeyinfrastructure.Repositories.UserRepository;
import org.example.publickeyinfrastructure.Utils.JwtUtil;
import org.springframework.stereotype.Service;



@Service
public class AuthService  {
    
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    
    public AuthService(UserRepository userRepository, 
                      JwtUtil jwtUtil, 
                      RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
    }
    
    public AuthResponseDTO registerUser(RegistrationDTO registrationDTO) {
        // Check if user already exists
        if (userRepository.existsByEmail(registrationDTO.getEmail())) {
            return new AuthResponseDTO(null, null, "User with this email already exists", false, null);
        }
        
        // Create new user
        RegularUser newUser = new RegularUser(
            registrationDTO.getEmail(),
            registrationDTO.getPassword(),
            registrationDTO.getName(),
            registrationDTO.getSurname(),
            registrationDTO.getOrganization()
        );
        
        // Persist user first to ensure ID is generated and relationships are valid
        User savedUser = userRepository.save(newUser);
        
        // Generate tokens using the persisted user's data
        String accessToken = jwtUtil.generateAccessToken(
            savedUser.getEmail(), 
            savedUser.getRole().toString(), 
            savedUser.getId()
        );
        
        var refreshToken = refreshTokenService.createRefreshToken(savedUser);

        if (refreshToken == null) {
            return new AuthResponseDTO(null, null, "Refresh token not found", false, null);
        }

        return new AuthResponseDTO(
            accessToken,
            refreshToken.getToken(),
            "User registered successfully",
            true,
            savedUser.getEmail()
        );
    }
    
    public AuthResponseDTO loginUser(LoginDTO loginDTO) {

        // Find user by email
        User user = userRepository.findByEmail(loginDTO.getEmail())
            .orElse(null);
 
        if (user == null) {
            return new AuthResponseDTO(null, null, "Invalid email or password", false, null);
        }
        
        // Check password
        if (!loginDTO.getPassword().equals(user.getPassword())) {
            return new AuthResponseDTO(null, null, "Invalid email or password", false, null);
        }
        
        // Check if user is enabled
        //if (user instanceof RegularUser) {
            //if (!((RegularUser) user).isEnabled()) {
            //    return new AuthResponseDTO(null, null, "Account is disabled", false, null);
            //}
        //}
              
        String accessToken;
        try {
            accessToken = jwtUtil.generateAccessToken(
                user.getEmail(), 
                user.getRole().toString(), 
                user.getId()
            );
        } catch (Exception e) {
            e.printStackTrace();
            return new AuthResponseDTO(null, null, "Token generation failed: " + e.getMessage(), false, null);
        }
        
        var refreshToken = refreshTokenService.createRefreshToken(user);

        
        return new AuthResponseDTO(
            accessToken,
            refreshToken.getToken(),
            "Login successful",
            true,
            user.getEmail()
        );
    }
    
    public AuthResponseDTO refreshToken(TokenRefreshDTO tokenRefreshDTO) {
        String refreshToken = tokenRefreshDTO.getRefreshToken();
        
        // Validate refresh token
        if (!jwtUtil.isRefreshToken(refreshToken)) {
            return new AuthResponseDTO(null, null, "Invalid refresh token", false, null);
        }
        
        // Find refresh token in database
        var refreshTokenEntity = refreshTokenService.findByToken(refreshToken)
            .orElse(null);
        
        if (refreshTokenEntity == null) {
            return new AuthResponseDTO(null, null, "Refresh token not found", false, null);
        }
        
        // Verify expiration
        try {
            refreshTokenService.verifyExpiration(refreshTokenEntity);
        } catch (RuntimeException e) {
            return new AuthResponseDTO(null, null, e.getMessage(), false, null);
        }
        
        // Get user and generate new access token
        User user = refreshTokenEntity.getUser();
        String newAccessToken = jwtUtil.generateAccessToken(
            user.getEmail(), 
            user.getRole().toString(), 
            user.getId()
        );
        
        return new AuthResponseDTO(
            newAccessToken,
            refreshToken, // Return same refresh token
            "Token refreshed successfully",
            true,
            user.getEmail()
        );
    }
    
    public AuthResponseDTO logout(String refreshToken) {
        try {
            refreshTokenService.deleteRefreshToken(refreshToken);
            return new AuthResponseDTO(
                null, null, "Logout successful", true, null
            );
        } catch (Exception e) {
            return new AuthResponseDTO(
                null, null, "Logout failed: " + e.getMessage(), false, null
            );
        }
    }

    
}