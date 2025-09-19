package org.example.publickeyinfrastructure.Entities.Infrastructure;

import lombok.Getter;
import lombok.Setter;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

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

  // Default constructor required by JPA
  public Organisation() {}

  public Organisation(Long id, String name, String unit, String country) {
    this.id = id;
    this.name = name;
    this.unit = unit;
    this.country = country;
  }

}
