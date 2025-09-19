package org.example.publickeyinfrastructure.DTOs.Auth;

import org.example.publickeyinfrastructure.DTOs.Infrastructure.OrganisationDTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegistrationDTO {
    private String email;
    private String password;
    private String name;
    private String surname;
    private OrganisationDTO organization;
}
