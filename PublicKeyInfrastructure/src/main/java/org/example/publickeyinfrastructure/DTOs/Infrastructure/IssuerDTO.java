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
    private Long id;
    private String commonName;
    private String organizationName;
    private String organizationUnit;
    private String country;
    private String email;
    private String x500Name;
}
