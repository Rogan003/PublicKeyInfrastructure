package org.example.publickeyinfrastructure.DTOs.Auth;

import lombok.Data;

@Data
public class TokenRefreshDTO {
  private String refreshToken;
}
