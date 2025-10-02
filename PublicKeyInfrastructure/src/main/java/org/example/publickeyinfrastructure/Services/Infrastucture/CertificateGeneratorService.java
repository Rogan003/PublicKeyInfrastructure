package org.example.publickeyinfrastructure.Services.Infrastucture;

import java.math.BigInteger;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;

import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.example.publickeyinfrastructure.Entities.Infrastructure.Issuer;
import org.example.publickeyinfrastructure.Entities.Infrastructure.Subject;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Service;

@Service
public class CertificateGeneratorService {

  private static final String ALGORITHM = "SHA256WithRSAEncryption";
  private static final String PROVIDER = "BC";

  /* EXAMPLE:
  1. User visits your frontend (localhost:3000)
  2. Frontend makes API call to backend (localhost:8080)
  3. Backend returns a certificate with CRL URL: "http://localhost:8080/api/crl/ca/1"
  4. When validating the certificate, the client software (browser/OpenSSL/etc.)
     directly calls: http://localhost:8080/api/crl/ca/1
  5. Backend returns the CRL (list of revoked certificates) */
  private static final String CRL_DISTRIBUTION_BASE_URL = "https://localhost:8443/api/crl/ca/";   // backend API endpoint - check there if a certificate is revoked

  public CertificateGeneratorService() {
    Security.addProvider(new BouncyCastleProvider());
  }

    public X509Certificate generateCertificate(
            Subject subject,
            Issuer issuer,
            Date startDate,
            Date endDate,
            String serialNumber,
            List<String> extensions,
            Long issuerCertificateId
    ) {
        try {
            JcaContentSignerBuilder builder = new JcaContentSignerBuilder(ALGORITHM).setProvider(PROVIDER);
            ContentSigner signer = builder.build(issuer.getPrivateKey());

            X509v3CertificateBuilder certGen = new JcaX509v3CertificateBuilder(
                    issuer.getX500Name(),
                    new BigInteger(serialNumber),
                    startDate,
                    endDate,
                    subject.getX500Name(),
                    subject.getPublicKey()
            );

            // Add CRL Distribution Point for all certificates
            addCRLDistributionPoint(certGen, issuerCertificateId);

            for (String ext : extensions) {
                System.out.println("Extension: ");
                System.out.println(ext);

                try {
                    switch (ext.toUpperCase()) {
                        case "BASIC_CA":
                            certGen.addExtension(
                                    Extension.basicConstraints,
                                    true,
                                    new BasicConstraints(true) // certificate authority
                            );
                            break;

                        case "BASIC_END":
                            certGen.addExtension(
                                    Extension.basicConstraints,
                                    true,
                                    new BasicConstraints(false) // end-entity
                            );
                            break;

                        case "KEY_USAGE":
                            certGen.addExtension(
                                    Extension.keyUsage,
                                    true,
                                    new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment)
                            );
                            break;

                        case "EXT_KEY_SERVER":
                            certGen.addExtension(
                                    Extension.extendedKeyUsage,
                                    false,
                                    new ExtendedKeyUsage(KeyPurposeId.id_kp_serverAuth)
                            );
                            break;

                        case "EXT_KEY_CLIENT":
                            certGen.addExtension(
                                    Extension.extendedKeyUsage,
                                    false,
                                    new ExtendedKeyUsage(KeyPurposeId.id_kp_clientAuth)
                            );
                            break;

                        case "SAN":
                            certGen.addExtension(
                                    Extension.subjectAlternativeName,
                                    false,
                                    new GeneralNames(
                                            new GeneralName(subject.getX500Name())
                                    )
                            );
                            break;

                    }
                } catch (Exception e) {}
            }

            X509CertificateHolder certHolder = certGen.build(signer);
            return new JcaX509CertificateConverter()
                    .setProvider(PROVIDER)
                    .getCertificate(certHolder);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /*
    What is "actual issuer certificate ID"?
    When a CA (Certificate Authority) issues a certificate, the issued certificate needs to include a CRL Distribution Point - a URL where clients can download the list of revoked certificates for that specific CA.
    For example:
    Root CA (ID=1) issues Intermediate CA (ID=2)
    Intermediate CA's CRL URL: http://localhost:8080/api/crl/ca/1 (points to Root CA's CRL)
    Intermediate CA (ID=2) issues End Entity certificate (ID=3)
    End Entity's CRL URL: http://localhost:8080/api/crl/ca/2 (points to Intermediate CA's CRL)
    Now when you revoke a certificate, clients can check the CRL at that URL to see if it's been revoked!
    */
    // add CRL Distribution Point extension
    // issuerCertificateId = ID of the CA certificate (used to construct CRL URL)
    private void addCRLDistributionPoint(X509v3CertificateBuilder certGen, Long issuerCertificateId) {
        try {
            String crlUrl = CRL_DISTRIBUTION_BASE_URL + issuerCertificateId;

            GeneralName generalName = new GeneralName(GeneralName.uniformResourceIdentifier, crlUrl);
            GeneralNames generalNames = new GeneralNames(generalName);
            DistributionPointName distributionPointName = new DistributionPointName(generalNames);
            DistributionPoint distributionPoint = new DistributionPoint(
                    distributionPointName,
                    null,
                    null
            );

            certGen.addExtension(
                    Extension.cRLDistributionPoints,
                    false,
                    new CRLDistPoint(new DistributionPoint[]{distributionPoint})
            );

        } catch (Exception e) {
            System.err.println("Failed to add CRL Distribution Point: " + e.getMessage());
        }
    }
}
