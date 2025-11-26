import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api';
const TOKEN_KEY = 'uav_token';

export function setToken(token: string | null) {
  if (token) {
    localStorage.setItem(TOKEN_KEY, token);
  } else {
    localStorage.removeItem(TOKEN_KEY);
  }
}

export function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

export const http = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000
});

export function ensureSuccess<T>(data: any, defaultMessage = '请求失败'): T {
  if (data && data.success === false) {
    throw new Error(data.message || defaultMessage);
  }
  return data as T;
}

http.interceptors.request.use(config => {
  const token = getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

http.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 401) {
      setToken(null);
    }
    return Promise.reject(error);
  }
);

export type ApiPage<T> = {
  content: T[];
  totalElements?: number;
  total?: number;
};

export function unwrapPage<T>(page: ApiPage<T>): { items: T[]; total: number } {
  const total = page.totalElements ?? page.total ?? page.content?.length ?? 0;
  return { items: page.content ?? [], total };
}
