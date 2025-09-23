package org.example.publickeyinfrastructure.DTOs.Csr;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IssuerConstraintsDTO {
    private String issuerId;
    private String issuerValidFrom;
    private String issuerValidTo;
    private boolean issuerRevoked;
    private Integer maxTTLDays;
}