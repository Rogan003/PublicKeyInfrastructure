package org.example.publickeyinfrastructure.Entities.Infrastructure;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;

import jakarta.persistence.*;
import org.bouncycastle.asn1.x500.X500Name;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.example.publickeyinfrastructure.Utils.KeyUtil;

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

  @ManyToOne
  @JoinColumn(name = "organisation_id")
  private Organisation organisation;

  public Issuer(PrivateKey privateKey, PublicKey publicKey, X500Name x500Name, Organisation organisation) {
    this.privateKey = privateKey;
    this.publicKey = publicKey;
    this.x500Name = x500Name;
    this.x500NameString = x500Name.toString();
    this.organisation = organisation;
    
    try {
        this.privateKeyBytes = KeyUtil.encryptPrivateKey(privateKey.getEncoded(), this.organisation.getKeyBytes());
        this.publicKeyBytes = publicKey.getEncoded();
    } catch (Exception e) {
        e.printStackTrace();
    }
  }
  
  public PrivateKey getPrivateKey() {
      try {
          byte[] decryptedKeyBytes = KeyUtil.decryptPrivateKey(this.privateKeyBytes, this.organisation.getKeyBytes());
          PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decryptedKeyBytes);
          KeyFactory keyFactory = KeyFactory.getInstance("RSA");
          return keyFactory.generatePrivate(keySpec);
      } catch (Exception e) {
          e.printStackTrace();
          return null;
      }
  }
}
