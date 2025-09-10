package org.example.publickeyinfrastructure.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.context.annotation.Primary;


@Configuration
public class PasswordConfig {
  @Bean
    @Primary
    public PasswordEncoder passwordEncoder() {
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }
    
    // Ili sa custom parametrima za jaču bezbednost
    @Bean("strongArgon2Encoder")
    public PasswordEncoder strongPasswordEncoder() {
        return new Argon2PasswordEncoder(
            16,    // saltLength - dužina salt-a (bajtovi)
            32,    // hashLength - dužina hash-a (bajtovi)  
            8,     // parallelism - broj paralelnih thread-ova
            65536, // memory - memorija u KB (64MB)
            10     // iterations - broj iteracija
        );
    }
    
    // Za visoke bezbednosne zahteve
    @Bean("highSecurityEncoder") 
    public PasswordEncoder highSecurityEncoder() {
        return new Argon2PasswordEncoder(
            32,     // saltLength
            64,     // hashLength
            16,     // parallelism
            131072, // memory (128MB)
            15      // iterations
        );
    }
}
