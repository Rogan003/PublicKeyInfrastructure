package org.example.publickeyinfrastructure.Controllers;

import org.example.publickeyinfrastructure.DTOs.AuthResponseDTO;
import org.example.publickeyinfrastructure.DTOs.LoginDTO;
import org.example.publickeyinfrastructure.DTOs.RegistrationDTO;
import org.example.publickeyinfrastructure.DTOs.TokenRefreshDTO;
import org.example.publickeyinfrastructure.Services.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.example.publickeyinfrastructure.DTOs.PwnedDTO;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    
    @Autowired
    private AuthService authService;
    
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
    
    // Logout endpoint removed - stateless refresh tokens don't require server-side logout
    
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Auth service is running");
    }
}