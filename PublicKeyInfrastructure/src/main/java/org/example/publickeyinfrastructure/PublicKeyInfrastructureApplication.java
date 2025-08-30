package org.example.publickeyinfrastructure;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class PublicKeyInfrastructureApplication {

    public static void main(String[] args) {
        SpringApplication.run(PublicKeyInfrastructureApplication.class, args);
    }

}
