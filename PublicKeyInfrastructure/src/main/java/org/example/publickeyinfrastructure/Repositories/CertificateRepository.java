package org.example.publickeyinfrastructure.Repositories;

import org.example.publickeyinfrastructure.Entities.Infrastructure.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.example.publickeyinfrastructure.Entities.Enums.CertificateType;

import java.util.List;
import java.util.Optional;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, Long> {
  
  @Query("SELECT c FROM Certificate c WHERE c.serialNumber = :serialNumber")
  Optional<Certificate> findBySerialNumber(String serialNumber);
  
  @Query("SELECT c FROM Certificate c WHERE c.subject.x500NameString = :subjectDN")
  List<Certificate> findBySubjectDN(String subjectDN);
  
  @Query("SELECT c FROM Certificate c WHERE c.issuer.x500NameString = :issuerDN")
  List<Certificate> findByIssuerDN(String issuerDN);
  
  @Query("SELECT c FROM Certificate c WHERE c.certificateType = :certificateType")
  List<Certificate> findByCertificateType(CertificateType certificateType);
  
  
}
