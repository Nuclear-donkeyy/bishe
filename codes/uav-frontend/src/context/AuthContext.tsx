import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import { authApi, type UserInfo } from '../services/api';
import { getToken, setToken } from '../services/http';
import { normalizeRole } from '../utils/roles';

interface AuthContextValue {
  currentUser: UserInfo | null;
  login: (username: string, password: string) => Promise<UserInfo>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [currentUser, setCurrentUser] = useState<UserInfo | null>(null);

  useEffect(() => {
    const token = getToken();
    if (token) {
      authApi
        .profile()
        .then(user => setCurrentUser({ ...user, role: normalizeRole(user.role) }))
        .catch(() => {
          setToken(null);
          setCurrentUser(null);
        });
    }
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      currentUser,
      login: async (username, password) => {
        const res = await authApi.login({ username, password });
        setToken(res.token);
        const user = { ...res.user, role: normalizeRole(res.user.role) };
        setCurrentUser(user);
        return user;
      },
      logout: () => {
        setToken(null);
        setCurrentUser(null);
        authApi.logout().catch(() => undefined);
      }
    }),
    [currentUser]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
}
