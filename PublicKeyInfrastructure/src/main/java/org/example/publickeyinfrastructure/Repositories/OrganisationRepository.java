package org.example.publickeyinfrastructure.Repositories;

import org.example.publickeyinfrastructure.Entities.Infrastructure.Organisation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrganisationRepository extends JpaRepository<Organisation, Long> {
    
    Optional<Organisation> findByName(String name);
    
    boolean existsByName(String name);
}
