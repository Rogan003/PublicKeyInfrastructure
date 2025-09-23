package org.example.publickeyinfrastructure.Controllers;

import jakarta.validation.Valid;
import org.example.publickeyinfrastructure.DTOs.Infrastructure.CACertificateInfoDTO;
import org.example.publickeyinfrastructure.DTOs.Csr.CAInfoDTO;
import org.example.publickeyinfrastructure.DTOs.Infrastructure.CreateCertificateDTO;
import org.example.publickeyinfrastructure.Entities.Infrastructure.Certificate;
import org.example.publickeyinfrastructure.DTOs.Infrastructure.CertificateDTO;
import org.example.publickeyinfrastructure.Entities.Enums.CertificateType;
import org.example.publickeyinfrastructure.Services.Infrastucture.CertificateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.*;

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

            if (request.getIssuerCertificateId() != null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("There is no issuer certificate for a root CA");
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

            if (request.getIssuerCertificateId() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("There is no issuer certificate!");
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

            if (request.getIssuerCertificateId() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("There is no issuer certificate!");
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

    @GetMapping("/ca-certificates")
    public ResponseEntity<?> getAllCACertificates() {
        try {
            List<Certificate> caCertificates = certificateService.findByCertificateType(CertificateType.ROOT_CA);
            List<Certificate> intermediateCertificates = certificateService.findByCertificateType(CertificateType.INTERMEDIATE_CA);

            List<CACertificateInfoDTO> responseDTO = new ArrayList<>();

            for (Certificate caCertificate : caCertificates) {
                responseDTO.add(new CACertificateInfoDTO(
                        caCertificate.getId(),
                        caCertificate.getSubject().getX500NameString())
                );
            }

            for (Certificate intermediateCertificate : intermediateCertificates) {
                responseDTO.add(new CACertificateInfoDTO(
                        intermediateCertificate.getId(),
                        intermediateCertificate.getSubject().getX500NameString())
                );
            }

            return ResponseEntity.ok(responseDTO);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving CAs: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/revoke")
    public ResponseEntity<?> revokeCertificate(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            String reason = request.get("reason");

            if (reason == null || reason.isEmpty()) {
                return ResponseEntity.badRequest().body("Revocation reason is required");
            }

            boolean revoked = certificateService.revokeCertificate(id, reason);
            if (!revoked) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Certificate could not be revoked. It may not exist or is already revoked.");
            }

            return ResponseEntity.ok("Certificate revoked successfully");

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error revoking certificate: " + e.getMessage());
        }
    }

    @GetMapping("/ca")
    public ResponseEntity<?> getAllCAs() {
        try {
            List<Certificate> caCertificates = certificateService.findByCertificateType(CertificateType.ROOT_CA);
            List<Certificate> intermediateCertificates = certificateService.findByCertificateType(CertificateType.INTERMEDIATE_CA);

            List<CAInfoDTO> responseDTO = certificateService.convertToCAInfoDTOList(caCertificates, intermediateCertificates);
            return ResponseEntity.ok(responseDTO);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error retrieving CAs: " + e.getMessage());
        }
    }
}
