package org.example.publickeyinfrastructure.Services.Auth;



import org.example.publickeyinfrastructure.DTOs.Auth.AuthResponseDTO;
import org.example.publickeyinfrastructure.DTOs.Auth.LoginDTO;
import org.example.publickeyinfrastructure.DTOs.Auth.PwnedDTO;
import org.example.publickeyinfrastructure.DTOs.Auth.RegistrationDTO;
import org.example.publickeyinfrastructure.DTOs.Auth.TokenRefreshDTO;
import org.example.publickeyinfrastructure.Entities.User.Role;
import org.example.publickeyinfrastructure.Entities.User.User;
import org.example.publickeyinfrastructure.Entities.User.RegularUser;
import org.example.publickeyinfrastructure.Repositories.UserRepository;
import org.example.publickeyinfrastructure.Utils.JwtUtil;
import org.springframework.stereotype.Service;



@Service
public class AuthService  {
    
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordService passwordService;
    private final EmailVerificationService emailVerificationService;
    
    public AuthService(UserRepository userRepository, 
                      JwtUtil jwtUtil, 
                      PasswordService passwordService,
                      EmailVerificationService emailVerificationService) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.passwordService = passwordService;
        this.emailVerificationService = emailVerificationService;
    }
    
    public AuthResponseDTO registerUser(RegistrationDTO registrationDTO) {
        // Check if user already exists
        if (userRepository.existsByEmail(registrationDTO.getEmail())) {
            return new AuthResponseDTO(null, null, "User with this email already exists", false, null, new PwnedDTO(false, 0));
        }

        // Check if password is pwned
        if (passwordService.isPasswordPwned(registrationDTO.getPassword())) {
            int breachCount = passwordService.getPasswordBreachCount(registrationDTO.getPassword());
            return new AuthResponseDTO(null, null, "Password is pwned, breach count: " + breachCount, false, null, new PwnedDTO(true, breachCount));
        }
        
        // Create new user
        RegularUser newUser = new RegularUser(
            registrationDTO.getEmail(),
            passwordService.hashPassword(registrationDTO.getPassword()),
            registrationDTO.getName(),
            registrationDTO.getSurname(),
            registrationDTO.getOrganization() != null ? registrationDTO.getOrganization().toEntity(registrationDTO.getOrganization()) : null            
        );
        
        // Persist user first to ensure ID is generated and relationships are valid
        User savedUser = userRepository.save(newUser);
        
        // Send verification email
        try {
            emailVerificationService.sendVerificationEmail((RegularUser) savedUser);
        } catch (Exception e) {
            System.err.println("Error sending verification email: " + e.getMessage());
            e.printStackTrace();
        }
        
        return new AuthResponseDTO(
            null,
            null,
            "User registered successfully. Please check your email to verify your account.",
            true,
            savedUser.getEmail(),
            new PwnedDTO(false, 0)
        );
    }

    public AuthResponseDTO registerCaUser(RegistrationDTO registrationDTO) {
        // Check if user already exists
        if (userRepository.existsByEmail(registrationDTO.getEmail())) {
            return new AuthResponseDTO(null, null, "User with this email already exists", false, null, new PwnedDTO(false, 0));
        }

        // Check if password is pwned
        if (passwordService.isPasswordPwned(registrationDTO.getPassword())) {
            int breachCount = passwordService.getPasswordBreachCount(registrationDTO.getPassword());
            return new AuthResponseDTO(null, null, "Password is pwned, breach count: " + breachCount, false, null, new PwnedDTO(true, breachCount));
        }
        
        // Create new user
        RegularUser newUser = RegularUser.caUser(
            registrationDTO.getEmail(),
            passwordService.hashPassword(registrationDTO.getPassword()),
            registrationDTO.getName(),
            registrationDTO.getSurname(),
            registrationDTO.getOrganization().toEntity(registrationDTO.getOrganization())            
        );
        
        // Persist user first to ensure ID is generated and relationships are valid
        newUser.setRole(Role.CERTIFICATE_AUTHORITY);
        System.out.println("New user: " + newUser.getRole());
        User savedUser = userRepository.save(newUser);
         
        return new AuthResponseDTO(
            null,
            null,
            "CA user registered successfully.",
            true,
            savedUser.getEmail(),
            new PwnedDTO(false, 0)
        );
    }
    
    public AuthResponseDTO loginUser(LoginDTO loginDTO) {

        // Find user by email
        User user = userRepository.findByEmail(loginDTO.getEmail())
            .orElse(null);
 
        if (user == null) {
            return new AuthResponseDTO(null, null, "Invalid email or password", false, null, new PwnedDTO(false, 0));
        }
        
 
        if (!passwordService.verifyPassword(loginDTO.getPassword(), user.getPassword())) {
            return new AuthResponseDTO(null, null, "Invalid email or password", false, null, new PwnedDTO(false, 0));
        }
        
        //Check if user is enabled
        if (user instanceof RegularUser) {
            if (!((RegularUser) user).isEnabled()) {
                return new AuthResponseDTO(null, null, "Email is not verified", false, null, new PwnedDTO(false, 0));
            }
        }
              
        String accessToken;
        String refreshToken;
        try {
            accessToken = jwtUtil.generateAccessToken(
                user.getEmail(), 
                user.getRole().toString(), 
                user.getId()
            );
            refreshToken = jwtUtil.generateRefreshToken(
                user.getEmail(), 
                user.getId()
            );
        } catch (Exception e) {
            e.printStackTrace();
            return new AuthResponseDTO(null, null, "Token generation failed: " + e.getMessage(), false, null, new PwnedDTO(false, 0));
        }
        
        return new AuthResponseDTO(
            accessToken,
            refreshToken,
            "Login successful",
            true,
            user.getEmail(),
            new PwnedDTO(false, 0)
        );
    }
    
    public AuthResponseDTO refreshToken(TokenRefreshDTO tokenRefreshDTO) {
        String refreshToken = tokenRefreshDTO.getRefreshToken();
        
        // Validate refresh token
        if (!jwtUtil.isRefreshToken(refreshToken)) {
            return new AuthResponseDTO(null, null, "Invalid refresh token", false, null, new PwnedDTO(false, 0));
        }
        
        // Validate token signature and expiration
        if (!jwtUtil.validateToken(refreshToken)) {
            return new AuthResponseDTO(null, null, "Refresh token is expired or invalid", false, null, new PwnedDTO(false, 0));
        }
        
        try {
            // Extract user information from refresh token
            String email = jwtUtil.extractEmail(refreshToken);
            Long userId = jwtUtil.extractUserId(refreshToken);
            
            // Verify user still exists
            User user = userRepository.findByEmail(email)
                .orElse(null);
            
            if (user == null || !user.getId().equals(userId)) {
                return new AuthResponseDTO(null, null, "User not found", false, null, new PwnedDTO(false, 0));
            }
            
            // Generate new access token
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
                user.getEmail(),
                new PwnedDTO(false, 0)
            );
        } catch (Exception e) {
            return new AuthResponseDTO(null, null, "Token refresh failed: " + e.getMessage(), false, null, new PwnedDTO(false, 0));
        }
    }
    


    
}