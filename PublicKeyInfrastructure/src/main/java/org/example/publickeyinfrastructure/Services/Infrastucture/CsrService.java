package org.example.publickeyinfrastructure.Services.Infrastucture;

import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.asn1.x500.X500Name;
import org.example.publickeyinfrastructure.DTOs.Csr.CsrDTO;
import org.example.publickeyinfrastructure.Utils.PemReaderUtil;
import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Service
public class CsrService {
    public CsrDTO processCsr(String pemContent) throws IllegalArgumentException {
        try {
            PKCS10CertificationRequest csr = new PKCS10CertificationRequest(
                PemReaderUtil.readPem(pemContent)
            );

            X500Name subject = csr.getSubject();
            PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(
                new X509EncodedKeySpec(csr.getSubjectPublicKeyInfo().getEncoded())
            );
            String publicKeyBase64 = Base64.getEncoder().encodeToString(publicKey.getEncoded());

            CsrDTO csrDto = new CsrDTO();
            csrDto.setSubject(subject.toString());
            csrDto.setPublicKey(publicKeyBase64);

            return csrDto;

        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to process CSR: " + e.getMessage());
        }
    }
}