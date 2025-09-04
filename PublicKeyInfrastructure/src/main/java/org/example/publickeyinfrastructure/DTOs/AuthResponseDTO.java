package org.example.publickeyinfrastructure.DTOs;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponseDTO {
    private String accessToken;
    private String refreshToken;
    private String message;
    private boolean success;
    private String userEmail;
    private PwnedDTO pwnedPassword;
}
