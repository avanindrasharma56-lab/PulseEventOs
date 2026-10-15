import React, { createContext, useContext, useState, useEffect } from 'react';
import { authService, tokenStorage } from '../services/api';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(tokenStorage.getUser());
  const [token, setToken] = useState(tokenStorage.get());
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    async function verifyAuth() {
      const storedToken = tokenStorage.get();
      if (storedToken) {
        try {
          const profile = await authService.getCurrentUser();
          setUser(profile);
          setToken(storedToken);
        } catch {
          tokenStorage.remove();
          setUser(null);
          setToken(null);
        }
      }
      setIsLoading(false);
    }
    verifyAuth();
  }, []);

  const login = async (email, password) => {
    const authData = await authService.login({ email, password });
    setUser(authData.user);
    setToken(authData.accessToken);
    return authData;
  };

  const register = async (userData) => {
    const authData = await authService.register(userData);
    setUser(authData.user);
    setToken(authData.accessToken);
    return authData;
  };

  const logout = () => {
    authService.logout();
    setUser(null);
    setToken(null);
  };

  const roles = user?.roles || [];
  const isAdmin = roles.includes('ROLE_ADMIN');
  const isOrganizer = roles.includes('ROLE_ORGANIZER');
  const isAttendee = roles.includes('ROLE_ATTENDEE');

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        isAuthenticated: !!token,
        isLoading,
        roles,
        isAdmin,
        isOrganizer,
        isAttendee,
        login,
        register,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
