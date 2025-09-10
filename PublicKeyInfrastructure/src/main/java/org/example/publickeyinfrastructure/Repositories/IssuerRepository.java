package org.example.publickeyinfrastructure.Repositories;

import org.example.publickeyinfrastructure.Entities.Infrastructure.Issuer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

@Repository
public interface IssuerRepository extends JpaRepository<Issuer, Long> {
  
  @Query("SELECT i FROM Issuer i WHERE i.x500NameString = :x500Name")
  Optional<Issuer> findByX500Name(String x500Name);
    
  @Query("SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END FROM Issuer i WHERE i.x500NameString = :x500Name")
  boolean existsByX500Name(String x500Name);
  
}
