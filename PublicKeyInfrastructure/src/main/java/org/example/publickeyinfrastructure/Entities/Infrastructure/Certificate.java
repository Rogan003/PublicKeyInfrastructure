package org.example.publickeyinfrastructure.Entities.Infrastructure;

import java.security.cert.X509Certificate;
import java.util.Date;

import org.example.publickeyinfrastructure.Entities.Enums.CertificateType;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.Column;
import jakarta.persistence.Transient;

@Entity
@Table(name = "certificates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Certificate {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  
  @ManyToOne
  @JoinColumn(name = "subject_id")
  private Subject subject;
  
  @ManyToOne
  @JoinColumn(name = "issuer_id")
  private Issuer issuer;
  
  @Column(name = "serial_number", unique = true)
  private String serialNumber;
  
  @Column(name = "start_date")
  private Date startDate;
  
  @Column(name = "end_date")
  private Date endDate;
  
  @Column(name = "certificate_type")
  private CertificateType certificateType;
  
  @Lob
  @Column(name = "x509_certificate")
  private byte[] x509CertificateBytes;
  
  @Transient
  private X509Certificate x509Certificate;

  public Certificate(Subject subject, Issuer issuer, String serialNumber, 
                    Date startDate, Date endDate, X509Certificate x509Certificate, CertificateType certificateType) {
      this.subject = subject;
      this.issuer = issuer;
      this.serialNumber = serialNumber;
      this.startDate = startDate;
      this.endDate = endDate;
      this.x509Certificate = x509Certificate;
      this.certificateType = certificateType;
      
      // Konverzija sertifikata u byte array za čuvanje u bazi
      try {
          this.x509CertificateBytes = x509Certificate.getEncoded();
      } catch (Exception e) {
          e.printStackTrace();
      }
  }
}
