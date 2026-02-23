import axios from 'axios';

const LOGGING_SERVICE_URL = 'http://localhost:8010/loggingService/api';
const GATEWAY_MGMT_URL = 'http://localhost:8080/api/mgmt';

export const logApi = {
    getRawLogs: (page = 0, size = 50, path = '', statusCode = '') => {
        let url = `${LOGGING_SERVICE_URL}/logs/raw?page=${page}&size=${size}`;
        if (path) url += `&path=${encodeURIComponent(path)}`;
        if (statusCode) url += `&statusCode=${statusCode}`;
        return axios.get(url);
    },
    getRouteSummary: (start, end) =>
        axios.get(`${LOGGING_SERVICE_URL}/logs/routes/summary?start=${start}&end=${end}`),
    getTimeSummary: (start, end) =>
        axios.get(`${LOGGING_SERVICE_URL}/logs/time/summary?start=${start}&end=${end}`),
    getIpSummary: (ip, start, end) =>
        axios.get(`${LOGGING_SERVICE_URL}/logs/ip/${ip}/summary?start=${start}&end=${end}`),
};

export const mgmtApi = {
    getBlacklist: () => axios.get(`${GATEWAY_MGMT_URL}/blacklist`),
    blockUuid: (uuid) => axios.post(`${GATEWAY_MGMT_URL}/blacklist/${uuid}`),
    unblockUuid: (uuid) => axios.delete(`${GATEWAY_MGMT_URL}/blacklist/${uuid}`),
};
