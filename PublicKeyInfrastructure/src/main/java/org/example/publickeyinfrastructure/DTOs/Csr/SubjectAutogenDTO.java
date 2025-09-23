package org.example.publickeyinfrastructure.DTOs.Csr;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubjectAutogenDTO {
    private String CN; // Common Name
    private String O;  // Organization
    private String OU; // Organization Unit (optional)
    private String C;  // Country (optional)
    private String E;  // Email
}

