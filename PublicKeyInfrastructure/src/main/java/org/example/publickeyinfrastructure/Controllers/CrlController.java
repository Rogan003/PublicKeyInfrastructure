package org.example.publickeyinfrastructure.Controllers;

import org.example.publickeyinfrastructure.Entities.Infrastructure.Certificate;
import org.example.publickeyinfrastructure.Services.Infrastucture.CertificateService;
import org.example.publickeyinfrastructure.Services.Infrastucture.CrlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.cert.X509CRL;
import java.util.Optional;

@RestController
@RequestMapping("/api/crl")
@CrossOrigin(origins = "*")
public class CrlController {
    @Autowired
    private CrlService crlService;
    @Autowired
    private CertificateService certificateService;

    @GetMapping("/revoked")
    public ResponseEntity<?> getRevokedCertificates() {
        return ResponseEntity.ok(crlService.getRevokedCertificates());
    }

    // download CRL for a specific CA certificate
    @GetMapping("/ca/{issuerCertificateId}")
    public ResponseEntity<String> getCRLForIssuer(@PathVariable Long issuerCertificateId) {
        try {
            Optional<Certificate> optionalCert = certificateService.findById(issuerCertificateId);

            if (optionalCert.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("CA certificate not found");
            }

            Certificate issuerCert = optionalCert.get();

            // generate CRL
            X509CRL crl = crlService.generateCRL(issuerCert);
            String pemCRL = crlService.getCRLAsPEM(crl);

            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=crl_" + issuerCertificateId + ".crl")
                .contentType(MediaType.TEXT_PLAIN)
                .body(pemCRL);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error generating CRL: " + e.getMessage());
        }
    }
}