package org.example.publickeyinfrastructure.DTOs.Infrastructure;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.publickeyinfrastructure.Entities.Enums.CertificateType;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CertificateDTO {
    private Long id;
    private String serialNumber;
    private Date startDate;
    private Date endDate;
    private CertificateType certificateType;
    private String subjectDN;
    private String issuerDN;
    private String certificateBase64;
    private String certificatePEM;
}
