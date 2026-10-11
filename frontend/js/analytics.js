// FoodBridge — Analytics and Chart Visualizations Helper

const Analytics = {
    async getImpactSummary() {
        return await API.get('/reports/impact');
    },

    async getCategoryDistribution() {
        return await API.get('/analytics/categories');
    },

    async getMonthlyCollections() {
        return await API.get('/analytics/monthly-collections');
    },

    async getRequestStatusDistribution() {
        return await API.get('/analytics/request-status');
    },

    renderDoughnutChart(canvasId, labels, data, colors) {
        const ctx = document.getElementById(canvasId).getContext('2d');
        return new Chart(ctx, {
            type: 'doughnut',
            data: {
                labels: labels,
                datasets: [{
                    data: data,
                    backgroundColor: colors || ['#176B45', '#E9A23B', '#3B82F6', '#8B5CF6', '#EC4899', '#6B7280'],
                    borderWidth: 2,
                    borderColor: '#FFFFFF'
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { position: 'bottom' }
                }
            }
        });
    },

    renderBarChart(canvasId, labels, data, label = 'Metric') {
        const ctx = document.getElementById(canvasId).getContext('2d');
        return new Chart(ctx, {
            type: 'bar',
            data: {
                labels: labels,
                datasets: [{
                    label: label,
                    data: data,
                    backgroundColor: '#176B45',
                    borderRadius: 6
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                    y: { beginAtZero: true, grid: { color: '#F1F5F9' } },
                    x: { grid: { display: false } }
                }
            }
        });
    }
};
