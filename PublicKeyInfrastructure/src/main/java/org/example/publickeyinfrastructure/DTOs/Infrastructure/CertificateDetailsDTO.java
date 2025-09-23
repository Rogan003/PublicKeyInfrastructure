package org.example.publickeyinfrastructure.DTOs.Infrastructure;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CertificateDetailsDTO {
    private Long id;
    private String subject;
    private String issuer;
    private Date validFrom;
    private Date validTo;
    private Boolean isCa;
    private List<String> keyUsages;
    private List<String> extendedKeyUsages;
}