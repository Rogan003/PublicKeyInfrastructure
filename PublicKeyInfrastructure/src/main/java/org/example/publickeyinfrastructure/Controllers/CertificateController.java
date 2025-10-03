package org.example.publickeyinfrastructure.Controllers;

import jakarta.validation.Valid;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.example.publickeyinfrastructure.DTOs.Csr.IssuerConstraintsDTO;
import org.example.publickeyinfrastructure.DTOs.Infrastructure.CACertificateInfoDTO;
import org.example.publickeyinfrastructure.DTOs.Csr.CAInfoDTO;
import org.example.publickeyinfrastructure.DTOs.Infrastructure.CreateCertificateDTO;
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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
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
                if (certificate.getRevocationReason() != null) {
                    continue;
                }

                if (user == null || (user.getRole() == Role.REGULAR_USER && !isUsersCertificate(certificate, user))) {
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
                        extendedKeyUsageStrings,
                        certificate.getKeystoreFormat()
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
    public ResponseEntity<?> createIntermediateCA(@RequestHeader(value = "Authorization", required = true) String token,
                                                  @RequestBody @Valid CreateCertificateDTO request,
                                                  BindingResult bindingResult) {
        try {
            User user = null;
            if(token != null) {
                token = token.substring(7);

                try {
                    user = authService.getUserById(jwtUtil.extractUserId(token));
                }
                catch (Exception ignored) {
                }
            }

            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("User is not authenticated");
            }

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

            Certificate intermediateCA = certificateService.createIntermediateCA(request, user.getRole() == Role.ADMIN);
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
    public ResponseEntity<?> getAllCACertificates(@RequestHeader(value = "Authorization", required = true) String token) {
        try {
            User user = null;
            if(token != null) {
                token = token.substring(7);

                try {
                    user = authService.getUserById(jwtUtil.extractUserId(token));
                }
                catch (Exception ignored) {
                }
            }

            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("User is not authenticated");
            }

            List<Certificate> caCertificates = certificateService.findByCertificateType(CertificateType.ROOT_CA);
            List<Certificate> intermediateCertificates = certificateService.findByCertificateType(CertificateType.INTERMEDIATE_CA);

            List<CACertificateInfoDTO> responseDTO = new ArrayList<>();

            for (Certificate caCertificate : caCertificates) {
                if (caCertificate.getRevocationReason() != null) {
                    continue;
                }

                if (user.getRole() != Role.ADMIN && !isCertifacateInUserOrganisation(caCertificate, user)) {
                    continue;
                }

                responseDTO.add(new CACertificateInfoDTO(
                        caCertificate.getId(),
                        caCertificate.getSubject().getX500NameString())
                );
            }

            for (Certificate intermediateCertificate : intermediateCertificates) {
                if (intermediateCertificate.getRevocationReason() != null) {
                    continue;
                }

                if (user.getRole() != Role.ADMIN && !isCertifacateInUserOrganisation(intermediateCertificate, user)) {
                    continue;
                }

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

        return isUsersCertificate(certificate, user) ||
                isUserInCertificateChain(certificate.getParentCertificate(), user);
    }

    private boolean isUsersCertificate(Certificate certificate, User user) {
        if (certificate == null) return false;

        String email = null;
        X500Name x500Name = certificate.getSubject().getX500Name();

        RDN[] rdns = x500Name.getRDNs(BCStyle.E);
        if (rdns != null && rdns.length > 0) {
            email = IETFUtils.valueToString(rdns[0].getFirst().getValue());
        }
        else {
            rdns = x500Name.getRDNs(BCStyle.EmailAddress);
            if (rdns != null && rdns.length > 0) {
                email = IETFUtils.valueToString(rdns[0].getFirst().getValue());
            }
        }

        return email != null && email.equals(user.getEmail());
    }

    private boolean isCertifacateInUserOrganisation(Certificate certificate, User user) {
        return certificate.getOrganisation().getId().equals(user.getOrganisation().getId());
    }

   @PostMapping("/{id}/revoke")
   public ResponseEntity<?> revokeCertificate(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            String reason = request.get("reason");

            if (reason == null || reason.isEmpty()) {
                return ResponseEntity.badRequest().body("Revocation reason is required");
            }

            int revokedCount = certificateService.revokeCertificate(id, reason);
            if (revokedCount == 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Certificate could not be revoked. It may not exist or is already revoked.");
            }

            return ResponseEntity.ok("Certificate and " + (revokedCount - 1) + " descendant(s) revoked successfully. Total: " + revokedCount);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error revoking certificate: " + e.getMessage());
        }
    }

    @GetMapping("/ca")
    public ResponseEntity<?> getAllCAs(@RequestHeader(value = "Authorization", required = true) String token) {
        try {
            User user = null;
            if(token != null) {
                token = token.substring(7);

                try {
                    user = authService.getUserById(jwtUtil.extractUserId(token));
                }
                catch (Exception ignored) {
                }
            }

            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("User is not authenticated");
            }

            List<Certificate> caCertificates = certificateService.findByCertificateType(CertificateType.ROOT_CA);
            List<Certificate> intermediateCertificates = certificateService.findByCertificateType(CertificateType.INTERMEDIATE_CA);

            List<CAInfoDTO> responseDTO = new ArrayList<>();

            for (Certificate cert : caCertificates) {
                if (cert.getRevocationReason() != null) {
                    continue;
                }

                if (user.getRole() != Role.ADMIN && !isCertifacateInUserOrganisation(cert, user)) {
                    continue;
                }

                responseDTO.add(convertToCAInfoDTO(cert));
            }

            for (Certificate cert : intermediateCertificates) {
                if (user.getRole() != Role.ADMIN && !isCertifacateInUserOrganisation(cert, user)) {
                    continue;
                }

                responseDTO.add(convertToCAInfoDTO(cert));
            }
            return ResponseEntity.ok(responseDTO);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error retrieving CAs: " + e.getMessage());
        }
    }

    private CAInfoDTO convertToCAInfoDTO(Certificate certificate) {
        CAInfoDTO dto = new CAInfoDTO();
        dto.setId(certificate.getId());
        dto.setSerialNumber(certificate.getSerialNumber());
        dto.setCommonName(certificate.getSubject().getCommonName());
        dto.setSubjectDN(certificate.getSubject().getX500NameString());
        dto.setIssuerDN(certificate.getIssuer().getX500NameString());
        dto.setValidFrom(certificate.getStartDate().toString());
        dto.setValidTo(certificate.getEndDate().toString());
        dto.setRevoked(certificate.getRevocationReason() != null);
        dto.setKeystoreFormat(certificate.getKeystoreFormat() == null ? "" : certificate.getKeystoreFormat());
        return dto;
    }

    @GetMapping("/ca/{issuerCAId}/constraints")
    public ResponseEntity<?> getCAConstraints(@PathVariable Long issuerCAId) {
        try {
            IssuerConstraintsDTO constraints = certificateService.getCAConstraints(issuerCAId);

            if (constraints == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("CA certificate or constraints not found for ID: " + issuerCAId);
            }
            return ResponseEntity.ok(constraints);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving CA constraints: " + e.getMessage());
        }
    }

    @GetMapping("/downloads/{filename}")
    public ResponseEntity<Resource> downloadKeystore(@PathVariable String filename) throws IOException {
        Path filePath = Paths.get("/path/to/keystores", filename);
        if (!Files.exists(filePath)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(null);
        }

        ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(filePath));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @GetMapping("/{id}/certificate")
    public ResponseEntity<Resource> downloadCertificateAsPem(@PathVariable Long id) {
        try {
            Optional<Certificate> optionalCertificate = certificateService.findById(id);
            if (optionalCertificate.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(null);
            }

            Certificate certificate = optionalCertificate.get();

            if (certificate.getRevocationReason() != null || certificate.getRevocationDate() != null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(null);
            }

            byte[] certBytes = certificate.getX509Certificate().getEncoded();

            String pemCertificate = "-----BEGIN CERTIFICATE-----\n" +
                    java.util.Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(certBytes) +
                    "\n-----END CERTIFICATE-----\n";

            ByteArrayResource resource = new ByteArrayResource(pemCertificate.getBytes());
            String filename = "certificate_" + certificate.getSerialNumber() + ".pem";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(null);
        }
    }

    /// ///////////////////////

    @GetMapping("/{id}/download-keystore")
    public ResponseEntity<Resource> downloadKeystoreWithFormat(
            @PathVariable Long id,
            @RequestParam String format,
            @RequestParam(required = false, defaultValue = "changeit") String password) {
        try {
            if (!format.equalsIgnoreCase("JKS") && !format.equalsIgnoreCase("PKCS12")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
            }

            Optional<Certificate> optionalCertificate = certificateService.findById(id);
            if (optionalCertificate.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            Certificate certificate = optionalCertificate.get();

            if (certificate.getRevocationReason() != null || certificate.getRevocationDate() != null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
            }

            byte[] keystoreData;
            String filename;
            String extension = format.equalsIgnoreCase("JKS") ? ".jks" : ".p12";

            // Check if this is an autogenerated certificate (has keystoreFormat field)
            if (certificate.getKeystoreFormat() != null &&
                    (certificate.getKeystoreFormat().equals("PKCS12") || certificate.getKeystoreFormat().equals("JKS"))) {

                // AUTOGENERATED: Read from filesystem
                Path keystoreDirectory = Paths.get("private_keys");
                Path originalPath = null;

                // Find the original keystore file
                Path p12Path = keystoreDirectory.resolve("keystore_" + certificate.getId() + ".p12");
                Path jksPath = keystoreDirectory.resolve("keystore_" + certificate.getId() + ".jks");

                if (Files.exists(p12Path)) {
                    originalPath = p12Path;
                } else if (Files.exists(jksPath)) {
                    originalPath = jksPath;
                }

                if (originalPath == null || !Files.exists(originalPath)) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
                }

                // If requested format matches stored format, return as-is
                String storedExtension = originalPath.toString().endsWith(".jks") ? ".jks" : ".p12";
                if (storedExtension.equals(extension)) {
                    keystoreData = Files.readAllBytes(originalPath);
                } else {
                    // Convert from one format to another
                    keystoreData = certificateService.convertKeystoreFormat(
                            Files.readAllBytes(originalPath),
                            certificate.getKeystoreFormat(),
                            format,
                            password
                    );
                }
            } else {
                // NORMAL CERTIFICATE: Generate keystore from Subject's private key
                KeyPair keyPair = new KeyPair(
                        certificate.getSubject().getPublicKey(),
                        certificate.getSubject().getPrivateKey()
                );
                keystoreData = certificateService.generateKeystore(keyPair, certificate, format, password);
            }

            filename = "keystore_" + certificate.getSerialNumber() + extension;

            ByteArrayResource resource = new ByteArrayResource(keystoreData);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }
}
