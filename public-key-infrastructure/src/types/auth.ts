export interface LoginDTO {
  email: string;
  password: string;
}

export interface RegistrationDTO {
  email: string;
  password: string;
  name: string;
  surname: string;
  organization: string;
}

interface PwnedDTO {
  pwned: boolean;
  breachCount: number;
}

export interface AuthResponseDTO {
  accessToken: string;
  refreshToken: string;
  message: string;
  success: boolean;
  userEmail: string;
  pwnedPassword: PwnedDTO;
}

export interface TokenRefreshDTO {
  refreshToken: string;
}

export interface User {
  id: number;
  email: string;
  role: string;
  name?: string;
  surname?: string;
  organization?: string;
  enabled: boolean;
}

export interface JwtPayload {
  userId: number;
  sub: string;
  role: string;
  tokenType: string;
  exp: number;
  iat: number;
}

export interface Organisation {
  id: number;
  name: string;
  unit: string;
  country: string;
}
