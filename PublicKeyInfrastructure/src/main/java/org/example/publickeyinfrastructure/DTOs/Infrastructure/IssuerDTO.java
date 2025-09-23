package org.example.publickeyinfrastructure.DTOs.Infrastructure;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IssuerDTO {
    private String commonName;
    private String surname;
    private String givenName;
    private String organizationName;
    private String organizationUnit;
    private String country;
    private String email;
}
