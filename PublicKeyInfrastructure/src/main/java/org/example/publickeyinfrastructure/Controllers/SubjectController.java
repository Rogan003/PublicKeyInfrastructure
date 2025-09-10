package org.example.publickeyinfrastructure.Controllers;

import org.example.publickeyinfrastructure.DTOs.Infrastructure.SubjectDTO;
import org.example.publickeyinfrastructure.Entities.Infrastructure.Subject;
import org.example.publickeyinfrastructure.Services.Infrastucture.SubjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/subjects")
@CrossOrigin(origins = "*")
public class SubjectController {

    @Autowired
    private SubjectService subjectService;

    @PostMapping
    public ResponseEntity<?> createSubject(@RequestBody SubjectDTO subjectDTO) {
        try {
            Subject createdSubject = subjectService.createSubject(subjectDTO);
            if (createdSubject != null) {
                SubjectDTO responseDTO = subjectService.convertToDTO(createdSubject);
                return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Failed to create subject");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error creating subject: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getSubjectById(@PathVariable Long id) {
        Optional<Subject> subject = subjectService.getSubjectById(id);
        if (subject.isPresent()) {
            SubjectDTO responseDTO = subjectService.convertToDTO(subject.get());
            return ResponseEntity.ok(responseDTO);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("Subject not found with id: " + id);
        }
    }

    @GetMapping("/x500/{x500Name}")
    public ResponseEntity<?> getSubjectByX500Name(@PathVariable String x500Name) {
        Optional<Subject> subject = subjectService.getSubjectByX500Name(x500Name);
        if (subject.isPresent()) {
            SubjectDTO responseDTO = subjectService.convertToDTO(subject.get());
            return ResponseEntity.ok(responseDTO);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("Subject not found with X500Name: " + x500Name);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateSubject(@PathVariable Long id, @RequestBody SubjectDTO subjectDTO) {
        try {
            Subject updatedSubject = subjectService.updateSubject(id, subjectDTO);
            if (updatedSubject != null) {
                SubjectDTO responseDTO = subjectService.convertToDTO(updatedSubject);
                return ResponseEntity.ok(responseDTO);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Subject not found with id: " + id);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error updating subject: " + e.getMessage());
        }
    }

    @GetMapping("/exists")
    public ResponseEntity<?> checkSubjectExists(@RequestParam String x500Name) {
        try {
            boolean exists = subjectService.existsByX500Name(x500Name);
            return ResponseEntity.ok(exists);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error checking subject existence: " + e.getMessage());
        }
    }
}
