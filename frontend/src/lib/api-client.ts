const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_URL || "http://localhost:8000";

type RequestOptions = RequestInit & {
  params?: Record<string, string>;
};

class ApiClient {
  private baseUrl: string;

  constructor(baseUrl: string) {
    this.baseUrl = baseUrl;
  }

  private getToken(): string | null {
    if (typeof window === "undefined") return null;
    return localStorage.getItem("access_token");
  }

  private handle401() {
    if (typeof window === "undefined") return;
    localStorage.removeItem("access_token");
    if (!["/login", "/register", "/"].includes(window.location.pathname)) {
      window.location.href = "/login";
    }
  }

  private async request<T>(
    endpoint: string,
    options: RequestOptions = {},
    rawBody = false
  ): Promise<T> {
    const { params, ...fetchOptions } = options;

    let url = `${this.baseUrl}${endpoint}`;
    if (params) {
      const searchParams = new URLSearchParams(params);
      url += `?${searchParams.toString()}`;
    }

    const token = this.getToken();
    const headers: Record<string, string> = { ...(options.headers as Record<string, string>) };
    if (!rawBody) headers["Content-Type"] = "application/json";
    if (token) headers["Authorization"] = `Bearer ${token}`;

    const response = await fetch(url, {
      ...fetchOptions,
      headers,
    });

    if (response.status === 401) {
      this.handle401();
      throw new ApiError(401, "Sesija je istekla. Prijavite se ponovo.");
    }

    if (!response.ok) {
      const error = await response.json().catch(() => ({}));
      throw new ApiError(response.status, error.detail || "Zahtjev nije uspio.");
    }

    if (response.status === 204) return undefined as T;
    return response.json();
  }

  get<T>(endpoint: string, options?: RequestOptions) {
    return this.request<T>(endpoint, { ...options, method: "GET" });
  }

  post<T>(endpoint: string, data?: unknown, options?: RequestOptions) {
    return this.request<T>(endpoint, {
      ...options,
      method: "POST",
      body: data ? JSON.stringify(data) : undefined,
    });
  }

  put<T>(endpoint: string, data?: unknown, options?: RequestOptions) {
    return this.request<T>(endpoint, {
      ...options,
      method: "PUT",
      body: data ? JSON.stringify(data) : undefined,
    });
  }

  patch<T>(endpoint: string, data?: unknown, options?: RequestOptions) {
    return this.request<T>(endpoint, {
      ...options,
      method: "PATCH",
      body: data ? JSON.stringify(data) : undefined,
    });
  }

  delete<T>(endpoint: string, options?: RequestOptions) {
    return this.request<T>(endpoint, { ...options, method: "DELETE" });
  }

  postForm<T>(endpoint: string, formData: FormData, options?: RequestOptions) {
    return this.request<T>(
      endpoint,
      { ...options, method: "POST", body: formData },
      true
    );
  }
}

export class ApiError extends Error {
  constructor(
    public status: number,
    message: string,
    public detail?: string
  ) {
    super(message);
    this.name = "ApiError";
    this.detail = detail ?? message;
  }
}

export const api = new ApiClient(API_BASE_URL);
