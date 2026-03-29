import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api-client";

interface LoginInput {
  email: string;
  password: string;
}

interface AuthUser {
  id: number;
  email: string;
}

interface TokenResponse {
  access_token: string;
  token_type: string;
  user: AuthUser;
}

export function useAuth() {
  return useQuery({
    queryKey: ["auth", "me"],
    queryFn: () => api.get<AuthUser>("/auth/me"),
    retry: false,
  });
}

export function useLogin() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: LoginInput) =>
      api.post<TokenResponse>("/auth/login", data),
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
