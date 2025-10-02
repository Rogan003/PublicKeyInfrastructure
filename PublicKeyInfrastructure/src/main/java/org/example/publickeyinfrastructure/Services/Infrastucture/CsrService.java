package org.example.publickeyinfrastructure.Services.Infrastucture;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.pkcs.Attribute;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.asn1.x500.X500Name;
import org.example.publickeyinfrastructure.DTOs.Csr.CsrDTO;
import org.example.publickeyinfrastructure.Utils.PemReaderUtil;
import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;


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

            List<String> extensions = extractSimplifiedExtensions(csr);
            extensions.add(0, "BASIC_END");
            csrDto.setExtensions(extensions);

            return csrDto;

        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to process CSR: " + e.getMessage());
        }
    }

    /**
     * Extracts extensions from CSR and maps them to simplified format:
     * - KEY_USAGE (if digitalSignature + keyEncipherment are present)
     * - EXT_KEY_SERVER (if serverAuth is present)
     * - EXT_KEY_CLIENT (if clientAuth is present)
     * - SAN (if Subject Alternative Names are present)
     */
    private List<String> extractSimplifiedExtensions(PKCS10CertificationRequest csr) {
        List<String> simplifiedExtensions = new ArrayList<>();

        try {
            Attribute[] attributes = csr.getAttributes(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest);

            if (attributes == null || attributes.length == 0) {
                return simplifiedExtensions;
            }

            Extensions extensions = Extensions.getInstance(attributes[0].getAttrValues().getObjectAt(0));

            // Check for Key Usage (digitalSignature + keyEncipherment)
            Extension keyUsageExt = extensions.getExtension(Extension.keyUsage);
            if (keyUsageExt != null) {
                KeyUsage keyUsage = KeyUsage.getInstance(keyUsageExt.getParsedValue());
                if (keyUsage.hasUsages(KeyUsage.digitalSignature) &&
                        keyUsage.hasUsages(KeyUsage.keyEncipherment)) {
                    simplifiedExtensions.add("KEY_USAGE");
                }
            }

            // Check for Extended Key Usage
            Extension extKeyUsageExt = extensions.getExtension(Extension.extendedKeyUsage);
            if (extKeyUsageExt != null) {
                ExtendedKeyUsage extKeyUsage = ExtendedKeyUsage.getInstance(extKeyUsageExt.getParsedValue());

                for (KeyPurposeId purpose : extKeyUsage.getUsages()) {
                    if (purpose.equals(KeyPurposeId.id_kp_serverAuth)) {
                        simplifiedExtensions.add("EXT_KEY_SERVER");
                    } else if (purpose.equals(KeyPurposeId.id_kp_clientAuth)) {
                        simplifiedExtensions.add("EXT_KEY_CLIENT");
                    }
                }
            }

            // Check for Subject Alternative Names
            Extension sanExt = extensions.getExtension(Extension.subjectAlternativeName);
            if (sanExt != null) {
                simplifiedExtensions.add("SAN");
            }

        } catch (Exception e) {
            System.err.println("Warning: Failed to parse CSR extensions: " + e.getMessage());
        }

        return simplifiedExtensions;
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