package org.example.publickeyinfrastructure.Controllers;

import javafx.util.Pair;
import org.example.publickeyinfrastructure.DTOs.Csr.*;
import org.example.publickeyinfrastructure.DTOs.Infrastructure.CreateCertificateDTO;
import org.example.publickeyinfrastructure.DTOs.Infrastructure.SubjectDTO;
import org.example.publickeyinfrastructure.Entities.Infrastructure.Certificate;
import org.example.publickeyinfrastructure.Services.Auth.AuthService;
import org.example.publickeyinfrastructure.Services.Infrastucture.CertificateService;
import org.example.publickeyinfrastructure.Services.Infrastucture.CsrService;
import org.example.publickeyinfrastructure.Services.Infrastucture.SubjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/csr")
@CrossOrigin
public class CsrController {
    @Autowired
    private CsrService csrService;
    @Autowired
    private CertificateService certificateService;
    @Autowired
    private AuthService authService;
    @Autowired
    private SubjectService subjectService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadCsr(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("CSR file cannot be empty");
        }

        try {
            String content = new String(file.getBytes());

            CsrDTO csrDetails = csrService.processCsr(content);
            return ResponseEntity.ok(csrDetails);

        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error reading CSR file: " + ex.getMessage());

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid CSR file: " + ex.getMessage());
        }
    }

    @PostMapping("/parse")
    public ResponseEntity<?> parseCsr(@RequestBody CsrRequest request) {
        try {
            String csrPem = request.getCsrPem();

            CsrDTO csrDTO = csrService.processCsr(csrPem);
            return ResponseEntity.ok(csrDTO);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error processing CSR: " + e.getMessage());
        }
    }

    @PostMapping("/issue")
    public ResponseEntity<?> issueFromCsr(@RequestBody CRSUploadRequest request) {
        try {
            String csrPem = request.getCsrPem();
            String issuerCAId = request.getIssuerCAId();
            String notBefore = request.getNotBefore();
            String notAfter = request.getNotAfter();

            if (csrPem == null || csrPem.isBlank()) {
                return ResponseEntity.badRequest().body("CSR content is missing or empty.");
            }

            // date validation
            if (notBefore == null || notAfter == null || notBefore.isBlank() || notAfter.isBlank()) {
                return ResponseEntity.badRequest().body("Certificate validity period is required.");
            }
            Date validityStart = java.sql.Date.valueOf(notBefore);
            Date validityEnd = java.sql.Date.valueOf(notAfter);
            if (validityStart.after(validityEnd)) {
                return ResponseEntity.badRequest().body("Invalid validity period: NotBefore must be before NotAfter.");
            }

            CsrDTO csrDetails = csrService.processCsr(csrPem);

            Optional<Certificate> optionalIssuerCertificate = certificateService.findById(Long.valueOf(issuerCAId));
            if (!optionalIssuerCertificate.isPresent()) {
                return ResponseEntity.badRequest().body("CA certificate with the provided ID does not exist.");
            }
            Certificate issuerCertificate = optionalIssuerCertificate.get();

            // revocation validation
            if (issuerCertificate.getRevocationReason() != null || issuerCertificate.getRevocationDate() != null) {
                return ResponseEntity.badRequest().body("The selected CA certificate is revoked.");
            }
            if (validityStart.before(issuerCertificate.getStartDate()) || validityEnd.after(issuerCertificate.getEndDate())) {
                return ResponseEntity.badRequest().body("The requested validity period exceeds the limits of the selected CA certificate.");
            }

            SubjectDTO subjectDTO = new SubjectDTO();

            subjectDTO.setCommonName(csrDetails.getCommonName());
            subjectDTO.setGivenName(csrDetails.getGivenName());
            subjectDTO.setSurname(csrDetails.getSurname());
            subjectDTO.setOrganizationName(csrDetails.getOrganization());
            subjectDTO.setCountry(csrDetails.getCountry());
            subjectDTO.setEmail(csrDetails.getEmail());
            String organizationUnit = csrDetails.getOrganizationUnit();
            subjectDTO.setOrganizationUnit((organizationUnit != null && !organizationUnit.isBlank()) ? organizationUnit : "Default Unit");
            // mocked extensions for now
            List<String> extensions = List.of("BASIC_END", "KEY_USAGE", "EXT_KEY_SERVER");

            Long ownerId = authService.getCurrentUserId();

            Certificate issuedCertificate = certificateService.createEndEntityCertificate(
                new CreateCertificateDTO(
                    issuerCertificate.getId(),
                    subjectDTO,
                    validityStart,
                    validityEnd,
                    extensions,
                    ownerId
                )
            );

            IssueCertificateResponseDTO response = certificateService.convertToCertificateResponseDTO(issuedCertificate);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing CSR: " + e.getMessage());
        }
    }

    @PostMapping("/issue-autogen")
    public ResponseEntity<?> issueAutogenerated(@RequestBody CSRAutogenRequest request) {
        try {
            String issuerCAId = request.getIssuerCAId();
            String notBefore = request.getNotBefore();
            String notAfter = request.getNotAfter();
            String keystoreFormat = request.getKeystoreFormat();
            String keystorePassword = request.getKeystorePassword();

            if (issuerCAId == null || issuerCAId.isBlank()) {
                return ResponseEntity.badRequest().body("Issuer CA ID is required.");
            }
            if (notBefore == null || notAfter == null || notBefore.isBlank() || notAfter.isBlank()) {
                return ResponseEntity.badRequest().body("Certificate validity period is required.");
            }
            if (keystorePassword == null || keystorePassword.isBlank()) {
                return ResponseEntity.badRequest().body("Keystore password is required.");
            }
            if (!List.of("JKS", "PKCS12").contains(keystoreFormat)) {
                return ResponseEntity.badRequest().body("Invalid keystore format. Allowed values are JKS or PKCS12.");
            }

            if (request.getSubjectCN() == null || request.getSubjectO() == null || request.getSubjectE() == null) {
                return ResponseEntity.badRequest().body("Subject data is incomplete. CN, O, and E are required.");
            }
            SubjectDTO subjectDTO = new SubjectDTO();
            subjectDTO.setCommonName(request.getSubjectCN());
            subjectDTO.setOrganizationName(request.getSubjectO());
            subjectDTO.setOrganizationUnit(request.getSubjectOU() != null && !request.getSubjectOU().isBlank() ? request.getSubjectOU() : "Default Unit");
            subjectDTO.setCountry(request.getSubjectC());
            subjectDTO.setEmail(request.getSubjectE());

            Date validityStart = java.sql.Date.valueOf(notBefore);
            Date validityEnd = java.sql.Date.valueOf(notAfter);
            if (validityStart.after(validityEnd)) {
                return ResponseEntity.badRequest().body("Invalid validity period: NotBefore must be before NotAfter.");
            }

            Optional<Certificate> optionalIssuerCertificate = certificateService.findById(Long.valueOf(issuerCAId));
            if (optionalIssuerCertificate.isEmpty()) {
                return ResponseEntity.badRequest().body("CA certificate with the provided ID does not exist.");
            }
            Certificate issuerCertificate = optionalIssuerCertificate.get();

            if (issuerCertificate.getRevocationReason() != null || issuerCertificate.getRevocationDate() != null) {
                return ResponseEntity.badRequest().body("The selected CA certificate is revoked.");
            }
            if (validityStart.before(issuerCertificate.getStartDate()) || validityEnd.after(issuerCertificate.getEndDate())) {
                return ResponseEntity.badRequest().body("The requested validity period exceeds the limits of the selected CA certificate.");
            }

            List<String> extensions = List.of("BASIC_END", "KEY_USAGE", "EXT_KEY_SERVER");
            Long ownerId = authService.getCurrentUserId();

            Pair<Certificate, KeyPair> cr = certificateService.createEndEntityCertificateCSR(
                new CreateCertificateDTO(
                    issuerCertificate.getId(),
                    subjectDTO,
                    validityStart,
                    validityEnd,
                    extensions,
                    ownerId
                ), keystoreFormat
            );

            if (cr.getKey() == null || cr.getValue() == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to create the certificate. Please check inputs and try again.");
            }
            Certificate issuedCertificate = cr.getKey();
            KeyPair keyPair = cr.getValue();

            byte[] keystoreData;
            try {
                keystoreData = certificateService.generateKeystore(keyPair, issuedCertificate, keystoreFormat, keystorePassword);
                saveKeystore(keystoreData, issuedCertificate.getId(), keystoreFormat);
                IssueCertificateResponseDTO response = certificateService.convertToCertificateResponseDTO(issuedCertificate);
                return ResponseEntity.ok(response);

            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to generate keystore: " + e.getMessage());
            }

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Autogenerated certificate issuance failed: " + e.getMessage());
        }
    }

    private void saveKeystore(byte[] keystoreData, Long certificateId, String keystoreFormat) throws IOException {
        try {
            Path directoryPath = Paths.get("private_keys");
            if (!Files.exists(directoryPath)) {
                Files.createDirectories(directoryPath);
            }

            // Use appropriate extension based on format
            String extension = keystoreFormat.equalsIgnoreCase("JKS") ? ".jks" : ".p12";
            String filename = "keystore_" + certificateId + extension;
            Path filePath = directoryPath.resolve(filename);

            Files.write(filePath, keystoreData);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/{certificateId}/download")
    public ResponseEntity<?> downloadCertificate(@PathVariable Long certificateId) {
        try {
            Certificate certificate = certificateService.findById(certificateId).orElseThrow(() -> new RuntimeException("Certificate not found"));

            String pemContent = certificateService.toPem(certificate);
            ByteArrayResource resource = new ByteArrayResource(
                    pemContent.getBytes(StandardCharsets.UTF_8)
            );

            String fileName = "certificate_" + certificateId + ".pem";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .contentType(MediaType.parseMediaType("application/x-pem-file"))
                    .contentLength(resource.contentLength())
                    .body(resource);

        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Download private key as keystore (.p12 or .jks)
    @GetMapping("/{certificateId}/download-keystore")
    public ResponseEntity<?> downloadKeystore(@PathVariable Long certificateId) {
        try {
            Path keystoreDirectory = Paths.get("private_keys");

            Path filePath = null;
            String fileName = null;

            // check for PKCS12 format
            Path p12Path = keystoreDirectory.resolve("keystore_" + certificateId + ".p12");
            if (Files.exists(p12Path)) {
                filePath = p12Path;
                fileName = "keystore_" + certificateId + ".p12";

            } else {
            // check for JKS format
                Path jksPath = keystoreDirectory.resolve("keystore_" + certificateId + ".jks");
                if (Files.exists(jksPath)) {
                    filePath = jksPath;
                    fileName = "keystore_" + certificateId + ".jks";
                }
            }

            if (filePath == null || !Files.exists(filePath)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(filePath));

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(resource.contentLength())
                    .body(resource);

        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}