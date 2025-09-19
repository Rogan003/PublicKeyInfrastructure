package org.example.publickeyinfrastructure.DTOs.Infrastructure;

import org.example.publickeyinfrastructure.Entities.Infrastructure.Organisation;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrganisationDTO {
  private Long id;
  private String name;
  private String unit;
  private String country;

  // Default constructor required for JSON deserialization
  public OrganisationDTO() {}

  public OrganisationDTO(Organisation organisation) {
    this.id = organisation.getId();
    this.name = organisation.getName();
    this.unit = organisation.getUnit();
    this.country = organisation.getCountry();
  }

  public Organisation toEntity(OrganisationDTO organisation) {
    return new Organisation(organisation.getId(), organisation.getName(), organisation.getUnit(), organisation.getCountry());
  }
}
