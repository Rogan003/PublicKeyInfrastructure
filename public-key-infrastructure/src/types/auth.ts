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

export interface AuthResponseDTO {
  accessToken: string;
  refreshToken: string;
  message: string;
  success: boolean;
  userEmail: string;
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