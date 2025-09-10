package org.example.publickeyinfrastructure.Repositories;

import org.example.publickeyinfrastructure.Entities.Infrastructure.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {
  
  @Query("SELECT s FROM Subject s WHERE s.x500NameString = :x500Name")
  Optional<Subject> findByX500Name(String x500Name);
  
  @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Subject s WHERE s.x500NameString = :x500Name")
  boolean existsByX500Name(String x500Name);
  
}
