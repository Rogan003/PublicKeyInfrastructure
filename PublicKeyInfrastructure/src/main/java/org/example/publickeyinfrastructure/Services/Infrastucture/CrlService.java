package org.example.publickeyinfrastructure.Services.Infrastucture;

import org.example.publickeyinfrastructure.Entities.Infrastructure.Certificate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CrlService {
    private final List<Certificate> revokedCertificates = new ArrayList<>();

    public void addCertificateToCRL(Certificate certificate) {
        if (certificate.getRevocationReason() != null && certificate.getRevocationDate() != null) {
            revokedCertificates.add(certificate);
        }
    }

    public List<Certificate> getCRL() {
        return new ArrayList<>(revokedCertificates);
    }
}