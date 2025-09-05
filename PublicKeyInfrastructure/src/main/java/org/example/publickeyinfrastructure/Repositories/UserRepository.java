package org.example.publickeyinfrastructure.Repositories;

import org.example.publickeyinfrastructure.Entities.User;
import org.example.publickeyinfrastructure.Entities.RegularUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
  
  @Query("SELECT u FROM User u WHERE u.email = :email")
  Optional<User> findByEmail(String email);

  @Query("SELECT u FROM User u WHERE u.email = :email")
  Optional<RegularUser> findRegularByEmail(String email);
    
  @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.email = :email")
  boolean existsByEmail(String email);
}
