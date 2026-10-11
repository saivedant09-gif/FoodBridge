// FoodBridge — In-App Notifications Helper

const Notifications = {
    async fetchNotifications(page = 0, size = 20) {
        return await API.get(`/notifications?page=${page}&size=${size}`);
    },

    async getUnreadCount() {
        return await API.get('/notifications/unread-count');
    },

    async markAsRead(id) {
        return await API.patch(`/notifications/${id}/read`);
    },

    async markAllAsRead() {
        return await API.post('/notifications/mark-all-read');
    }
};
