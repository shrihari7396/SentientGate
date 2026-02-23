import axios from 'axios';

const LOGS_BASE_URL = 'http://localhost:8079/api/logs';
const MGMT_BASE_URL = 'http://localhost:8079/api/mgmt';

export const logApi = {
    getRawLogs: (page = 0, size = 50, path = '', statusCode = '') => {
        let url = `${LOGS_BASE_URL}/raw?page=${page}&size=${size}`;
        if (path) url += `&path=${encodeURIComponent(path)}`;
        if (statusCode) url += `&statusCode=${statusCode}`;
        return axios.get(url);
    },
    getDashboardSummary: (start, end) =>
        axios.get(`${LOGS_BASE_URL}/stats/summary?start=${start}&end=${end}`),
    getTrafficVelocity: (start, end) =>
        axios.get(`${LOGS_BASE_URL}/stats/velocity?start=${start}&end=${end}`),
    getRouteSummary: (start, end) =>
        axios.get(`${LOGS_BASE_URL}/routes/summary?start=${start}&end=${end}`),
};

export const mgmtApi = {
    getBlacklist: () => axios.get(`${MGMT_BASE_URL}/blacklist`),
    blockUuid: (uuid) => axios.post(`${MGMT_BASE_URL}/blacklist/${uuid}`),
    unblockUuid: (uuid) => axios.delete(`${MGMT_BASE_URL}/blacklist/${uuid}`),
};
