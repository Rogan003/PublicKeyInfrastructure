package org.example.publickeyinfrastructure.Controllers;

import org.example.publickeyinfrastructure.DTOs.Csr.*;
import org.example.publickeyinfrastructure.DTOs.Infrastructure.CertificateDTO;
import org.example.publickeyinfrastructure.DTOs.Infrastructure.CreateCertificateDTO;
import org.example.publickeyinfrastructure.DTOs.Infrastructure.SubjectDTO;
import org.example.publickeyinfrastructure.Entities.Infrastructure.Certificate;
import org.example.publickeyinfrastructure.Services.Infrastucture.CertificateService;
import org.example.publickeyinfrastructure.Services.Infrastucture.CsrService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
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

    @PostMapping("/upload")
    public ResponseEntity<?> uploadCsr(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("CSR file cannot be empty");
        }

        try {
            String content = new String(file.getBytes());
            System.out.println("File content: \n" + content);

            CsrDTO csrDetails = csrService.processCsr(content);
            return ResponseEntity.ok(csrDetails);

        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error reading CSR file: " + ex.getMessage());

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Invalid CSR file: " + ex.getMessage());
        }
    }

    @PostMapping("/parse")
    public ResponseEntity<?> parseCsr(@RequestBody CsrRequest request
    ) {
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
            // Extract CSR payload data
            String csrPem = request.getCsrPem();
            String issuerCAId = request.getIssuerCAId();
            String notBefore = request.getNotBefore();
            String notAfter = request.getNotAfter();

            if (csrPem == null || csrPem.isBlank()) {
                return ResponseEntity.badRequest().body("CSR content is missing or empty.");
            }

            // Validate Date Range (if required)
            if (notBefore == null || notAfter == null || notBefore.isBlank() || notAfter.isBlank()) {
                return ResponseEntity.badRequest().body("Certificate validity period is required.");
            }
            Date validityStart = java.sql.Date.valueOf(notBefore);
            Date validityEnd = java.sql.Date.valueOf(notAfter);
            if (validityStart.after(validityEnd)) {
                return ResponseEntity.badRequest().body("Invalid validity period: NotBefore must be before NotAfter.");
            }

            // Process the CSR and parse subject, public key
            CsrDTO csrDetails = csrService.processCsr(csrPem);

            // Locate the Issuer CA certificate using the provided issuerCAId
            Optional<Certificate> optionalIssuerCertificate = certificateService.findById(Long.valueOf(issuerCAId));
            if (!optionalIssuerCertificate.isPresent()) {
                return ResponseEntity.badRequest().body("CA certificate with the provided ID does not exist.");
            }
            Certificate issuerCertificate = optionalIssuerCertificate.get();

            // Validate the CA Certificate (e.g., ensure it is not revoked and is within valid date range)
            if (issuerCertificate.getRevocationReason() != null || issuerCertificate.getRevocationDate() != null) {
                return ResponseEntity.badRequest().body("The selected CA certificate is revoked.");
            }
            if (validityStart.before(issuerCertificate.getStartDate()) || validityEnd.after(issuerCertificate.getEndDate())) {
                return ResponseEntity.badRequest().body("The requested validity period exceeds the limits of the selected CA certificate.");
            }

            // Convert issuer Certificate to CertificateDTO
            CertificateDTO issuerCertificateDTO = certificateService.convertToDTO(issuerCertificate);

            // Convert the subject from CSR into a SubjectDTO
            SubjectDTO subjectDTO = new SubjectDTO();
            subjectDTO.setCommonName(csrDetails.getSubject());
            subjectDTO.setEmail(csrDetails.getSubject()); // Add any extra attributes parsed from CSR details as needed

            // Prepare extensions (mocked for now, adjust dynamically if needed)
            List<String> extensions = List.of("BASIC_END", "KEY_USAGE", "EXT_KEY_SERVER");

            // Generate the end-entity certificate using the certificate service
            Certificate issuedCertificate = certificateService.createEndEntityCertificate(
                    new CreateCertificateDTO(
                            issuerCertificateDTO,  // Issuer Certificate DTO
                            subjectDTO,            // Subject DTO from CSR
                            validityStart,         // NotBefore
                            validityEnd,           // NotAfter
                            extensions             // Extensions
                    )
            );

            // Convert the issued Certificate object to its DTO for the response
            CertificateDTO issuedCertificateDTO = certificateService.convertToDTO(issuedCertificate);

            // Return created certificate details
            return ResponseEntity.ok(issuedCertificateDTO);


        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing CSR: " + e.getMessage());
        }
    }

    @PostMapping("/issue-autogen")
    public ResponseEntity<?> issueAutogenerated(@RequestBody CSRAutogenRequest request) {
        try {
            // Extract input details
            String issuerCAId = request.getIssuerCAId();
            String notBefore = request.getNotBefore();
            String notAfter = request.getNotAfter();
            SubjectAutogenDTO subject = request.getSubject();
            String keystoreFormat = request.getKeystoreFormat();
            String keystorePassword = request.getKeystorePassword();

            // Validate mandatory fields
            if (issuerCAId == null || issuerCAId.isBlank()) {
                return ResponseEntity.badRequest().body("Issuer CA ID is required.");
            }
            if (notBefore == null || notAfter == null || notBefore.isBlank() || notAfter.isBlank()) {
                return ResponseEntity.badRequest().body("Certificate validity period is required.");
            }
            if (subject == null || subject.getCN() == null || subject.getO() == null || subject.getE() == null) {
                return ResponseEntity.badRequest().body("Subject data is incomplete. CN, O, and E are required.");
            }
            if (keystorePassword == null || keystorePassword.isBlank()) {
                return ResponseEntity.badRequest().body("Keystore password is required.");
            }

            Date validityStart = java.sql.Date.valueOf(notBefore);
            Date validityEnd = java.sql.Date.valueOf(notAfter);
            if (validityStart.after(validityEnd)) {
                return ResponseEntity.badRequest().body("Invalid validity period: NotBefore must be before NotAfter.");
            }

            // Locate the issuer certificate
            Optional<Certificate> optionalIssuerCertificate = certificateService.findById(Long.valueOf(issuerCAId));
            if (!optionalIssuerCertificate.isPresent()) {
                return ResponseEntity.badRequest().body("CA certificate with the provided ID does not exist.");
            }
            Certificate issuerCertificate = optionalIssuerCertificate.get();

            // Validate the issuer certificate (e.g., revocation, validity)
            if (issuerCertificate.getRevocationReason() != null || issuerCertificate.getRevocationDate() != null) {
                return ResponseEntity.badRequest().body("The selected CA certificate is revoked.");
            }
            if (validityStart.before(issuerCertificate.getStartDate()) || validityEnd.after(issuerCertificate.getEndDate())) {
                return ResponseEntity.badRequest().body("The requested validity period exceeds the limits of the selected CA certificate.");
            }

            // Convert issuer certificate to DTO
            CertificateDTO issuerCertificateDTO = certificateService.convertToDTO(issuerCertificate);

            // Generate initial key pair
            KeyPair keyPair = certificateService.generateKeyPair();

            // Prepare the SubjectDTO with provided details
            SubjectDTO subjectDTO = new SubjectDTO();
            subjectDTO.setCommonName(subject.getCN());
            subjectDTO.setOrganization(subject.getO());
            subjectDTO.setOrganization(subject.getOU());
            subjectDTO.setCountry(subject.getC());
            subjectDTO.setEmail(subject.getE());
            subjectDTO.setX500Name(constructX500Name(subject)); // Generate full X500 Name

            // Prepare certificate extensions (fixed here, adjust dynamically as needed)
            List<String> extensions = List.of("BASIC_END", "KEY_USAGE", "EXT_KEY_SERVER");

            // Create the certificate with the generated data
            Certificate issuedCertificate = certificateService.createEndEntityCertificate(
                    new CreateCertificateDTO(
                            issuerCertificateDTO,  // Issuer CA certificate
                            subjectDTO,            // Subject for the certificate
                            validityStart,         // Start date
                            validityEnd,           // End date
                            extensions             // Supported extensions
                    )
            );

            // Convert the issued certificate to a DTO
            CertificateDTO issuedCertificateDTO = certificateService.convertToDTO(issuedCertificate);

            // Generate the keystore (e.g., JKS or PKCS12)
            byte[] keystoreData = certificateService.generateKeystore(keyPair, issuedCertificate, keystoreFormat, keystorePassword);

            // Create a response object
            IssueAutogenResponse response = new IssueAutogenResponse();
            response.setCertificate(issuedCertificateDTO);
            response.setKeystoreDownloadUrl("mocked-url-for-keystore/" + issuedCertificate.getId()); // Replace with the actual URL logic

            // Send response
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Autogenerated certificate issuance failed: " + e.getMessage());
        }
    }

    private String constructX500Name(SubjectAutogenDTO subject) {
        StringBuilder x500NameBuilder = new StringBuilder();

        if (subject.getCN() != null) {
            x500NameBuilder.append("CN=").append(subject.getCN()).append(", ");
        }
        if (subject.getO() != null) {
            x500NameBuilder.append("O=").append(subject.getO()).append(", ");
        }
        if (subject.getOU() != null) {
            x500NameBuilder.append("OU=").append(subject.getOU()).append(", ");
        }
        if (subject.getC() != null) {
            x500NameBuilder.append("C=").append(subject.getC()).append(", ");
        }
        if (subject.getE() != null) {
            x500NameBuilder.append("E=").append(subject.getE()).append(", ");
        }

        // Remove the trailing comma and space
        if (x500NameBuilder.length() > 0) {
            x500NameBuilder.setLength(x500NameBuilder.length() - 2);
        }

        return x500NameBuilder.toString();
    }
}