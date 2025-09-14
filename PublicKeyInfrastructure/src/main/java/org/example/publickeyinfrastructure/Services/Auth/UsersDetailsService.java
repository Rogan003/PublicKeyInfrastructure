package org.example.publickeyinfrastructure.Services.Auth;

import java.util.Collections;

import org.example.publickeyinfrastructure.Entities.User.User;
import org.example.publickeyinfrastructure.Entities.User.RegularUser;
import org.example.publickeyinfrastructure.Repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@Service
public class UsersDetailsService implements UserDetailsService {

  @Autowired
  private final UserRepository userRepository;

  public UsersDetailsService(UserRepository repository) {
    this.userRepository = repository;
  }

  
  @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
        
        return org.springframework.security.core.userdetails.User.builder()
            .username(user.getEmail())          
            .password(user.getPassword())        
            .authorities(Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().toString())
            ))
            .accountExpired(false)              
            .accountLocked(false)               
            .credentialsExpired(false)          
            .disabled(!isUserEnabled(user))       
            .build();
    }
    
    private boolean isUserEnabled(User user) {
        if (user instanceof RegularUser) {
            return ((RegularUser) user).isEnabled();
        }
        return true; 
    }
}
