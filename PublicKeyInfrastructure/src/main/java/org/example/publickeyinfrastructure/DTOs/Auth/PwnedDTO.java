package org.example.publickeyinfrastructure.DTOs.Auth;

import lombok.Data;

@Data
public class PwnedDTO {
  private boolean pwned;
  private int breachCount;

  public PwnedDTO(boolean pwned, int breachCount) {
    this.pwned = pwned;
    this.breachCount = breachCount;
  }
}
