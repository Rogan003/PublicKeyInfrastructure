package org.example.publickeyinfrastructure.DTOs.Infrastructure;

import jakarta.validation.constraints.NotNull;
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
public class CreateCertificateDTO {
    @NotNull(message = "Issuer Certificate is required!")
    private CertificateDTO issuerCertificate;
    @NotNull(message = "Subject is required!")
    private SubjectDTO subject;
    @NotNull(message = "Start Date is required!")
    private Date startDate;
    @NotNull(message = "End Date is required!")
    private Date endDate;
    /**
     * Supported values:
     * BASIC_CA, BASIC_END, KEY_USAGE, EXT_KEY_SERVER, EXT_KEY_CLIENT, SAN_EXAMPLE
     */
    @NotNull(message = "Extensions are required!")
    private List<String> extensions;
}
