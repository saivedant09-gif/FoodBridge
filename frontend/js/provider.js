// FoodBridge — Provider Workflow Client Helper

const Provider = {
    async getDashboardStats() {
        return await API.get('/analytics/provider');
    },

    async getMyListings(page = 0, size = 10) {
        return await API.get(`/provider/listings?page=${page}&size=${size}`);
    },

    async createListing(listingData) {
        return await API.post('/listings', listingData);
    },

    async cancelListing(listingId) {
        return await API.delete(`/listings/${listingId}`);
    },

    async getIncomingRequests(page = 0, size = 10) {
        return await API.get(`/provider/requests?page=${page}&size=${size}`);
    },

    async approveRequest(requestId) {
        return await API.post(`/requests/${requestId}/approve`);
    },

    async confirmHandover(collectionId) {
        return await API.post(`/collections/${collectionId}/confirm-provider`);
    }
};
