package org.example.publickeyinfrastructure.DTOs.Csr;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CsrDTO {
    private String subject;
    private String commonName;
    private String surname;
    private String givenName;
    private String organization;
    private String organizationUnit;
    private String country;
    private String email;
    private String publicKey;
    private List<String> extensions;
}