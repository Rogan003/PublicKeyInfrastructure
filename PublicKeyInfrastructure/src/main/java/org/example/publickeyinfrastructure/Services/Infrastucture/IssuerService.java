package org.example.publickeyinfrastructure.Services.Infrastucture;

import org.example.publickeyinfrastructure.DTOs.Infrastructure.IssuerDTO;
import org.example.publickeyinfrastructure.Entities.Infrastructure.Issuer;
import org.example.publickeyinfrastructure.Repositories.IssuerRepository;
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
public class IssuerService {
  
  @Autowired
  private IssuerRepository issuerRepository;

  public Issuer createIssuer(IssuerDTO issuerDTO) {
    try {
      KeyPair keyPair = generateKeyPair();
      X500NameBuilder builder = new X500NameBuilder(BCStyle.INSTANCE);
      
      if (issuerDTO.getCommonName() != null) {
        builder.addRDN(BCStyle.CN, issuerDTO.getCommonName());
      }
      if (issuerDTO.getOrganization() != null) {
        builder.addRDN(BCStyle.O, issuerDTO.getOrganization());
      }
      if (issuerDTO.getCountry() != null) {
        builder.addRDN(BCStyle.C, issuerDTO.getCountry());
      }
      if (issuerDTO.getEmail() != null) {
        builder.addRDN(BCStyle.E, issuerDTO.getEmail());
      }

      Issuer issuer = new Issuer(keyPair.getPrivate(), keyPair.getPublic(), builder.build());
      return issuerRepository.save(issuer);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  public Optional<Issuer> getIssuerById(Long id) {
    return issuerRepository.findById(id);
  }

  public Optional<Issuer> getIssuerByX500Name(String x500Name) {
    return issuerRepository.findByX500Name(x500Name);
  }

  public Issuer updateIssuer(Long id, IssuerDTO issuerDTO) {
    Optional<Issuer> optionalIssuer = issuerRepository.findById(id);
    if (optionalIssuer.isPresent()) {
      Issuer issuer = optionalIssuer.get();
      
      X500NameBuilder builder = new X500NameBuilder(BCStyle.INSTANCE);
      if (issuerDTO.getCommonName() != null) {
        builder.addRDN(BCStyle.CN, issuerDTO.getCommonName());
      }
      if (issuerDTO.getOrganization() != null) {
        builder.addRDN(BCStyle.O, issuerDTO.getOrganization());
      }
      if (issuerDTO.getCountry() != null) {
        builder.addRDN(BCStyle.C, issuerDTO.getCountry());
      }
      if (issuerDTO.getEmail() != null) {
        builder.addRDN(BCStyle.E, issuerDTO.getEmail());
      }
      
      issuer.setX500Name(builder.build());
      return issuerRepository.save(issuer);
    }
    return null;
  }

  public boolean existsByX500Name(String x500Name) {
    return issuerRepository.existsByX500Name(x500Name);
  }

  private KeyPair generateKeyPair() throws NoSuchAlgorithmException, NoSuchProviderException {
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
    SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
    keyGen.initialize(2048, random);
    return keyGen.generateKeyPair();
  }

  public IssuerDTO convertToDTO(Issuer issuer) {
    IssuerDTO dto = new IssuerDTO();
    dto.setId(issuer.getId());
    dto.setX500Name(issuer.getX500NameString());
    
    String x500String = issuer.getX500NameString();
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

  public List<IssuerDTO> convertToDTOList(List<Issuer> issuers) {
    return issuers.stream()
        .map(this::convertToDTO)
        .collect(Collectors.toList());
  }
}
