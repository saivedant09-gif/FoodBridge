const Common = {
  showAlert(message, type = 'success', containerId = 'alertContainer') {
    const container = document.getElementById(containerId);
    if (!container) return;

    const bsClass = type === 'error' ? 'danger' : type;
    const alertHtml = `
      <div class="alert alert-${bsClass} alert-dismissible fade show" role="alert">
        ${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
      </div>
    `;
    container.innerHTML = alertHtml;
  },

  showLoading(containerId) {
    const container = document.getElementById(containerId);
    if (!container) return;
    container.innerHTML = `
      <div class="text-center py-5">
        <div class="spinner-border text-success" role="status" style="width: 3rem; height: 3rem;">
          <span class="visually-hidden">Loading...</span>
        </div>
        <p class="mt-3 text-muted">Loading data...</p>
      </div>
    `;
  },

  hideLoading(containerId) {
    const container = document.getElementById(containerId);
    if (container) {
      container.innerHTML = '';
    }
  },

  formatDate(dateStr) {
    if (!dateStr) return 'N/A';
    try {
      const d = new Date(dateStr);
      return d.toLocaleDateString(undefined, {
        year: 'numeric',
        month: 'short',
        day: 'numeric'
      });
    } catch (e) {
      return dateStr;
    }
  },

  formatDateTime(dateStr) {
    if (!dateStr) return 'N/A';
    try {
      const d = new Date(dateStr);
      return d.toLocaleString(undefined, {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch (e) {
      return dateStr;
    }
  },

  statusBadge(status) {
    if (!status) return '<span class="badge bg-secondary">UNKNOWN</span>';
    const s = status.toUpperCase();
    return `<span class="badge-status badge-status-${s}">${s}</span>`;
  },

  emptyState(message = 'No data available', icon = 'bi-inbox') {
    return `
      <div class="text-center py-5 text-muted">
        <i class="bi ${icon}" style="font-size: 3rem; color: #9EAF9F;"></i>
        <p class="mt-3 fs-6">${message}</p>
      </div>
    `;
  },

  formatQuantity(qty, unit) {
    if (qty === undefined || qty === null) return '0';
    return `${qty} ${unit || ''}`.trim();
  },

  initMobileSidebar() {
    const toggleBtn = document.querySelector('.sidebar-toggle-btn');
    const sidebar = document.querySelector('.sidebar');
    const backdrop = document.querySelector('.sidebar-backdrop');

    if (toggleBtn && sidebar && backdrop) {
      toggleBtn.addEventListener('click', () => {
        sidebar.classList.toggle('show');
        backdrop.classList.toggle('show');
      });

      backdrop.addEventListener('click', () => {
        sidebar.classList.remove('show');
        backdrop.classList.remove('show');
      });
    }
  }
};
