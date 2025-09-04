import React, { createContext, useContext, useState, useEffect, type ReactNode } from 'react';
import { apiService } from '../services/api';
import type { JwtPayload, User } from '../types/auth';
import { jwtDecode } from 'jwt-decode';

interface AuthContextType {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<boolean>;
  register: (userData: any) => Promise<boolean>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

interface AuthProviderProps {
  children: ReactNode;
}

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const checkAuth = async () => {
      if (apiService.isAuthenticated()) {
        try {
          const accessToken = apiService.getAccessToken();
          if (accessToken) {
            const decoded = jwtDecode<JwtPayload>(accessToken);
            const userData: User = {
              id: decoded.userId,
              email: decoded.sub,
              role: decoded.role,
              enabled: true
            };
            //console.log(userData);
            setUser(userData);
            setIsLoading(false);
          }
        } catch (error) {
          apiService.clearTokens();
          setIsLoading(false);
        }
      } else {
        setIsLoading(false);
      }
    };

    checkAuth();
  }, []);

  const login = async (email: string, password: string): Promise<boolean> => {
    try {
      const response = await apiService.login({ email, password });
      if (response.success) {
        apiService.setTokens(response.accessToken, response.refreshToken);
        
        const userData: User = {
          id: 0,
          email: response.userEmail,
          role: 'REGULAR_USER',
          enabled: true
        };
        
        setUser(userData);
        return true;
      }
      return false;
    } catch (error) {
      console.error('Login error:', error);
      return false;
    }
  };

  const register = async (userData: any): Promise<boolean> => {
    try {
      const response = await apiService.register(userData);
      if (response.success) {
        apiService.setTokens(response.accessToken, response.refreshToken);
        
        const user: User = {
          id: 0,
          email: response.userEmail,
          role: 'REGULAR_USER',
          enabled: true
        };
        
        setUser(user);
        return true;
      }
      return false;
    } catch (error) {
      console.error('Registration error:', error);
      return false;
    }
  };

  const logout = () => {
    apiService.logout();
    setUser(null);
  };

  const value: AuthContextType = {
    user,
    isAuthenticated: !!user,
    isLoading,
    login,
    register,
    logout,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};