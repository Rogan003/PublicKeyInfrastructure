package org.example.publickeyinfrastructure.Controllers;

import jakarta.validation.Valid;
import org.example.publickeyinfrastructure.DTOs.Infrastructure.CreateCertificateDTO;
import org.example.publickeyinfrastructure.Entities.Infrastructure.Certificate;
import org.example.publickeyinfrastructure.DTOs.Infrastructure.CertificateDTO;
import org.example.publickeyinfrastructure.DTOs.Infrastructure.IssuerDTO;
import org.example.publickeyinfrastructure.Entities.Enums.CertificateType;
import org.example.publickeyinfrastructure.Services.Infrastucture.CertificateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/api/certificates")
@CrossOrigin(origins = "*")
public class CertificateController {

    @Autowired
    private CertificateService certificateService;

    @GetMapping
    public ResponseEntity<?> getAllCertificates() {
        try {
            List<Certificate> certificates = certificateService.getAllCertificates();
            List<CertificateDTO> responseDTOs = certificateService.convertToDTOList(certificates);
            return ResponseEntity.ok(responseDTOs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error retrieving certificates: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getCertificateById(@PathVariable Long id) {
        try {
            Optional<Certificate> certificate = certificateService.findBySerialNumber(id.toString());
            if (certificate.isPresent()) {
                CertificateDTO responseDTO = certificateService.convertToDTO(certificate.get());
                return ResponseEntity.ok(responseDTO);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Certificate not found with id: " + id);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error retrieving certificate: " + e.getMessage());
        }
    }

    @GetMapping("/serial/{serialNumber}")
    public ResponseEntity<?> getCertificateBySerialNumber(@PathVariable String serialNumber) {
        try {
            Optional<Certificate> certificate = certificateService.findBySerialNumber(serialNumber);
            if (certificate.isPresent()) {
                CertificateDTO responseDTO = certificateService.convertToDTO(certificate.get());
                return ResponseEntity.ok(responseDTO);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Certificate not found with serial number: " + serialNumber);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error retrieving certificate: " + e.getMessage());
        }
    }

    @GetMapping("/subject/{subjectDN}")
    public ResponseEntity<?> getCertificatesBySubject(@PathVariable String subjectDN) {
        try {
            List<Certificate> certificates = certificateService.findBySubjectDN(subjectDN);
            List<CertificateDTO> responseDTOs = certificateService.convertToDTOList(certificates);
            return ResponseEntity.ok(responseDTOs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error retrieving certificates by subject: " + e.getMessage());
        }
    }

    @GetMapping("/issuer/{issuerDN}")
    public ResponseEntity<?> getCertificatesByIssuer(@PathVariable String issuerDN) {
        try {
            List<Certificate> certificates = certificateService.findByIssuerDN(issuerDN);
            List<CertificateDTO> responseDTOs = certificateService.convertToDTOList(certificates);
            return ResponseEntity.ok(responseDTOs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error retrieving certificates by issuer: " + e.getMessage());
        }
    }

    @GetMapping("/type/{certificateType}")
    public ResponseEntity<?> getCertificatesByType(@PathVariable CertificateType certificateType) {
        try {
            List<Certificate> certificates = certificateService.findByCertificateType(certificateType);
            List<CertificateDTO> responseDTOs = certificateService.convertToDTOList(certificates);
            return ResponseEntity.ok(responseDTOs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error retrieving certificates by type: " + e.getMessage());
        }
    }

    @PostMapping("/root-ca")
    public ResponseEntity<?> createRootCA(@RequestBody @Valid CreateCertificateDTO request,
                                          BindingResult bindingResult) {
        try {
            if (request == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Data for the end entity certificate is required");
            }

            if (bindingResult.hasErrors()) {
                return ResponseEntity.badRequest().body(bindingResult.getAllErrors());
            }

            // this may need to change, not sure if this is the right check
            // but the point is if issuer != subject => can't issue this certificate
            if (!Objects.equals(request.getIssuerCertificate().getIssuerDN(), request.getSubject().getX500Name())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Issuer and subject must be the same");
            }
            Certificate rootCA = certificateService.createRootCA(request);
            if (rootCA != null) {
                CertificateDTO responseDTO = certificateService.convertToDTO(rootCA);
                return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Failed to create Root CA");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error creating Root CA: " + e.getMessage());
        }
    }

    @PostMapping("/intermediate-ca")
    public ResponseEntity<?> createIntermediateCA(@RequestBody @Valid CreateCertificateDTO request,
                                                  BindingResult bindingResult) {
        try {
            if (request == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Data for the end entity certificate is required");
            }

            if (bindingResult.hasErrors()) {
                return ResponseEntity.badRequest().body(bindingResult.getAllErrors());
            }
            
            Certificate intermediateCA = certificateService.createIntermediateCA(request);
            if (intermediateCA != null) {
                CertificateDTO responseDTO = certificateService.convertToDTO(intermediateCA);
                return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Failed to create Intermediate CA");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error creating Intermediate CA: " + e.getMessage());
        }
    }

    @PostMapping("/end-entity")
    public ResponseEntity<?> createEndEntity(@RequestBody @Valid CreateCertificateDTO request,
                                             BindingResult bindingResult) {
        try {
            if (request == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Data for the end entity certificate is required");
            }

            if (bindingResult.hasErrors()) {
                return ResponseEntity.badRequest().body(bindingResult.getAllErrors());
            }

            Certificate endEntityCertificate = certificateService.createEndEntityCertificate(request);

            if (endEntityCertificate != null) {
                CertificateDTO responseDTO = certificateService.convertToDTO(endEntityCertificate);
                return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Failed to create end-entity certificate");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating end-entity certificate: " + e.getMessage());
        }
    }

}
