package org.example.publickeyinfrastructure.DTOs.Csr;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CAInfoDTO {
    private Long id;
    private String serialNumber;
    private String commonName;
    private String subjectDN;
    private String issuerDN;
    private String validFrom;
    private String validTo;
    private boolean isRevoked;
}