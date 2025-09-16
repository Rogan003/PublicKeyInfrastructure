package org.example.publickeyinfrastructure.Services.Infrastucture;

import org.example.publickeyinfrastructure.DTOs.Infrastructure.CreateCertificateDTO;
import org.example.publickeyinfrastructure.DTOs.Infrastructure.SubjectDTO;
import org.springframework.stereotype.Service;
import org.example.publickeyinfrastructure.Entities.Infrastructure.Issuer;
import org.example.publickeyinfrastructure.Entities.Infrastructure.Subject;
import org.example.publickeyinfrastructure.DTOs.Infrastructure.CertificateDTO;
import org.example.publickeyinfrastructure.DTOs.Infrastructure.IssuerDTO;
import org.example.publickeyinfrastructure.Entities.Enums.CertificateType;


import  org.example.publickeyinfrastructure.Entities.Infrastructure.Certificate;
import org.example.publickeyinfrastructure.Repositories.CertificateRepository;

import java.security.*;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;


@Service
public class CertificateService {
  private SubjectService subjectService;
  private CertificateGeneratorService certificateGeneratorService;
  private CertificateRepository certificateRepository;
  private IssuerService issuerService;

  public CertificateService(CertificateGeneratorService certificateGeneratorService, CertificateRepository certificateRepository, IssuerService issuerService, SubjectService subjectService) {
    this.certificateRepository = certificateRepository;
    this.certificateGeneratorService = certificateGeneratorService;
    this.issuerService = issuerService;
    this.subjectService = subjectService;
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
      Optional<Issuer> rootCAOptional = issuerService.getIssuerById(newCertificate.getSubject().getId());
      if (!rootCAOptional.isPresent()) {
          return null;
      }
      Issuer rootCA = rootCAOptional.get();
      Subject rootSubject = new Subject(rootCA.getPublicKey(), rootCA.getX500Name());

      List<Certificate> issuerCertificates = findBySubjectDN(rootCA.getX500NameString());
      boolean isValidIssuerCertificate = issuerCertificates.stream()
              .anyMatch(this::isCertificateValid);

      if (!isValidIssuerCertificate) {
          return null;
      }

      X509Certificate certificate = certificateGeneratorService.generateCertificate(rootSubject,
              rootCA, newCertificate.getStartDate(), newCertificate.getEndDate(),
              UUID.randomUUID().toString(), newCertificate.getExtensions());

      Certificate created = new Certificate(rootSubject, rootCA,
              UUID.randomUUID().toString(), newCertificate.getStartDate(), newCertificate.getEndDate(),
              certificate, CertificateType.ROOT_CA);
      certificateRepository.save(created);
      return created;
  }

  public Certificate createIntermediateCA(CreateCertificateDTO newCertificate) {
      Optional<Certificate> issuerCertificateOptional = findBySerialNumber(newCertificate.getIssuerCertificate().getSerialNumber());
      Optional<Subject> subjectOptional = subjectService.getSubjectById(newCertificate.getSubject().getId());

      if (!issuerCertificateOptional.isPresent() || !subjectOptional.isPresent()) {
          return null;
      }

      Certificate issuerCertificate = issuerCertificateOptional.get();
      // Subject intermediateSubject = new Subject(intermediateCA.getPublicKey(), intermediateCA.getX500Name());
      Subject subject = subjectOptional.get();

      if (subject == null) {
          subject = subjectService.createSubject(newCertificate.getSubject());
      }

      if (isCertificateValid(issuerCertificate)) {
          return null;
      }

      // Intermediate CA signs Root CA
      X509Certificate certificate = certificateGeneratorService.generateCertificate(subject,
              issuerCertificate.getIssuer(), newCertificate.getStartDate(),
              newCertificate.getEndDate(), UUID.randomUUID().toString(), newCertificate.getExtensions());

      Certificate created = new Certificate(subject, issuerCertificate.getIssuer(),
              UUID.randomUUID().toString(), newCertificate.getStartDate(), newCertificate.getEndDate(),
              certificate, CertificateType.INTERMEDIATE_CA);
      certificateRepository.save(created);
      return created;
  }

  public Certificate createEndEntityCertificate(CreateCertificateDTO newCertificate) {
      Optional<Certificate> issuerCertificateOptional = findBySerialNumber(newCertificate.getIssuerCertificate().getSerialNumber());
      Optional<Subject> subjectOptional = subjectService.getSubjectById(newCertificate.getSubject().getId());

      if (!issuerCertificateOptional.isPresent()) {
          return null;
      }

      Certificate issuerCertificate = issuerCertificateOptional.get();
      Subject subject = subjectOptional.get();

      if (subject == null) {
          subject = subjectService.createSubject(newCertificate.getSubject());
      }

      if (isCertificateValid(issuerCertificate)) {
          return null;
      }

      X509Certificate certificate = certificateGeneratorService.generateCertificate(subject,
              issuerCertificate.getIssuer(), newCertificate.getStartDate(),
              newCertificate.getEndDate(), UUID.randomUUID().toString(), newCertificate.getExtensions());

      Certificate created = new Certificate(subject, issuerCertificate.getIssuer(),
              UUID.randomUUID().toString(), newCertificate.getStartDate(),
              newCertificate.getEndDate(), certificate, CertificateType.END_ENTITY);

      certificateRepository.save(created);
      return created;
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
      // TODO: currently we have no implementation of revoking certificates

      if(certificate.getSubject().getX500NameString().equals(certificate.getIssuer().getX500NameString())) {
          return true;
      }
      else {
          Issuer issuer = certificate.getIssuer();
          // should we only check the certificate that issued this certificate
          // or all certificates from the issuer?
          List<Certificate> issuerCertificates = findByIssuerDN(issuer.getX500NameString());
          return issuerCertificates.stream()
                  .anyMatch(this::isCertificateValid);
      }
  }
}
