package org.example.publickeyinfrastructure.DTOs.Csr;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CSRAutogenRequest {
    private String issuerCAId;
    private SubjectAutogenDTO subject;
    private String notBefore;
    private String notAfter;
    private String keystoreFormat;
    private String keystorePassword;
}
