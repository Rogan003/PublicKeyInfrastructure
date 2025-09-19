package org.example.publickeyinfrastructure.Controllers;

import org.example.publickeyinfrastructure.DTOs.Auth.AuthResponseDTO;
import org.example.publickeyinfrastructure.DTOs.Auth.LoginDTO;
import org.example.publickeyinfrastructure.DTOs.Auth.PwnedDTO;
import org.example.publickeyinfrastructure.DTOs.Auth.RegistrationDTO;
import org.example.publickeyinfrastructure.DTOs.Auth.ResendVerificationDTO;
import org.example.publickeyinfrastructure.DTOs.Auth.TokenRefreshDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.example.publickeyinfrastructure.Services.Auth.AuthService;
import org.example.publickeyinfrastructure.Services.Auth.EmailVerificationService;

import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

import org.example.publickeyinfrastructure.Entities.EmailVerification.EmailVerificationResult;
import org.example.publickeyinfrastructure.Entities.User.User;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    
    @Autowired
    private AuthService authService;
    @Autowired
    private EmailVerificationService emailVerificationService;
    
    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> registerUser(@RequestBody RegistrationDTO registrationDTO) {
        try {
            AuthResponseDTO response = authService.registerUser(registrationDTO);
            if (response.isSuccess() || response.getPwnedPassword().isPwned()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(new AuthResponseDTO(null, null, "Registration failed: " + e.getMessage(), false, null, new PwnedDTO(false, 0)));
        }
    }

    @PostMapping("/register-ca")
    public ResponseEntity<AuthResponseDTO> registerCAUser(@RequestBody RegistrationDTO registrationDTO) {
        try {
            AuthResponseDTO response = authService.registerCaUser(registrationDTO);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new AuthResponseDTO(null, null, "Registration failed: " + e.getMessage(), false, null, new PwnedDTO(false, 0)));
        }
    }
    
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> loginUser(@RequestBody LoginDTO loginDTO) {
        try {
            AuthResponseDTO response = authService.loginUser(loginDTO);
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(new AuthResponseDTO(null, null, "Login failed: " + e.getMessage(), false, null, new PwnedDTO(false, 0)));
        }
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDTO> refreshToken(@RequestBody TokenRefreshDTO tokenRefreshDTO) {
        try {
            AuthResponseDTO response = authService.refreshToken(tokenRefreshDTO);
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(new AuthResponseDTO(null, null, "Token refresh failed: " + e.getMessage(), false, null, new PwnedDTO(false, 0)));
        }
    }
    
    @GetMapping("/verify-email")
    public ResponseEntity<Map<String, Object>> verifyEmail(@RequestParam String token) {
        EmailVerificationResult result = emailVerificationService.verifyEmail(token);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", result.isSuccess());
        response.put("message", result.getMessage());
        
        if (result.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @PostMapping("/resend-verification")
    public ResponseEntity<Map<String, Object>> resendVerification(@RequestBody ResendVerificationDTO request) {
        EmailVerificationResult result = emailVerificationService.resendVerificationEmail(request.getEmail());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", result.isSuccess());
        response.put("message", result.getMessage());
        
        if (result.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
}