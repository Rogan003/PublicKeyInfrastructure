package org.example.publickeyinfrastructure.Services.Infrastucture;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.style.BCStyle;
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

            parseX500Name(subject, csrDto);

            return csrDto;

        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to process CSR: " + e.getMessage());
        }
    }

    private void parseX500Name(X500Name subject, CsrDTO csrDto) {
        csrDto.setCommonName(getRDNValue(subject, BCStyle.CN));
        csrDto.setSurname(getRDNValue(subject, BCStyle.SURNAME));
        csrDto.setGivenName(getRDNValue(subject, BCStyle.GIVENNAME));
        csrDto.setOrganization(getRDNValue(subject, BCStyle.O));
        csrDto.setOrganizationUnit(getRDNValue(subject, BCStyle.OU));
        csrDto.setCountry(getRDNValue(subject, BCStyle.C));
        csrDto.setEmail(getRDNValue(subject, BCStyle.E));
    }

    private String getRDNValue(X500Name name, ASN1ObjectIdentifier identifier) {
        RDN[] rdns = name.getRDNs(identifier);
        if (rdns != null && rdns.length > 0) {
            return rdns[0].getFirst().getValue().toString();
        }
        return null;
    }
}