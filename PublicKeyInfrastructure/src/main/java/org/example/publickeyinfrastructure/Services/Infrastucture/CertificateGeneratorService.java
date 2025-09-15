package org.example.publickeyinfrastructure.Services.Infrastucture;

import java.math.BigInteger;
import java.security.Security;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;

import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
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

  public CertificateGeneratorService() {
    Security.addProvider(new BouncyCastleProvider());
  }

    public X509Certificate generateCertificate(
            Subject subject,
            Issuer issuer,
            Date startDate,
            Date endDate,
            String serialNumber,
            List<String> extensions
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

            for (String ext : extensions) {
                switch (ext) {
                    case "CA":
                        certGen.addExtension(
                                org.bouncycastle.asn1.x509.Extension.basicConstraints,
                                true,
                                new BasicConstraints(true) // this is a CA
                        );
                        certGen.addExtension(
                                org.bouncycastle.asn1.x509.Extension.keyUsage,
                                true,
                                new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign)
                        );
                        break;

                    case "SERVER":
                        certGen.addExtension(
                                org.bouncycastle.asn1.x509.Extension.keyUsage,
                                true,
                                new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment)
                        );
                        certGen.addExtension(
                                org.bouncycastle.asn1.x509.Extension.extendedKeyUsage,
                                false,
                                new ExtendedKeyUsage(KeyPurposeId.id_kp_serverAuth)
                        );
                        break;

                    case "CLIENT":
                        certGen.addExtension(
                                org.bouncycastle.asn1.x509.Extension.extendedKeyUsage,
                                false,
                                new ExtendedKeyUsage(KeyPurposeId.id_kp_clientAuth)
                        );
                        break;
                }
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
  
}
