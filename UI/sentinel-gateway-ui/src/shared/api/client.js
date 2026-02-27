import axios from 'axios';

// Get base URL from environment variable, fallback to localhost for dev
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8079/api';
const LOGS_BASE_URL = `${API_BASE_URL}/logs`;
const MGMT_BASE_URL = `${API_BASE_URL}/mgmt`;

// Configure Axios instance with default headers and interceptors
const apiClient = axios.create({
    headers: {
        'Content-Type': 'application/json'
    }
});

// Example Request Interceptor (uncomment and configure when Auth is ready)
/*
apiClient.interceptors.request.use((config) => {
    const token = localStorage.getItem('token');
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});
*/

// Example Response Interceptor for global error handling
apiClient.interceptors.response.use(
    (response) => response.data, // Simplify response payload
    (error) => {
        if (error.response?.status === 401) {
            // Handle unauthorized - e.g., redirect to login
            console.warn('Unauthorized access. Redirecting...');
        }
        return Promise.reject(error);
    }
);

export const logApi = {
    getRawLogs: (page = 0, size = 50, path = '', statusCode = '') => {
        let url = `${LOGS_BASE_URL}/raw?page=${page}&size=${size}`;
        if (path) url += `&path=${encodeURIComponent(path)}`;
        if (statusCode) url += `&statusCode=${statusCode}`;
        // Since interceptor returns response.data, we pass the generic get
        return apiClient.get(url);
    },
    getDashboardSummary: (start, end) =>
        apiClient.get(`${LOGS_BASE_URL}/stats/summary?start=${start}&end=${end}`),
    getTrafficVelocity: (start, end) =>
        apiClient.get(`${LOGS_BASE_URL}/stats/velocity?start=${start}&end=${end}`),
    getRouteSummary: (start, end) =>
        apiClient.get(`${LOGS_BASE_URL}/routes/summary?start=${start}&end=${end}`),
};

export const mgmtApi = {
    getBlacklist: () => apiClient.get(`${MGMT_BASE_URL}/blacklist`),
    blockUuid: (uuid) => apiClient.post(`${MGMT_BASE_URL}/blacklist/${uuid}`),
    unblockUuid: (uuid) => apiClient.delete(`${MGMT_BASE_URL}/blacklist/${uuid}`),
};
