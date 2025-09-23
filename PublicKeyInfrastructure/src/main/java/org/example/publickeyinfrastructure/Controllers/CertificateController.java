package org.example.publickeyinfrastructure.Controllers;

import jakarta.validation.Valid;
import org.example.publickeyinfrastructure.DTOs.Infrastructure.*;
import org.example.publickeyinfrastructure.Entities.Infrastructure.Certificate;
import org.example.publickeyinfrastructure.DTOs.Infrastructure.CertificateDTO;
import org.example.publickeyinfrastructure.Entities.Enums.CertificateType;
import org.example.publickeyinfrastructure.Entities.User.Role;
import org.example.publickeyinfrastructure.Entities.User.User;
import org.example.publickeyinfrastructure.Services.Auth.AuthService;
import org.example.publickeyinfrastructure.Services.Infrastucture.CertificateService;
import org.example.publickeyinfrastructure.Utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.security.cert.X509Certificate;
import java.util.*;

@RestController
@RequestMapping("/api/certificates")
@CrossOrigin(origins = "*")
public class CertificateController {

    @Autowired
    private CertificateService certificateService;
    @Autowired
    private AuthService authService;
    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping
    public ResponseEntity<?> getAllCertificates(@RequestHeader(value = "Authorization", required = true) String token) {
        User user = null;
        if(token != null) {
            token = token.substring(7);

            try {
                user = authService.getUserById(jwtUtil.extractUserId(token));
            }
            catch (Exception ignored) {
            }
        }

        try {
            List<Certificate> certificates = certificateService.getAllCertificates();
            List<CertificateDetailsDTO> responseDTOs = new ArrayList<>();

            for (Certificate certificate : certificates) {
                if (user == null || (user.getRole() == Role.REGULAR_USER && !certificate.getOwner().getId().equals(user.getId()))) {
                    continue;
                } else if (user.getRole() == Role.CERTIFICATE_AUTHORITY && !isUserInCertificateChain(certificate, user)) {
                    continue;
                }
                List<String> keyUsageStrings = getKeyUsageStrings(certificate.getX509Certificate());
                List<String> extendedKeyUsageStrings = getExtendedKeyUsageStrings(certificate.getX509Certificate());

                responseDTOs.add(new CertificateDetailsDTO(
                        certificate.getId(),
                        certificate.getSubject().getX500NameString(),
                        certificate.getIssuer().getX500NameString(),
                        certificate.getStartDate(),
                        certificate.getEndDate(),
                        certificate.getX509Certificate().getBasicConstraints() >= 0,
                        keyUsageStrings,
                        extendedKeyUsageStrings
                ));
            }

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
    @PreAuthorize("hasRole('ADMIN')")
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
    @PreAuthorize("hasRole('ADMIN') or hasRole('CERTIFICATE_AUTHORITY')")
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

    private final String[] KEY_USAGE_NAMES = {
            "digitalSignature",   // 0
            "nonRepudiation",     // 1
            "keyEncipherment",    // 2
            "dataEncipherment",   // 3
            "keyAgreement",       // 4
            "keyCertSign",        // 5
            "cRLSign",            // 6
            "encipherOnly",       // 7
            "decipherOnly"        // 8
    };

    private final Map<String, String> EXT_KEY_USAGE_NAMES = Map.ofEntries(
            Map.entry("1.3.6.1.5.5.7.3.1", "TLS Web Server Authentication"),
            Map.entry("1.3.6.1.5.5.7.3.2", "TLS Web Client Authentication"),
            Map.entry("1.3.6.1.5.5.7.3.3", "Code Signing"),
            Map.entry("1.3.6.1.5.5.7.3.4", "Email Protection"),
            Map.entry("1.3.6.1.5.5.7.3.8", "Time Stamping"),
            Map.entry("1.3.6.1.5.5.7.3.9", "OCSP Signing")
            // add more as needed
    );

    private List<String> getKeyUsageStrings(X509Certificate cert) {
        List<String> usages = new ArrayList<>();
        boolean[] keyUsage = cert.getKeyUsage();
        if (keyUsage != null) {
            for (int i = 0; i < keyUsage.length && i < KEY_USAGE_NAMES.length; i++) {
                if (keyUsage[i]) {
                    usages.add(KEY_USAGE_NAMES[i]);
                }
            }
        }
        return usages;
    }

    private List<String> getExtendedKeyUsageStrings(X509Certificate cert) throws Exception {
        List<String> usages = new ArrayList<>();
        List<String> eku = cert.getExtendedKeyUsage();
        if (eku != null) {
            for (String oid : eku) {
                usages.add(EXT_KEY_USAGE_NAMES.getOrDefault(oid, "Unknown OID: " + oid));
            }
        }
        return usages;
    }

    private boolean isUserInCertificateChain(Certificate certificate, User user) {
        if (certificate == null) return false;

        return certificate.getOwner().getId().equals(user.getId()) ||
                isUserInCertificateChain(certificate.getParentCertificate(), user);
    }

}
