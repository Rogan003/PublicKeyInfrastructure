package org.example.publickeyinfrastructure.DTOs.Csr;

import lombok.Getter;
import lombok.Setter;
import org.example.publickeyinfrastructure.DTOs.Infrastructure.CertificateDTO;

@Getter
@Setter
public class IssueAutogenResponse {
    private CertificateDTO certificate;
    private String keystoreDownloadUrl; // URL to download the keystore generated
}

