package org.example.publickeyinfrastructure.Entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter 
@Setter
public class RegularUser extends User {
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    private String surname;
    
    @Column(nullable = false)
    private String organization;
    
    @Column(nullable = false)
    private boolean enabled = false;
    
    // Default constructor required by Hibernate
    public RegularUser() {
        super();
    }
    
    public RegularUser(String email, String password, String name, String surname, String organization) {
        super();
        this.setEmail(email);
        this.setPassword(password);
        this.name = name;
        this.setRole(Role.REGULAR_USER);
        this.surname = surname;
        this.organization = organization;
    }
}