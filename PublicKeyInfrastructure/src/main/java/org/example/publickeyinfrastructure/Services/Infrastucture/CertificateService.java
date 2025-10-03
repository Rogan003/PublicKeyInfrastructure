package org.example.publickeyinfrastructure.Services.Infrastucture;

import org.example.publickeyinfrastructure.DTOs.Csr.IssueCertificateResponseDTO;
import org.example.publickeyinfrastructure.DTOs.Csr.IssuerConstraintsDTO;
import org.example.publickeyinfrastructure.DTOs.Infrastructure.CreateCertificateDTO;
import org.example.publickeyinfrastructure.DTOs.Infrastructure.IssuerDTO;
import org.example.publickeyinfrastructure.Entities.Infrastructure.Organisation;
import org.example.publickeyinfrastructure.Entities.User.RegularUser;
import org.example.publickeyinfrastructure.Services.Auth.AuthService;
import org.springframework.stereotype.Service;
import org.example.publickeyinfrastructure.Entities.Infrastructure.Issuer;
import org.example.publickeyinfrastructure.Entities.Infrastructure.Subject;
import org.example.publickeyinfrastructure.DTOs.Infrastructure.CertificateDTO;
import org.example.publickeyinfrastructure.Entities.Enums.CertificateType;
import org.example.publickeyinfrastructure.Entities.Infrastructure.Certificate;
import org.example.publickeyinfrastructure.Repositories.CertificateRepository;

import java.io.ByteArrayOutputStream;
import java.security.*;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import javafx.util.Pair;

@Service
public class CertificateService {
  private SubjectService subjectService;
  private CertificateGeneratorService certificateGeneratorService;
  private CertificateRepository certificateRepository;
  private IssuerService issuerService;
  private CrlService crlService;
  private AuthService authService;
  private OrganisationService organisationService;

  public CertificateService(CertificateGeneratorService certificateGeneratorService, CertificateRepository certificateRepository, IssuerService issuerService, SubjectService subjectService, CrlService crlService, AuthService authService, OrganisationService organisationService) {
    this.certificateRepository = certificateRepository;
    this.certificateGeneratorService = certificateGeneratorService;
    this.issuerService = issuerService;
    this.subjectService = subjectService;
    this.crlService = crlService;
    this.authService = authService;
    this.organisationService = organisationService;
  }
  
  public KeyPair generateKeyPair() {
    try {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
        keyGen.initialize(2048, random);
        return keyGen.generateKeyPair();
    } catch (NoSuchAlgorithmException e) {
        e.printStackTrace();
    } catch (NoSuchProviderException e) {
        e.printStackTrace();
    }
    return null;
 }

  public Certificate createRootCA(CreateCertificateDTO newCertificate) {
      // Root CA signs itself
      RegularUser user = authService.getUserById(newCertificate.getOwnerId());

      if (user == null || !Objects.equals(user.getOrganisation().getName(), newCertificate.getSubject().getOrganizationName())
              || !Objects.equals(user.getOrganisation().getCountry(), newCertificate.getSubject().getCountry()) ||
              !Objects.equals(user.getOrganisation().getUnit(), newCertificate.getSubject().getOrganizationUnit())) {
          return null;
      }

      Issuer issuer = issuerService.createIssuer(new IssuerDTO(
              newCertificate.getSubject().getCommonName(),
              newCertificate.getSubject().getSurname(),
              newCertificate.getSubject().getGivenName(),
              newCertificate.getSubject().getOrganizationName(),
              newCertificate.getSubject().getOrganizationUnit(),
              newCertificate.getSubject().getCountry(),
              newCertificate.getSubject().getEmail()
      ), user.getOrganisation());
      Subject subject = subjectService.createSubject(newCertificate.getSubject(), user.getOrganisation(), issuer);

      String sn = String.valueOf(getAllCertificates().size());

      X509Certificate certificate = certificateGeneratorService.generateCertificate(subject,
              issuer, newCertificate.getStartDate(), newCertificate.getEndDate(),
              sn, newCertificate.getExtensions(), null);

      Certificate created = new Certificate(subject, issuer,
              sn, newCertificate.getStartDate(), newCertificate.getEndDate(),
              certificate, CertificateType.ROOT_CA, null, user, user.getOrganisation());
      certificateRepository.save(created);
      return created;
  }

  public Certificate createIntermediateCA(CreateCertificateDTO newCertificate, boolean isAdmin) {
      RegularUser user = authService.getUserById(newCertificate.getOwnerId());

      if (user == null) {
          return null;
      }

      Organisation organisation = user.getOrganisation();

      if (!isAdmin && (!Objects.equals(user.getOrganisation().getName(), newCertificate.getSubject().getOrganizationName())
              || !Objects.equals(user.getOrganisation().getCountry(), newCertificate.getSubject().getCountry()) ||
              !Objects.equals(user.getOrganisation().getUnit(), newCertificate.getSubject().getOrganizationUnit()))) {
          return null;
      } else if (isAdmin) {
          organisation = organisationService.getOrganisationByNameAndUnitAndCountry(
                  newCertificate.getSubject().getOrganizationName(),
                  newCertificate.getSubject().getOrganizationUnit(),
                  newCertificate.getSubject().getCountry()
          );
      }

      if (organisation == null) {
          return null;
      }

      Certificate issuerCertificate = getCertificateById(newCertificate.getIssuerCertificateId());

      if (issuerCertificate == null) {
          return null;
      }

      Issuer thisCertIssuer = issuerService.createIssuer(new IssuerDTO(
              newCertificate.getSubject().getCommonName(),
              newCertificate.getSubject().getSurname(),
              newCertificate.getSubject().getGivenName(),
              newCertificate.getSubject().getOrganizationName(),
              newCertificate.getSubject().getOrganizationUnit(),
              newCertificate.getSubject().getCountry(),
              newCertificate.getSubject().getEmail()
      ), user.getOrganisation());
      Subject subject = subjectService.createSubject(newCertificate.getSubject(), organisation, thisCertIssuer);

      if (isCertificateValid(issuerCertificate)) {
          return null;
      }

      String sn = String.valueOf(getAllCertificates().size());

      // Intermediate CA signs Root CA
      X509Certificate certificate = certificateGeneratorService.generateCertificate(subject,
              issuerCertificate.getSubject().getIssuerVersion(), newCertificate.getStartDate(),
              newCertificate.getEndDate(), sn, newCertificate.getExtensions(), issuerCertificate.getId());

      Certificate created = new Certificate(subject, issuerCertificate.getSubject().getIssuerVersion(),
              sn, newCertificate.getStartDate(), newCertificate.getEndDate(),
              certificate, CertificateType.INTERMEDIATE_CA, issuerCertificate, user, organisation);
      certificateRepository.save(created);
      return created;
  }

  public Certificate createEndEntityCertificate(CreateCertificateDTO newCertificate) {
      Certificate issuerCertificate = getCertificateById(newCertificate.getIssuerCertificateId());
      if (issuerCertificate == null || issuerCertificate.getRevocationReason() != null) {
          return null;
      }

      if (!Objects.equals(issuerCertificate.getOrganisation().getName(), newCertificate.getSubject().getOrganizationName())
              || !Objects.equals(issuerCertificate.getOrganisation().getCountry(), newCertificate.getSubject().getCountry()) ||
              !Objects.equals(issuerCertificate.getOrganisation().getUnit(), newCertificate.getSubject().getOrganizationUnit())) {
          return null;
      }

      RegularUser user = authService.getUserById(newCertificate.getOwnerId());

      if (user == null) {
          return null;
      }

      Subject subject = subjectService.createSubject(newCertificate.getSubject(), issuerCertificate.getOrganisation(), null);

      if (isCertificateValid(issuerCertificate)) {
          return null;
      }

      String sn = String.valueOf(getAllCertificates().size());

      X509Certificate certificate = certificateGeneratorService.generateCertificate(subject,
              issuerCertificate.getSubject().getIssuerVersion(), newCertificate.getStartDate(),
              newCertificate.getEndDate(), sn, newCertificate.getExtensions(), issuerCertificate.getId());

      Certificate created = new Certificate(subject, issuerCertificate.getSubject().getIssuerVersion(),
              sn, newCertificate.getStartDate(),
              newCertificate.getEndDate(), certificate, CertificateType.END_ENTITY, issuerCertificate,
              user, issuerCertificate.getOrganisation());

      created = certificateRepository.save(created);
      return created;
  }

  public Pair<Certificate, KeyPair> createEndEntityCertificateCSR(CreateCertificateDTO newCertificate, String keystoreFormat) {
    Certificate issuerCertificate = getCertificateById(newCertificate.getIssuerCertificateId());
    if (issuerCertificate == null) {
        return null;
    }

    if (!Objects.equals(issuerCertificate.getOrganisation().getName(), newCertificate.getSubject().getOrganizationName())
            || !Objects.equals(issuerCertificate.getOrganisation().getCountry(), newCertificate.getSubject().getCountry()) ||
            !Objects.equals(issuerCertificate.getOrganisation().getUnit(), newCertificate.getSubject().getOrganizationUnit())) {
        return null;
    }

    RegularUser user = authService.getUserById(newCertificate.getOwnerId());

    if (user == null) {
        return null;
    }

    Subject subject = subjectService.createSubject(newCertificate.getSubject(), issuerCertificate.getOrganisation(), null);

    if (isCertificateValid(issuerCertificate)) {
        return null;
    }

    String sn = String.valueOf(getAllCertificates().size());

    X509Certificate certificate = certificateGeneratorService.generateCertificate(subject,
            issuerCertificate.getSubject().getIssuerVersion(), newCertificate.getStartDate(),
            newCertificate.getEndDate(), sn, newCertificate.getExtensions(),  issuerCertificate.getId());

    Certificate created = new Certificate(subject, issuerCertificate.getSubject().getIssuerVersion(),
            sn, newCertificate.getStartDate(),
            newCertificate.getEndDate(), certificate, CertificateType.END_ENTITY, issuerCertificate,
            user, issuerCertificate.getOrganisation());
    created.setKeystoreFormat(keystoreFormat);

    KeyPair keyPair = new KeyPair(subject.getPublicKey(), subject.getPrivateKey());

    created = certificateRepository.save(created);
    return (Pair<Certificate, KeyPair>) new Pair(created, keyPair);
  }

    public String toPem(Certificate certificate) {
        try {
            X509Certificate x509Certificate = certificate.getX509Certificate();

            byte[] encoded = x509Certificate.getEncoded();
            String base64Encoded = Base64.getEncoder().encodeToString(encoded);

            StringBuilder pem = new StringBuilder();
            pem.append("-----BEGIN CERTIFICATE-----\n");

            int index = 0;
            while (index < base64Encoded.length()) {
                int endIndex = Math.min(index + 64, base64Encoded.length());
                pem.append(base64Encoded, index, endIndex).append("\n");
                index += 64;
            }

            pem.append("-----END CERTIFICATE-----\n");

            return pem.toString();

        } catch (Exception e) {
            throw new RuntimeException("Failed to convert certificate to PEM format: " + e.getMessage(), e);
        }
    }

  public Certificate getCertificateById(Long id) {
    return certificateRepository.findById(id).orElse(null);
  }

  public Optional<Certificate> findBySerialNumber(String serialNumber) {
    return certificateRepository.findBySerialNumber(serialNumber);
  }

  public List<Certificate> findBySubjectDN(String subjectDN) {
    return certificateRepository.findBySubjectDN(subjectDN);
  }

  public List<Certificate> findByIssuerDN(String issuerDN) {
    return certificateRepository.findByIssuerDN(issuerDN);
  }

  public List<Certificate> findByCertificateType(CertificateType certificateType) {
    return certificateRepository.findByCertificateType(certificateType);
  }

  public List<Certificate> getAllCertificates() {
    return certificateRepository.findAll();
  }

  public CertificateDTO convertToDTO(Certificate certificate) {
    CertificateDTO dto = new CertificateDTO();
    dto.setId(certificate.getId());
    dto.setSerialNumber(certificate.getSerialNumber());
    dto.setStartDate(certificate.getStartDate());
    dto.setEndDate(certificate.getEndDate());
    dto.setCertificateType(certificate.getCertificateType());
    dto.setSubjectDN(certificate.getSubject().getX500NameString());
    dto.setIssuerDN(certificate.getIssuer().getX500NameString());
    
    if (certificate.getX509Certificate() != null) {
      try {
        byte[] certBytes = certificate.getX509Certificate().getEncoded();
        dto.setCertificateBase64(java.util.Base64.getEncoder().encodeToString(certBytes));
        dto.setCertificatePEM("-----BEGIN CERTIFICATE-----\n" +
                             java.util.Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(certBytes) +
                             "\n-----END CERTIFICATE-----\n");
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    
    return dto;
  }

  public List<CertificateDTO> convertToDTOList(List<Certificate> certificates) {
    return certificates.stream()
        .map(this::convertToDTO)
        .collect(java.util.stream.Collectors.toList());
  }

  public boolean isCertificateValid(Certificate certificate) {
      try {
          // is digital signature valid?
          certificate.getX509Certificate().verify(certificate.getIssuer().getPublicKey());

          // is certificate in the date range, is it still valid
          certificate.getX509Certificate().checkValidity();
      } catch(Exception e) {
          return false;
      }

      // is certificate not revoked?
      if (isCertificateRevoked(certificate)) {
          return false;
      }

      if (certificate.getSubject().getIssuerVersion().getId().equals(certificate.getIssuer().getId())) {
          return true;
      }
      else {
          Certificate parentCertificate = certificate.getParentCertificate();
          return isCertificateValid(parentCertificate);
      }
  }

  public int revokeCertificate(Long id, String reason) {
    Optional<Certificate> optionalCertificate = certificateRepository.findById(id);

      if (!optionalCertificate.isPresent()) {
          return 0;
      }

    Certificate certificate = optionalCertificate.get();

    // check if certificate is already revoked
    if (certificate.getRevocationReason() != null) {
        return 0;
    }

    certificate.setRevocationReason(reason);
    certificate.setRevocationDate(new Date());
    certificateRepository.save(certificate);
    crlService.addCertificateToCRL(certificate);

      int count = 1;

      // Find and revoke all children recursively
      List<Certificate> children = findChildCertificates(id);
      for (Certificate child : children) {
          count += revokeCertificate(child.getId(), reason);
      }

      return count;
  }

    private List<Certificate> findChildCertificates(Long parentId) {
        return certificateRepository.findByParentCertificate_Id(parentId);
    }

  public boolean isCertificateRevoked(Certificate certificate) {
    return certificate.getRevocationReason() != null && certificate.getRevocationDate() != null;
  }

  public Optional<Certificate> findById(Long id) {
    return certificateRepository.findById(id);
  }

  public byte[] generateKeystore(KeyPair keyPair, Certificate certificate, String keystoreFormat, String keystorePassword) {
    try {
        KeyStore keyStore = KeyStore.getInstance(keystoreFormat);
        keyStore.load(null, keystorePassword.toCharArray());

        X509Certificate x509Certificate = certificate.getX509Certificate();
        keyStore.setKeyEntry(
            "private-key",  // Alias
            keyPair.getPrivate(),  // Private key
            keystorePassword.toCharArray(),  // Password for accessing the private key
            new java.security.cert.Certificate[] {x509Certificate}  // Certificate chain
        );

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        keyStore.store(outputStream, keystorePassword.toCharArray());
        return outputStream.toByteArray();

    } catch (Exception e) {
        throw new RuntimeException("Failed to generate keystore: " + e.getMessage(), e);
    }
  }

    public IssuerConstraintsDTO getCAConstraints(Long issuerCAId) {
        Optional<Certificate> optionalCertificate = certificateRepository.findById(issuerCAId);
        if (optionalCertificate.isEmpty()) {
            return null;
        }

        Certificate certificate = optionalCertificate.get();

        IssuerConstraintsDTO constraintsDTO = new IssuerConstraintsDTO();
        constraintsDTO.setIssuerId(certificate.getId().toString());
        constraintsDTO.setIssuerValidFrom(certificate.getStartDate().toString());
        constraintsDTO.setIssuerValidTo(certificate.getEndDate().toString());
        constraintsDTO.setIssuerRevoked(certificate.getRevocationReason() != null);

        long maxTTL = Duration.between(
                certificate.getStartDate().toInstant(),
                certificate.getEndDate().toInstant()
        ).toDays();
        constraintsDTO.setMaxTTLDays((int) maxTTL);

        return constraintsDTO;
    }

    public IssueCertificateResponseDTO convertToCertificateResponseDTO(Certificate certificate) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        IssueCertificateResponseDTO dto = new IssueCertificateResponseDTO();
        dto.setId(certificate.getId());
        dto.setSerialNumber(certificate.getSerialNumber());
        dto.setSubjectDN(certificate.getSubject().getX500NameString());
        dto.setIssuerDN(certificate.getIssuer().getX500NameString());
        dto.setValidFrom(dateFormat.format(certificate.getStartDate()));
        dto.setValidTo(dateFormat.format(certificate.getEndDate()));
        dto.setCertificateType(certificate.getCertificateType().toString());
        dto.setKeystoreFormat(certificate.getKeystoreFormat());

        dto.setExtensions(extractExtensionsFromCertificate(certificate));

        return dto;
    }

    /**
     * Extracts extensions from X509Certificate and maps them to simplified format:
     * - BASIC_CA or BASIC_END (based on BasicConstraints)
     * - KEY_USAGE (if digitalSignature + keyEncipherment are present)
     * - EXT_KEY_SERVER (if serverAuth is present)
     * - EXT_KEY_CLIENT (if clientAuth is present)
     * - SAN (if Subject Alternative Names are present)
     */
    public List<String> extractExtensionsFromCertificate(Certificate certificate) {
        List<String> extensions = new ArrayList<>();

        try {
            X509Certificate x509 = certificate.getX509Certificate();

            // 1. Basic Constraints
            int basicConstraints = x509.getBasicConstraints();
            if (basicConstraints >= 0) {
                extensions.add("BASIC_CA");
            } else {
                extensions.add("BASIC_END");
            }

            // 2. Key Usage
            boolean[] keyUsage = x509.getKeyUsage();
            if (keyUsage != null) {
                // Check for digitalSignature (bit 0) and keyEncipherment (bit 2)
                if (keyUsage.length > 2 && keyUsage[0] && keyUsage[2]) {
                    extensions.add("KEY_USAGE");
                }
            }

            // 3. Extended Key Usage
            try {
                List<String> extendedKeyUsage = x509.getExtendedKeyUsage();
                if (extendedKeyUsage != null) {
                    for (String oid : extendedKeyUsage) {
                        if (oid.equals("1.3.6.1.5.5.7.3.1")) { // serverAuth
                            extensions.add("EXT_KEY_SERVER");
                        } else if (oid.equals("1.3.6.1.5.5.7.3.2")) { // clientAuth
                            extensions.add("EXT_KEY_CLIENT");
                        }
                    }
                }
            } catch (Exception e) {
                // Extended Key Usage not present
            }

            // 4. Subject Alternative Names
            try {
                Collection<List<?>> sans = x509.getSubjectAlternativeNames();
                if (sans != null && !sans.isEmpty()) {
                    extensions.add("SAN");
                }
            } catch (Exception e) {
                // SAN not present
            }

        } catch (Exception e) {
            System.err.println("Error extracting extensions: " + e.getMessage());
        }

        return extensions;
    }

    /// /////////////////

    public byte[] convertKeystoreFormat(byte[] keystoreData, String fromFormat, String toFormat, String password) {
        try {
            // Load the source keystore
            KeyStore sourceKeyStore = KeyStore.getInstance(fromFormat);
            sourceKeyStore.load(new java.io.ByteArrayInputStream(keystoreData), password.toCharArray());

            // Create destination keystore
            KeyStore destKeyStore = KeyStore.getInstance(toFormat);
            destKeyStore.load(null, password.toCharArray());

            // Copy all entries
            java.util.Enumeration<String> aliases = sourceKeyStore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();

                if (sourceKeyStore.isKeyEntry(alias)) {
                    java.security.Key key = sourceKeyStore.getKey(alias, password.toCharArray());
                    java.security.cert.Certificate[] chain = sourceKeyStore.getCertificateChain(alias);
                    destKeyStore.setKeyEntry(alias, key, password.toCharArray(), chain);
                } else if (sourceKeyStore.isCertificateEntry(alias)) {
                    java.security.cert.Certificate cert = sourceKeyStore.getCertificate(alias);
                    destKeyStore.setCertificateEntry(alias, cert);
                }
            }

            // Write to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            destKeyStore.store(outputStream, password.toCharArray());
            return outputStream.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Failed to convert keystore format: " + e.getMessage(), e);
        }
    }
}
