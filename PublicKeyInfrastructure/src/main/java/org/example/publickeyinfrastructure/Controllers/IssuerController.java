package org.example.publickeyinfrastructure.Controllers;

import org.example.publickeyinfrastructure.DTOs.Infrastructure.IssuerDTO;
import org.example.publickeyinfrastructure.Entities.Infrastructure.Issuer;
import org.example.publickeyinfrastructure.Services.Infrastucture.IssuerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/issuers")
@CrossOrigin(origins = "*")
public class IssuerController {

    @Autowired
    private IssuerService issuerService;

    @PostMapping
    public ResponseEntity<?> createIssuer(@RequestBody IssuerDTO issuerDTO) {
        try {
            Issuer createdIssuer = issuerService.createIssuer(issuerDTO);
            if (createdIssuer != null) {
                IssuerDTO responseDTO = issuerService.convertToDTO(createdIssuer);
                return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Failed to create issuer");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error creating issuer: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getIssuerById(@PathVariable Long id) {
        Optional<Issuer> issuer = issuerService.getIssuerById(id);
        if (issuer.isPresent()) {
            IssuerDTO responseDTO = issuerService.convertToDTO(issuer.get());
            return ResponseEntity.ok(responseDTO);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("Issuer not found with id: " + id);
        }
    }

    @GetMapping("/x500/{x500Name}")
    public ResponseEntity<?> getIssuerByX500Name(@PathVariable String x500Name) {
        Optional<Issuer> issuer = issuerService.getIssuerByX500Name(x500Name);
        if (issuer.isPresent()) {
            IssuerDTO responseDTO = issuerService.convertToDTO(issuer.get());
            return ResponseEntity.ok(responseDTO);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("Issuer not found with X500Name: " + x500Name);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateIssuer(@PathVariable Long id, @RequestBody IssuerDTO issuerDTO) {
        try {
            Issuer updatedIssuer = issuerService.updateIssuer(id, issuerDTO);
            if (updatedIssuer != null) {
                IssuerDTO responseDTO = issuerService.convertToDTO(updatedIssuer);
                return ResponseEntity.ok(responseDTO);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Issuer not found with id: " + id);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error updating issuer: " + e.getMessage());
        }
    }

    @GetMapping("/exists")
    public ResponseEntity<?> checkIssuerExists(@RequestParam String x500Name) {
        try {
            boolean exists = issuerService.existsByX500Name(x500Name);
            return ResponseEntity.ok(exists);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error checking issuer existence: " + e.getMessage());
        }
    }
}
