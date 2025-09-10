package org.example.publickeyinfrastructure.Entities.Infrastructure;

import java.security.PrivateKey;
import java.security.PublicKey;

import org.bouncycastle.asn1.x500.X500Name;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Lob;
import jakarta.persistence.Column;
import jakarta.persistence.Transient;

@Entity
@Table(name = "issuers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Issuer {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  
  @Lob
  @Column(name = "private_key")
  private byte[] privateKeyBytes;
  
  @Lob
  @Column(name = "public_key")
  private byte[] publicKeyBytes;
  
  @Column(name = "x500_name", length = 1000)
  private String x500NameString;
  
  @Transient
  private PrivateKey privateKey;
  
  @Transient
  private PublicKey publicKey;
  
  @Transient
  private X500Name x500Name;

  public Issuer(PrivateKey privateKey, PublicKey publicKey, X500Name x500Name) {
    this.privateKey = privateKey;
    this.publicKey = publicKey;
    this.x500Name = x500Name;
    this.x500NameString = x500Name.toString();
    
    try {
      this.privateKeyBytes = privateKey.getEncoded();
      this.publicKeyBytes = publicKey.getEncoded();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
