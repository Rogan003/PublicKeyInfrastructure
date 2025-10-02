package org.example.publickeyinfrastructure.Entities.Infrastructure;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.bouncycastle.asn1.x500.X500Name;
import org.example.publickeyinfrastructure.Utils.KeyUtil;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;

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

    @Column(name = "public_key")
    private byte[] publicKeyBytes;

    @Column(name = "private_key")
    private byte[] privateKeyBytes;
    
    @Column(name = "x500_name")
    private String x500NameString;
    
    @Transient
    private PublicKey publicKey;

    @Transient
    private PrivateKey privateKey;
    
    @Transient
    private X500Name x500Name;

    @ManyToOne
    @JoinColumn(name = "organisation_id")
    private Organisation organisation;

    @OneToOne
    @JoinColumn(name = "issuer_version_id")
    private Issuer issuerVersion;

    public Subject(PrivateKey privateKey, PublicKey publicKey, X500Name x500Name, Organisation organisation, Issuer issuer) {
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        this.x500Name = x500Name;
        this.x500NameString = x500Name.toString();
        this.organisation = organisation;
        this.issuerVersion = issuer;
        
        // Konverzija ključa u byte array za čuvanje u bazi
        try {
            this.privateKeyBytes = KeyUtil.encryptPrivateKey(privateKey.getEncoded(), this.organisation.getKeyBytes());
            this.publicKeyBytes = publicKey.getEncoded();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public X500Name getX500Name() {
        if (x500Name == null && x500NameString != null) {
            x500Name = new X500Name(x500NameString);
        }
        return x500Name;
    }

    public String getCommonName() {
        if (this.x500Name != null) {
            return x500Name.getRDNs(org.bouncycastle.asn1.x500.style.BCStyle.CN).length > 0
                    ? x500Name.getRDNs(org.bouncycastle.asn1.x500.style.BCStyle.CN)[0].getFirst().getValue().toString()
                    : null;

        } else if (this.x500NameString != null) {
            String[] attributes = this.x500NameString.split(", ");
            for (String attribute : attributes) {
                if (attribute.startsWith("CN=")) {
                    return attribute.substring(3);
                }
            }
        }

        return null;
    }

    public PrivateKey getPrivateKey() {
        try {
            byte[] decryptedKeyBytes = KeyUtil.decryptPrivateKey(this.privateKeyBytes, this.organisation.getKeyBytes());
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decryptedKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePrivate(keySpec);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}

