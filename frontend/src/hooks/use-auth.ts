import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api-client";
import { USE_MOCKS, mockDelay } from "@/lib/mocks";
import { mockUser, MOCK_TOKEN } from "@/lib/mocks/user";

interface LoginInput {
  email: string;
  password: string;
}

interface RegisterInput {
  email: string;
  password: string;
  name?: string;
}

interface AuthUser {
  id: number;
  email: string;
  name?: string;
}

interface TokenResponse {
  access_token: string;
  token_type: string;
  user: AuthUser;
}

export function useAuth() {
  return useQuery({
    queryKey: ["auth", "me"],
    queryFn: async () => {
      if (USE_MOCKS) {
        if (typeof window !== "undefined" && !localStorage.getItem("access_token")) {
          throw new Error("Not authenticated");
        }
        return mockDelay(mockUser as AuthUser);
      }
      return api.get<AuthUser>("/auth/me");
    },
    retry: false,
  });
}

export function useLogin() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (data: LoginInput) => {
      if (USE_MOCKS) {
        await mockDelay(null, 400);
        if (!data.email || !data.password)
          throw new Error("Email i lozinka su obavezni.");
        return {
          access_token: MOCK_TOKEN,
          token_type: "Bearer",
          user: mockUser,
        } as TokenResponse;
      }
      return api.post<TokenResponse>("/auth/login", data);
    },
    onSuccess: (data) => {
      localStorage.setItem("access_token", data.access_token);
      queryClient.invalidateQueries({ queryKey: ["auth"] });
    },
  });
}

export function useRegister() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (data: RegisterInput) => {
      if (USE_MOCKS) {
        await mockDelay(null, 400);
        if (!data.email || !data.password)
          throw new Error("Email i lozinka su obavezni.");
        return {
          access_token: MOCK_TOKEN,
          token_type: "Bearer",
          user: { ...mockUser, email: data.email, name: data.name ?? mockUser.name },
        } as TokenResponse;
      }
      return api.post<TokenResponse>("/auth/register", data);
    },
    onSuccess: (data) => {
      localStorage.setItem("access_token", data.access_token);
      queryClient.invalidateQueries({ queryKey: ["auth"] });
    },
  });
}

export function useLogout() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async () => {
      localStorage.removeItem("access_token");
    },
    onSuccess: () => {
      queryClient.clear();
    },
  });
}
