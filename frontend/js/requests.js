// FoodBridge — Requests Management Helper

const Requests = {
    async fetchMyRequests(page = 0, size = 10) {
        return await API.get(`/requests/me?page=${page}&size=${size}`);
    },

    async fetchProviderRequests(page = 0, size = 10) {
        return await API.get(`/provider/requests?page=${page}&size=${size}`);
    },

    async cancelRequest(requestId) {
        return await API.patch(`/requests/${requestId}/cancel`);
    }
};
