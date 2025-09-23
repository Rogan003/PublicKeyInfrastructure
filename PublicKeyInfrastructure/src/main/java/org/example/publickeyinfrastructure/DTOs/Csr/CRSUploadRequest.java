package org.example.publickeyinfrastructure.DTOs.Csr;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CRSUploadRequest {
    private String csrPem;       // The PEM content of the CSR
    private String issuerCAId;   // ID of the issuer CA
    private String notBefore;    // Validity start date
    private String notAfter;     // Validity end date
}
