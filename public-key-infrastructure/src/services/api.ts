import axios, { type AxiosInstance, type AxiosResponse } from 'axios';
import type { LoginDTO, RegistrationDTO, AuthResponseDTO, TokenRefreshDTO } from '../types/auth';

class ApiService {
  private api: AxiosInstance;
  private baseURL = 'https://localhost:8443/api';

  constructor() {
    this.api = axios.create({
      baseURL: this.baseURL,
      headers: {
        'Content-Type': 'application/json',
      },
    });

    // Request interceptor za dodavanje access tokena
    this.api.interceptors.request.use(
      (config) => {
        const token = localStorage.getItem('accessToken');
        if (token) {
          config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
      },
      (error) => {
        return Promise.reject(error);
      }
    );

    // Response interceptor za automatsko refresh tokena
    this.api.interceptors.response.use(
      (response) => response,
      async (error) => {
        const originalRequest = error.config;

        if (error.response?.status === 401 && !originalRequest._retry) {
          originalRequest._retry = true;

          try {
            const refreshToken = localStorage.getItem('refreshToken');
            if (refreshToken) {
              const response = await this.refreshToken({ refreshToken });
              if (response.success) {
                localStorage.setItem('accessToken', response.accessToken);
                originalRequest.headers.Authorization = `Bearer ${response.accessToken}`;
                return this.api(originalRequest);
              }
            }
          } catch (refreshError) {
            // Refresh token ne radi, redirect na login
            this.logout();
            window.location.href = '/login';
          }
        }

        return Promise.reject(error);
      }
    );
  }

  // Auth endpoints
  async login(credentials: LoginDTO): Promise<AuthResponseDTO> {
    const response: AxiosResponse<AuthResponseDTO> = await this.api.post('/auth/login', credentials);
    return response.data;
  }

  async register(userData: RegistrationDTO): Promise<AuthResponseDTO> {
    const response: AxiosResponse<AuthResponseDTO> = await this.api.post('/auth/register', userData);
    return response.data;
  }

  async refreshToken(tokenData: TokenRefreshDTO): Promise<AuthResponseDTO> {
    const response: AxiosResponse<AuthResponseDTO> = await this.api.post('/auth/refresh', tokenData);
    return response.data;
  }

  async logout(): Promise<void> {
    // For stateless refresh tokens, logout is handled client-side
    // Simply clear the tokens from localStorage
    this.clearTokens();
  }

  // Token management
  setTokens(accessToken: string, refreshToken: string): void {
    localStorage.setItem('accessToken', accessToken);
    localStorage.setItem('refreshToken', refreshToken);
  }

  clearTokens(): void {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
  }

  getAccessToken(): string | null {
    return localStorage.getItem('accessToken');
  }

  getRefreshToken(): string | null {
    return localStorage.getItem('refreshToken');
  }

  isAuthenticated(): boolean {
    return !!this.getAccessToken();
  }
}

export const apiService = new ApiService();