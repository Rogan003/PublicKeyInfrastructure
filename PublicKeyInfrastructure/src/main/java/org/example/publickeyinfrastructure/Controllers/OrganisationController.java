package org.example.publickeyinfrastructure.Controllers;

import org.example.publickeyinfrastructure.DTOs.Infrastructure.OrganisationDTO;
import org.example.publickeyinfrastructure.Entities.Infrastructure.Organisation;
import org.example.publickeyinfrastructure.Services.Infrastucture.OrganisationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/organisations")
@CrossOrigin(origins = "*")
public class OrganisationController {

    @Autowired
    private OrganisationService organisationService;

    @GetMapping
    public ResponseEntity<List<OrganisationDTO>> getAllOrganisations() {
        List<Organisation> organisations = organisationService.getAllOrganisations();
        List<OrganisationDTO> response = organisationService.convertToDTOList(organisations);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<OrganisationDTO> createOrganisation(@RequestBody OrganisationDTO organisationDTO) {
        Organisation organisation = organisationService.createOrganisation(organisationDTO);
        OrganisationDTO response = organisationService.convertToDTO(organisation);
        return ResponseEntity.ok(response);
    }
}


