import { createContext, useContext, useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import { userAccounts, type UserAccount } from '../data/mock';

interface AuthContextValue {
  currentUser: UserAccount | null;
  login: (username: string, password: string) => Promise<UserAccount>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [currentUser, setCurrentUser] = useState<UserAccount | null>(null);

  const value = useMemo<AuthContextValue>(
    () => ({
      currentUser,
      login: async (username, password) => {
        const user = userAccounts.find(
          account => account.username === username && account.password === password
        );
        if (!user) {
          return Promise.reject(new Error('账号或密码错误'));
        }
        setCurrentUser(user);
        return user;
      },
      logout: () => setCurrentUser(null)
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
