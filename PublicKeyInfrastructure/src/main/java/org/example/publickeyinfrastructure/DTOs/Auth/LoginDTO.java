package org.example.publickeyinfrastructure.DTOs.Auth;

import lombok.Data;

@Data
public class LoginDTO {
    private String email;
    private String password;
}
