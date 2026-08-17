import axios from 'axios';

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8079/api',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' },
});

// Request interceptor: attach JWT if present in localStorage
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('sg_token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// Response interceptor: global error handling
apiClient.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      // redirect to /login
      window.location.href = '/login';
    }
    return Promise.reject(err);
  }
);
