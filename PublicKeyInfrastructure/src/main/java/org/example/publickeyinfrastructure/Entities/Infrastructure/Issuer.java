package org.example.publickeyinfrastructure.Entities.Infrastructure;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

import org.bouncycastle.asn1.x500.X500Name;
import org.example.publickeyinfrastructure.Utils.AesGcmEncryptor;

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
    //this.privateKey = this.encryptPrivateKey(privateKey);
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

  private static byte[] getMasterKey() {
    String hex = System.getenv("MASTER");
    if (hex == null || hex.isEmpty()) {
      hex = System.getProperty("MASTER");
    }
    if (hex == null || hex.isEmpty()) {
      throw new IllegalStateException("MASTER key not configured in environment or system properties");
    }
    return hexStringToByteArray(hex);
  }

  private static byte[] hexStringToByteArray(String s) {
    int len = s.length();
    if (len % 2 != 0) {
      throw new IllegalArgumentException("Invalid hex string length");
    }
    byte[] data = new byte[len / 2];
    for (int i = 0; i < len; i += 2) {
      int hi = Character.digit(s.charAt(i), 16);
      int lo = Character.digit(s.charAt(i + 1), 16);
      if (hi < 0 || lo < 0) {
        throw new IllegalArgumentException("Invalid hex character in MASTER key");
      }
      data[i / 2] = (byte) ((hi << 4) + lo);
    }
    return data;
  }

  private PrivateKey encryptPrivateKey(PrivateKey privateKey) {
    try {
      byte[] pkcs8 = privateKey.getEncoded();
      String base64Pkcs8 = Base64.getEncoder().encodeToString(pkcs8);
      String cipherB64 = AesGcmEncryptor.encrypt(base64Pkcs8, getMasterKey());
      this.privateKeyBytes = cipherB64.getBytes(StandardCharsets.UTF_8);
      return null; // do not keep plaintext key in memory field
    } catch (Exception e) {
      throw new RuntimeException("Failed to encrypt private key", e);
    }
  }

  private PrivateKey decryptPrivateKey() {
    try {
      if (this.privateKeyBytes == null) return null;
      String cipherB64 = new String(this.privateKeyBytes, StandardCharsets.UTF_8);
      String base64Pkcs8 = AesGcmEncryptor.decrypt(cipherB64, getMasterKey());
      byte[] pkcs8 = Base64.getDecoder().decode(base64Pkcs8);
      PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(pkcs8);
      KeyFactory kf = KeyFactory.getInstance("RSA");
      return kf.generatePrivate(spec);
    } catch (Exception e) {
      throw new RuntimeException("Failed to decrypt private key", e);
    }
  }
  
//  public PrivateKey getPrivateKey() {
//      return this.decryptPrivateKey();
//  }
}
