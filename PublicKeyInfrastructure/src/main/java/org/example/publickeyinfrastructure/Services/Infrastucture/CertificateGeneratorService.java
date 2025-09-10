package org.example.publickeyinfrastructure.Services.Infrastucture;

import java.math.BigInteger;
import java.security.Security;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;

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

  public X509Certificate generateCertificate(Subject subject, Issuer issuer, Date startDate, Date endDate, String serialNumber) {
    try {
      JcaContentSignerBuilder builder = new JcaContentSignerBuilder(ALGORITHM).setProvider(PROVIDER);

      ContentSigner signer = builder.build(issuer.getPrivateKey());
      X509v3CertificateBuilder certGen = new JcaX509v3CertificateBuilder(issuer.getX500Name(),
        new BigInteger(serialNumber),
        startDate,
        endDate,
        subject.getX500Name(),
        subject.getPublicKey());

      X509CertificateHolder certHolder = certGen.build(signer);

      JcaX509CertificateConverter certConverter = new JcaX509CertificateConverter();
            certConverter = certConverter.setProvider(PROVIDER);

      return certConverter.getCertificate(certHolder);
    
    } catch (CertificateEncodingException e) {
      e.printStackTrace();
    } catch (IllegalArgumentException e) {
        e.printStackTrace();
    } catch (IllegalStateException e) {
        e.printStackTrace();
    } catch (OperatorCreationException e) {
        e.printStackTrace();
    } catch (CertificateException e) {
        e.printStackTrace();
    }
    return null;
}
  
}
