package org.example.publickeyinfrastructure.Entities.Infrastructure;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Lob;
import jakarta.persistence.Column;
import jakarta.persistence.Transient;
import org.bouncycastle.asn1.x500.X500Name;

import java.security.PublicKey;

@Entity
@Table(name = "subjects")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Subject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Lob
    @Column(name = "public_key")
    private byte[] publicKeyBytes;
    
    @Column(name = "x500_name", length = 1000)
    private String x500NameString;
    
    @Transient
    private PublicKey publicKey;
    
    @Transient
    private X500Name x500Name;

    public Subject(PublicKey publicKey, X500Name x500Name) {
        this.publicKey = publicKey;
        this.x500Name = x500Name;
        this.x500NameString = x500Name.toString();
        
        // Konverzija ključa u byte array za čuvanje u bazi
        try {
            this.publicKeyBytes = publicKey.getEncoded();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

