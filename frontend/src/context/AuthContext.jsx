
import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useState,
} from 'react';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUserState] = useState(() => {
    try {
      return JSON.parse(localStorage.getItem('user') || 'null');
    } catch {
      return null;
    }
  });

  const setUser = useCallback((nextUser) => {
    setUserState(nextUser);

    if (nextUser) {
      localStorage.setItem('user', JSON.stringify(nextUser));
    } else {
      localStorage.removeItem('user');
    }
  }, []);

  const login = useCallback((token, nextUser) => {
    if (token) {
      localStorage.setItem('token', token);
    }

    setUser(nextUser);
  }, [setUser]);

  const logout = useCallback(() => {
    localStorage.removeItem('token');
    setUser(null);
  }, [setUser]);

  const value = useMemo(
      () => ({ user, setUser, login, logout }),
      [user, setUser, login, logout]
  );

  return (
      <AuthContext.Provider value={value}>
        {children}
      </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }

  return context;
}
