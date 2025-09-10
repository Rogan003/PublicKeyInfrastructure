package org.example.publickeyinfrastructure.Services;

import java.util.Collections;

import org.example.publickeyinfrastructure.Repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.example.publickeyinfrastructure.Entities.User;
import org.example.publickeyinfrastructure.Entities.RegularUser;

@Service
public class UsersDetailsService implements UserDetailsService {

  @Autowired
  private final UserRepository userRepository;

  public UsersDetailsService(UserRepository repository) {
    this.userRepository = repository;
  }

  
  @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Pronađi korisnika u bazi
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
        
        // Kreiraj Spring Security UserDetails objekat
        return org.springframework.security.core.userdetails.User.builder()
            .username(user.getEmail())           // Email kao username
            .password(user.getPassword())        // Enkodiran password
            .authorities(Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().toString())
            ))
            .accountExpired(false)              // Account nije istekao
            .accountLocked(false)               // Account nije blokiran
            .credentialsExpired(false)          // Credentials nisu istekli
            .disabled(!isUserEnabled(user))       // Da li je korisnik aktivan
            .build();
    }
    
    private boolean isUserEnabled(User user) {
        // Proveri da li je korisnik aktivan
        if (user instanceof RegularUser) {
            return ((RegularUser) user).isEnabled();
        }
        return true; // Admin korisnici su uvek aktivni
    }
}
