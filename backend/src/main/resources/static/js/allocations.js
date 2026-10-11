// FoodBridge — Allocations Management Helper

const Allocations = {
    async fetchMyAllocations(page = 0, size = 10) {
        return await API.get(`/allocations/me?page=${page}&size=${size}`);
    },

    async fetchAllocationById(id) {
        return await API.get(`/allocations/${id}`);
    },

    async cancelAllocation(allocationId, reason) {
        return await API.post(`/allocations/${allocationId}/cancel`, { reason });
    }
};
