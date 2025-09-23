package org.example.publickeyinfrastructure.Controllers;

import org.example.publickeyinfrastructure.Services.Infrastucture.CrlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/crl")
public class CrlController {
    @Autowired
    private CrlService crlService;

    @GetMapping
    public ResponseEntity<?> getCRL() {
        return ResponseEntity.ok(crlService.getCRL());
    }
}