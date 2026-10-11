// FoodBridge — Administrator Workflow Client Helper

const Admin = {
    async getPlatformAnalytics() {
        return await API.get('/admin/analytics');
    },

    async getUsers(page = 0, size = 20) {
        return await API.get(`/admin/users?page=${page}&size=${size}`);
    },

    async updateUserStatus(userId, status) {
        return await API.patch(`/admin/users/${userId}/status`, { status });
    },

    async getPendingOrganizations() {
        return await API.get('/admin/organizations/pending');
    },

    async verifyOrganization(orgId, status) {
        return await API.patch(`/admin/organizations/${orgId}/verification`, { status });
    },

    async getAuditLogs(page = 0, size = 50) {
        return await API.get(`/admin/audit?page=${page}&size=${size}`);
    },

    async getIssues() {
        return await API.get('/admin/issues');
    }
};
