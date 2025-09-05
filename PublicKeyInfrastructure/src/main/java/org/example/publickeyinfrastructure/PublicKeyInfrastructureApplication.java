package org.example.publickeyinfrastructure;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PublicKeyInfrastructureApplication {

    public static void main(String[] args) {
        SpringApplication.run(PublicKeyInfrastructureApplication.class, args);
    }

}
