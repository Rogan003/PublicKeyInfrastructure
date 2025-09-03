import React, { createContext, useContext, useState, useEffect, type ReactNode } from 'react';
import { apiService } from '../services/api';
import type { User } from '../types/auth';

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
    // Proveri da li postoji validan token pri učitavanju
    const checkAuth = async () => {
      if (apiService.isAuthenticated()) {
        try {
          // Ovde možeš dodati poziv na backend da proveriš validnost tokena
          // i da dobiješ informacije o korisniku
          setIsLoading(false);
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
        
        // Kreiraj user objekat
        const userData: User = {
          id: 0, // Ovo ćeš dobiti iz JWT tokena ili backend-a
          email: response.userEmail,
          role: 'REGULAR_USER', // Ovo ćeš dobiti iz JWT tokena
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