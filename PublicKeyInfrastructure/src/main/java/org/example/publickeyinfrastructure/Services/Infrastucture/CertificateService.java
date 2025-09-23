package org.example.publickeyinfrastructure.Services.Infrastucture;

import org.example.publickeyinfrastructure.DTOs.Infrastructure.CreateCertificateDTO;
import org.example.publickeyinfrastructure.DTOs.Infrastructure.SubjectDTO;
import org.example.publickeyinfrastructure.Entities.Infrastructure.Organisation;
import org.example.publickeyinfrastructure.Entities.User.RegularUser;
import org.example.publickeyinfrastructure.Services.Auth.AuthService;
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
  private AuthService authService;

  public CertificateService(CertificateGeneratorService certificateGeneratorService, CertificateRepository certificateRepository, IssuerService issuerService, SubjectService subjectService, AuthService authService) {
    this.certificateRepository = certificateRepository;
    this.certificateGeneratorService = certificateGeneratorService;
    this.issuerService = issuerService;
    this.subjectService = subjectService;
    this.authService = authService;
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
      Issuer issuer = issuerService.createIssuer(new IssuerDTO(
              newCertificate.getSubject().getCommonName(),
              newCertificate.getSubject().getSurname(),
              newCertificate.getSubject().getGivenName(),
              newCertificate.getSubject().getOrganizationName(),
              newCertificate.getSubject().getOrganizationUnit(),
              newCertificate.getSubject().getCountry(),
              newCertificate.getSubject().getEmail()
      ), user.getOrganisation());
      Subject subject = subjectService.createSubject(newCertificate.getSubject());

      X509Certificate certificate = certificateGeneratorService.generateCertificate(subject,
              issuer, newCertificate.getStartDate(), newCertificate.getEndDate(),
              UUID.randomUUID().toString(), newCertificate.getExtensions());

      Certificate created = new Certificate(subject, issuer,
              UUID.randomUUID().toString(), newCertificate.getStartDate(), newCertificate.getEndDate(),
              certificate, CertificateType.ROOT_CA, null);
      certificateRepository.save(created);
      return created;
  }

  public Certificate createIntermediateCA(CreateCertificateDTO newCertificate) {
      Certificate issuerCertificate = getCertificateById(newCertificate.getIssuerCertificateId());

      if (issuerCertificate == null) {
          return null;
      }

      Subject subject = subjectService.createSubject(newCertificate.getSubject());

      if (isCertificateValid(issuerCertificate)) {
          return null;
      }

      // Intermediate CA signs Root CA
      X509Certificate certificate = certificateGeneratorService.generateCertificate(subject,
              issuerCertificate.getIssuer(), newCertificate.getStartDate(),
              newCertificate.getEndDate(), UUID.randomUUID().toString(), newCertificate.getExtensions());

      Certificate created = new Certificate(subject, issuerCertificate.getIssuer(),
              UUID.randomUUID().toString(), newCertificate.getStartDate(), newCertificate.getEndDate(),
              certificate, CertificateType.INTERMEDIATE_CA, issuerCertificate);
      certificateRepository.save(created);
      return created;
  }

  public Certificate createEndEntityCertificate(CreateCertificateDTO newCertificate) {
      Certificate issuerCertificate = getCertificateById(newCertificate.getIssuerCertificateId());
      if (issuerCertificate == null) {
          return null;
      }

      Subject subject = subjectService.createSubject(newCertificate.getSubject());

      if (isCertificateValid(issuerCertificate)) {
          return null;
      }

      X509Certificate certificate = certificateGeneratorService.generateCertificate(subject,
              issuerCertificate.getIssuer(), newCertificate.getStartDate(),
              newCertificate.getEndDate(), UUID.randomUUID().toString(), newCertificate.getExtensions());

      Certificate created = new Certificate(subject, issuerCertificate.getIssuer(),
              UUID.randomUUID().toString(), newCertificate.getStartDate(),
              newCertificate.getEndDate(), certificate, CertificateType.END_ENTITY, issuerCertificate);

      certificateRepository.save(created);
      return created;
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
      // TODO: currently we have no implementation of revoking certificates

      if(certificate.getSubject().getX500NameString().equals(certificate.getIssuer().getX500NameString())) {
          return true;
      }
      else {
          Certificate parentCertificate = certificate.getParentCertificate();
          return isCertificateValid(parentCertificate);
      }
  }
}
