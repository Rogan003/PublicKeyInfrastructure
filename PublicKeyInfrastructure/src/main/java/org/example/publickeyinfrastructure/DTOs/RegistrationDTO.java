package org.example.publickeyinfrastructure.DTOs;

import lombok.Data;

@Data
public class RegistrationDTO {
    private String email;
    private String password;
    private String name;
    private String surname;
    private String organization;
}
