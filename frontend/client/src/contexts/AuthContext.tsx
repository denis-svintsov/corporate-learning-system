import { createContext, useContext, useState, useEffect, ReactNode } from "react";
import { useLocation } from "wouter";
import { extractApiErrorMessage } from "@/lib/apiError";

interface User {
  id: string;
  username: string;
  email: string;
  positionId?: string | null;
  roles: string[];
}

interface AuthContextType {
  user: User | null;
  token: string | null;
  login: (username: string, password: string) => Promise<void>;
  register: (data: RegisterData) => Promise<void>;
  logout: () => void;
  isAuthenticated: boolean;
  isLoading: boolean;
}

interface RegisterData {
  username: string;
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  positionId?: string;
  departmentId?: string;
  hireDate?: string;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

const AUTH_API_URL = import.meta.env.VITE_AUTH_API_URL ?? "http://localhost:8080/auth";
const TOKEN_KEY = "auth_token";
const USER_KEY = "auth_user";

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [, setLocation] = useLocation();

  useEffect(() => {
    // Восстановление сессии из localStorage
    const savedToken = localStorage.getItem(TOKEN_KEY);
    const savedUser = localStorage.getItem(USER_KEY);
    
    if (savedToken && savedUser) {
      setToken(savedToken);
      try {
        const parsed = JSON.parse(savedUser);
        if (!parsed?.id) {
          localStorage.removeItem(TOKEN_KEY);
          localStorage.removeItem(USER_KEY);
        } else {
          setUser(parsed);
        }
      } catch (e) {
        localStorage.removeItem(TOKEN_KEY);
        localStorage.removeItem(USER_KEY);
      }
    }
    setIsLoading(false);
  }, []);

  useEffect(() => {
    const handleUnauthorized = () => {
      setToken(null);
      setUser(null);
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(USER_KEY);
      setLocation("/auth");
    };

    window.addEventListener("auth:unauthorized", handleUnauthorized);
    return () => window.removeEventListener("auth:unauthorized", handleUnauthorized);
  }, [setLocation]);

  const login = async (username: string, password: string) => {
    try {
      const response = await fetch(`${AUTH_API_URL}/login`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ username, password }),
      });

      if (!response.ok) {
        throw new Error(await extractApiErrorMessage(response, "Неверный логин или пароль"));
      }

      const data = await response.json();
      const userData: User = {
        id: data.userId,
        username: data.username,
        email: data.email,
        positionId: data.positionId ?? null,
        roles: data.roles || [],
      };

      setToken(data.token);
      setUser(userData);
      localStorage.setItem(TOKEN_KEY, data.token);
      localStorage.setItem(USER_KEY, JSON.stringify(userData));
      setLocation("/");
    } catch (error) {
      throw error;
    }
  };

  const register = async (data: RegisterData) => {
    try {
      const response = await fetch(`${AUTH_API_URL}/register`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(data),
      });

      if (!response.ok) {
        throw new Error(await extractApiErrorMessage(response, "Ошибка при регистрации"));
      }

      const responseData = await response.json();
      const userData: User = {
        id: responseData.userId,
        username: responseData.username,
        email: responseData.email,
        positionId: responseData.positionId ?? null,
        roles: responseData.roles || [],
      };

      setToken(responseData.token);
      setUser(userData);
      localStorage.setItem(TOKEN_KEY, responseData.token);
      localStorage.setItem(USER_KEY, JSON.stringify(userData));
      setLocation("/");
    } catch (error) {
      throw error;
    }
  };

  const logout = async () => {
    // Инвалидация сессии на сервере
    if (token) {
      try {
        await fetch(`${AUTH_API_URL}/logout`, {
          method: "POST",
          headers: {
            "Authorization": `Bearer ${token}`,
            "Content-Type": "application/json",
          },
        });
      } catch (error) {
        console.error("Ошибка при выходе:", error);
      }
    }
    
    setToken(null);
    setUser(null);
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    setLocation("/auth");
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        login,
        register,
        logout,
        isAuthenticated: !!token && !!user,
        isLoading,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
