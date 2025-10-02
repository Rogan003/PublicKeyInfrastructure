package org.example.publickeyinfrastructure.Services.Infrastucture;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.CRLNumber;
import org.bouncycastle.asn1.x509.CRLReason;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.cert.X509CRLHolder;
import org.bouncycastle.cert.X509v2CRLBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CRLConverter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.example.publickeyinfrastructure.Entities.Infrastructure.Certificate;
import org.example.publickeyinfrastructure.Entities.Infrastructure.Issuer;
import org.example.publickeyinfrastructure.Repositories.CertificateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.security.cert.X509CRL;
import java.util.*;

@Service
public class CrlService {
    @Autowired
    private CertificateRepository certificateRepository;

    private long crlNumber = 1;

    public void addCertificateToCRL(Certificate certificate) {
        // cert is already marked as revoked in database, method is kept for compatibility
        if (certificate.getRevocationReason() == null || certificate.getRevocationDate() == null) {
            throw new IllegalArgumentException("Certificate must have revocation reason and date");
        }
    }

    public List<Certificate> getRevokedCertificates() {
        return certificateRepository.findAll().stream()
                .filter(c -> c.getRevocationReason() != null)
                .toList();
    }

    // Generate a CRL for a specific issuer
    public X509CRL generateCRL(Certificate issuerCertificate) {
        try {
            Issuer issuer = issuerCertificate.getIssuer();
            X500Name issuerName = issuer.getX500Name();

            Date now = new Date();
            Date nextUpdate = new Date(now.getTime() + 24 * 60 * 60 * 1000); // 24 hours

            X509v2CRLBuilder crlBuilder = new X509v2CRLBuilder(issuerName, now);
            crlBuilder.setNextUpdate(nextUpdate);

            // Add CRL number extension
            crlBuilder.addExtension(Extension.cRLNumber, false, new CRLNumber(BigInteger.valueOf(crlNumber++)));

            // Find all revoked certificates issued by this CA
            List<Certificate> revokedCerts = certificateRepository.findAll().stream()
                    .filter(c -> c.getRevocationReason() != null)
                    .filter(c -> isIssuedBy(c, issuerCertificate))
                    .toList();

            // Add revoked certificates to CRL
            for (Certificate cert : revokedCerts) {
                BigInteger serialNumber = new BigInteger(cert.getSerialNumber());
                Date revocationDate = cert.getRevocationDate();

                // Map reason string to CRLReason
                int reasonCode = mapRevocationReason(cert.getRevocationReason());

                crlBuilder.addCRLEntry(serialNumber, revocationDate, reasonCode);
            }

            // Sign the CRL
            ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSAEncryption")
                    .setProvider("BC")
                    .build(issuer.getPrivateKey());

            X509CRLHolder crlHolder = crlBuilder.build(signer);

            return new JcaX509CRLConverter()
                    .setProvider("BC")
                    .getCRL(crlHolder);

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate CRL: " + e.getMessage(), e);
        }
    }

    private boolean isIssuedBy(Certificate cert, Certificate issuerCert) {
        if (cert.getParentCertificate() == null) {
            return false;
        }
        return cert.getParentCertificate().getId().equals(issuerCert.getId());
    }

    /**
     * Map revocation reason string to CRLReason code
     */
    private int mapRevocationReason(String reason) {
        if (reason == null) return CRLReason.unspecified;

        return switch (reason.toLowerCase()) {
            case "keycompromise", "key_compromise" -> CRLReason.keyCompromise;
            case "cacompromise", "ca_compromise" -> CRLReason.cACompromise;
            case "affiliationchanged", "affiliation_changed" -> CRLReason.affiliationChanged;
            case "superseded" -> CRLReason.superseded;
            case "cessationofoperation", "cessation_of_operation" -> CRLReason.cessationOfOperation;
            case "certificatehold", "certificate_hold" -> CRLReason.certificateHold;
            case "removefromcrl", "remove_from_crl" -> CRLReason.removeFromCRL;
            case "privilegewithdrawn", "privilege_withdrawn" -> CRLReason.privilegeWithdrawn;
            case "aacompromis   e", "aa_compromise" -> CRLReason.aACompromise;
            default -> CRLReason.unspecified;
        };
    }

    public String getCRLAsPEM(X509CRL crl) {
        try {
            byte[] encoded = crl.getEncoded();
            String base64 = Base64.getEncoder().encodeToString(encoded);

            StringBuilder pem = new StringBuilder();
            pem.append("-----BEGIN X509 CRL-----\n");

            int index = 0;
            while (index < base64.length()) {
                int endIndex = Math.min(index + 64, base64.length());
                pem.append(base64, index, endIndex).append("\n");
                index += 64;
            }

            pem.append("-----END X509 CRL-----\n");

            return pem.toString();

        } catch (Exception e) {
            throw new RuntimeException("Failed to convert CRL to PEM: " + e.getMessage(), e);
        }
    }
}