package org.example.publickeyinfrastructure.Entities.Infrastructure;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.example.publickeyinfrastructure.Utils.KeyUtil;

import javax.crypto.SecretKey;

@Getter
@Setter @Entity
@Table(name = "organisations")
public class Organisation {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  
  @Column(name = "name")
  private String name;

  @Column(name = "unit")
  private String unit;

  @Column(name = "country")
  private String country;

  @Transient
  private SecretKey key;

  @Lob
  @Column(name = "private_key")
  private byte[] keyBytes;

  // Default constructor required by JPA
  public Organisation() {}

  public Organisation(Long id, String name, String unit, String country) {
    this.id = id;
    this.name = name;
    this.unit = unit;
    this.country = country;

    try {
        this.key = KeyUtil.generateAes256Key();
        this.keyBytes = key.getEncoded();
    } catch (Exception e) {
        e.printStackTrace();
    }
  }

}
