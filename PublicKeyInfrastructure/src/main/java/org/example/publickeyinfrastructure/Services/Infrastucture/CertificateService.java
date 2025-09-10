package org.example.publickeyinfrastructure.Services.Infrastucture;

import org.springframework.stereotype.Service;
import org.example.publickeyinfrastructure.Entities.Infrastructure.Issuer;
import org.example.publickeyinfrastructure.Entities.Infrastructure.Subject;
import org.example.publickeyinfrastructure.DTOs.Infrastructure.CertificateDTO;
import org.example.publickeyinfrastructure.Entities.Enums.CertificateType;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;


import  org.example.publickeyinfrastructure.Entities.Infrastructure.Certificate;
import org.example.publickeyinfrastructure.Repositories.CertificateRepository;

import java.security.*;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Service
public class CertificateService {

  private CertificateGeneratorService certificateGeneratorService;
  private CertificateRepository certificateRepository;

  public CertificateService(CertificateGeneratorService certificateGeneratorService, CertificateRepository certificateRepository) {
    this.certificateRepository = certificateRepository;
    this.certificateGeneratorService = certificateGeneratorService;
  }
  
  public Subject generateSubject() {
    KeyPair keyPairSubject = generateKeyPair();

    //class X500NameBuilder creates X500Name object which represents the data about the owner
    X500NameBuilder builder = new X500NameBuilder(BCStyle.INSTANCE);
    builder.addRDN(BCStyle.CN, "Ivana Kovacevic");
    builder.addRDN(BCStyle.SURNAME, "Kovacevic");
    builder.addRDN(BCStyle.GIVENNAME, "Ivana");
    builder.addRDN(BCStyle.O, "UNS-FTN");
    builder.addRDN(BCStyle.OU, "Katedra za informatiku");
    builder.addRDN(BCStyle.C, "RS");
    builder.addRDN(BCStyle.E, "kovacevic.ivana@uns.ac.rs");
    //UID (USER ID) is the ID of the user
    builder.addRDN(BCStyle.UID, "123456");

    return new Subject(keyPairSubject.getPublic(), builder.build());
  }

  public Issuer generateIssuer() {
    KeyPair kp = generateKeyPair();
    X500NameBuilder builder = new X500NameBuilder(BCStyle.INSTANCE);
    builder.addRDN(BCStyle.CN, "IT sluzba");
    builder.addRDN(BCStyle.SURNAME, "sluzba");
    builder.addRDN(BCStyle.GIVENNAME, "IT");
    builder.addRDN(BCStyle.O, "UNS-FTN");
    builder.addRDN(BCStyle.OU, "Katedra za informatiku");
    builder.addRDN(BCStyle.C, "RS");
    builder.addRDN(BCStyle.E, "itsluzba@uns.ac.rs");
    //UID (USER ID) je ID korisnika
    builder.addRDN(BCStyle.UID, "654321");

    //The data for the issuer is created, which in this case includes:
    // - private key which will be used to sign the certificate that is issued
    // - data about the owner of the certificate that issues the new certificate
    return new Issuer(kp.getPrivate(), kp.getPublic(), builder.build());
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

 public Certificate createRootCA(Issuer rootCA) {
  try {
      // Root CA signs itself
      Subject rootSubject = new Subject(rootCA.getPublicKey(), rootCA.getX500Name());

      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
      Date startDate = sdf.parse("2025-01-01");
      Date endDate = sdf.parse("2035-01-01"); // Root CA has a longer lifespan

      X509Certificate certificate = certificateGeneratorService.generateCertificate(rootSubject,
              rootCA, startDate, endDate, UUID.randomUUID().toString());

      Certificate created = new Certificate(rootSubject, rootCA,
              UUID.randomUUID().toString(), startDate, endDate, certificate, CertificateType.ROOT_CA);
      certificateRepository.save(created);
      return created;
  } catch (ParseException e) {  
      e.printStackTrace();
  }
  return null;
}

  public Certificate createIntermediateCA(Issuer rootCA, Issuer intermediateCA) {
    try {
        Subject intermediateSubject = new Subject(intermediateCA.getPublicKey(), intermediateCA.getX500Name());

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date startDate = sdf.parse("2025-01-01");
        Date endDate = sdf.parse("2030-01-01");

        // Intermediate CA signs Root CA
        X509Certificate certificate = certificateGeneratorService.generateCertificate(intermediateSubject,
                rootCA, startDate, endDate, UUID.randomUUID().toString());

        Certificate created = new Certificate(intermediateSubject, rootCA,
                UUID.randomUUID().toString(), startDate, endDate, certificate, CertificateType.INTERMEDIATE_CA);
        certificateRepository.save(created);
        return created;
    } catch (ParseException e) {
        e.printStackTrace();
    }
    return null;
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
}
