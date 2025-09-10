package org.example.publickeyinfrastructure.DTOs.Infrastructure;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubjectDTO {
    private Long id;
    private String commonName;
    private String surname;
    private String givenName;
    private String organization;
    private String organizationalUnit;
    private String country;
    private String email;
    private String uid;
    private String x500Name;
}
