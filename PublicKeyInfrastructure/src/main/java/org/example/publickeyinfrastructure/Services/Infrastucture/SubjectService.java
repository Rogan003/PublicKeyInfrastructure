package org.example.publickeyinfrastructure.Services.Infrastucture;

import org.example.publickeyinfrastructure.DTOs.Infrastructure.SubjectDTO;
import org.example.publickeyinfrastructure.Entities.Infrastructure.Subject;
import org.example.publickeyinfrastructure.Repositories.SubjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;

@Service
@Transactional
public class SubjectService {
  
  @Autowired
  private SubjectRepository subjectRepository;

  public Subject createSubject(SubjectDTO subjectDTO) {
    try {
      KeyPair keyPair = generateKeyPair();
      X500NameBuilder builder = new X500NameBuilder(BCStyle.INSTANCE);
      
      if (subjectDTO.getCommonName() != null) {
        builder.addRDN(BCStyle.CN, subjectDTO.getCommonName());
      }
      if (subjectDTO.getSurname() != null) {
        builder.addRDN(BCStyle.SURNAME, subjectDTO.getSurname());
      }
      if (subjectDTO.getGivenName() != null) {
        builder.addRDN(BCStyle.GIVENNAME, subjectDTO.getGivenName());
      }
      if (subjectDTO.getOrganization() != null) {
        builder.addRDN(BCStyle.O, subjectDTO.getOrganization());
      }
      if (subjectDTO.getCountry() != null) {
        builder.addRDN(BCStyle.C, subjectDTO.getCountry());
      }
      if (subjectDTO.getEmail() != null) {
        builder.addRDN(BCStyle.E, subjectDTO.getEmail());
      }

      Subject subject = new Subject(keyPair.getPublic(), builder.build());
      return subjectRepository.save(subject);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  public Optional<Subject> getSubjectById(Long id) {
    return subjectRepository.findById(id);
  }

  public Optional<Subject> getSubjectByX500Name(String x500Name) {
    return subjectRepository.findByX500Name(x500Name);
  }

  private KeyPair generateKeyPair() throws NoSuchAlgorithmException, NoSuchProviderException {
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
    SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
    keyGen.initialize(2048, random);
    return keyGen.generateKeyPair();
  }

  public SubjectDTO convertToDTO(Subject subject) {
    SubjectDTO dto = new SubjectDTO();
    dto.setId(subject.getId());
    dto.setX500Name(subject.getX500NameString());
    
    String x500String = subject.getX500NameString();
    String[] parts = x500String.split(",");
    
    for (String part : parts) {
      String[] keyValue = part.trim().split("=");
      if (keyValue.length == 2) {
        String key = keyValue[0].trim();
        String value = keyValue[1].trim();
        
        switch (key) {
          case "CN":
            dto.setCommonName(value);
            break;
          case "SURNAME":
            dto.setSurname(value);
            break;
          case "GIVENNAME":
            dto.setGivenName(value);
            break;
          case "O":
            dto.setOrganization(value);
            break;
          case "C":
            dto.setCountry(value);
            break;
          case "E":
            dto.setEmail(value);
            break;
        }
      }
    }
    
    return dto;
  }

  public List<SubjectDTO> convertToDTOList(List<Subject> subjects) {
    return subjects.stream()
        .map(this::convertToDTO)
        .collect(Collectors.toList());
  }
}
