import axios from 'axios';

// Eureka expects an Accept header for JSON, otherwise it defaults to XML
export const actuatorApi = {
    // Fetch apps from Eureka Server
    getEurekaApps: async (eurekaUrl) => {
        let url = eurekaUrl;
        if (!url.endsWith('/apps') && !url.endsWith('/apps/')) {
            url = url.endsWith('/') ? url + 'eureka/apps' : url + '/eureka/apps';
        }

        const response = await axios.get(url, {
            headers: {
                'Accept': 'application/json'
            }
        });
        return response.data;
    },

    // Actuator endpoints
    getHealth: async (actuatorUrl) => {
        const url = actuatorUrl.endsWith('/') ? actuatorUrl + 'health' : actuatorUrl + '/health';
        return await axios.get(url);
    },

    getInfo: async (actuatorUrl) => {
        const url = actuatorUrl.endsWith('/') ? actuatorUrl + 'info' : actuatorUrl + '/info';
        return await axios.get(url);
    },

    getMetricsList: async (actuatorUrl) => {
        const url = actuatorUrl.endsWith('/') ? actuatorUrl + 'metrics' : actuatorUrl + '/metrics';
        return await axios.get(url);
    },

    getMetricDetail: async (actuatorUrl, metricName) => {
        const url = actuatorUrl.endsWith('/') ? actuatorUrl + `metrics/${metricName}` : actuatorUrl + `/metrics/${metricName}`;
        return await axios.get(url);
    },

    getEnv: async (actuatorUrl) => {
        const url = actuatorUrl.endsWith('/') ? actuatorUrl + 'env' : actuatorUrl + '/env';
        return await axios.get(url);
    }
};
