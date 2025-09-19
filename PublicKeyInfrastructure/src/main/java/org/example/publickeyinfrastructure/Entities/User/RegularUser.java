package org.example.publickeyinfrastructure.Entities.User;

import jakarta.persistence.*;

import org.example.publickeyinfrastructure.Entities.Infrastructure.Organisation;

import org.hibernate.annotations.CreationTimestamp;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Getter 
@Setter
public class RegularUser extends User {
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    private String surname;

    @ManyToOne
    @JoinColumn(name = "organisation_id")
    private Organisation organisation;
        
    @Column(nullable = false)
    private boolean enabled = false;

    @CreationTimestamp
    @Column(nullable = true, updatable = false)
    private LocalDateTime createdAt;
    
    public RegularUser() {
        super();
    }
    
    public RegularUser(String email, String password, String name, String surname, Organisation organisation) {
        super();
        this.setEmail(email);
        this.setPassword(password);
        this.name = name;
        this.setRole(Role.REGULAR_USER);
        this.surname = surname;
        this.organisation = organisation;
        this.createdAt = LocalDateTime.now();
    }

    public static RegularUser caUser(String email, String password, String name, String surname, Organisation organisation) {
       RegularUser regularUser = new RegularUser(email, password, name, surname, organisation);
       regularUser.setRole(Role.CERTIFICATE_AUTHORITY);
       regularUser.setEnabled(true);
       return regularUser;
    }
}