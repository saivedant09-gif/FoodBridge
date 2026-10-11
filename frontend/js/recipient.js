// FoodBridge — Recipient Workflow Client Helper

const Recipient = {
    async getDashboardStats() {
        return await API.get('/analytics/recipient');
    },

    async getAvailableFood(category = '', city = '', page = 0, size = 10) {
        let path = `/listings?page=${page}&size=${size}`;
        if (category) path += `&category=${encodeURIComponent(category)}`;
        if (city) path += `&city=${encodeURIComponent(city)}`;
        return await API.get(path);
    },

    async submitClaim(listingId, quantity, message) {
        return await API.post(`/listings/${listingId}/requests`, {
            requestedQuantity: quantity,
            message: message
        });
    },

    async getMyRequests(page = 0, size = 10) {
        return await API.get(`/requests/me?page=${page}&size=${size}`);
    },

    async cancelRequest(requestId) {
        return await API.patch(`/requests/${requestId}/cancel`);
    },

    async getMyAllocations(page = 0, size = 10) {
        return await API.get(`/allocations/me?page=${page}&size=${size}`);
    },

    async confirmReceipt(collectionId) {
        return await API.post(`/collections/${collectionId}/confirm-recipient`);
    }
};
