package org.example.publickeyinfrastructure.Entities.EmailVerification;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmailVerificationResult {

  private boolean success;
  private String message;

  public EmailVerificationResult(boolean success, String message) {
    this.success = success;
    this.message = message;
  }
  
  public static EmailVerificationResult success(String message) {
    return new EmailVerificationResult(true, message);
  }

  public static EmailVerificationResult failure(String message) {
    return new EmailVerificationResult(false, message);
  }

}
