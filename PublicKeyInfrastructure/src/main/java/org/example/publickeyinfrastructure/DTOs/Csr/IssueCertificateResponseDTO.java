package org.example.publickeyinfrastructure.DTOs.Csr;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IssueCertificateResponseDTO {
    private Long id;
    private String serialNumber;
    private String subjectDN;
    private String issuerDN;
    private String validFrom;
    private String validTo;
    private String certificateType;
    private List<String> extensions;
}

