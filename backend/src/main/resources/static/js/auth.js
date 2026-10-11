const Auth = {
  getCurrentUser() {
    const raw = sessionStorage.getItem('currentUser');
    if (!raw) return null;
    try {
      return JSON.parse(raw);
    } catch (e) {
      return null;
    }
  },

  setCurrentUser(user) {
    if (user) {
      sessionStorage.setItem('currentUser', JSON.stringify(user));
    } else {
      sessionStorage.removeItem('currentUser');
    }
  },

  async checkAuth() {
    try {
      const response = await API.get('/auth/me');
      // Response might be wrapped in CommonResponse { data: user } or direct user
      const user = response.data || response;
      if (!user || (!user.userId && !user.id && !user.email)) {
        throw new Error('Not authenticated');
      }
      Auth.setCurrentUser(user);
      return user;
    } catch (err) {
      Auth.setCurrentUser(null);
      // Redirect to login page
      const currentPath = window.location.pathname;
      const isSubDir = currentPath.includes('/dashboards/') || currentPath.includes('/pages/');
      const loginUrl = isSubDir ? '../login.html' : 'login.html';
      window.location.href = loginUrl;
      return null;
    }
  },

  checkRole(requiredRole) {
    const user = Auth.getCurrentUser();
    if (!user) {
      Auth.checkAuth();
      return false;
    }
    const role = (user.role || '').toUpperCase();
    const target = (requiredRole || '').toUpperCase();
    if (role !== target) {
      alert(`Unauthorized access. Required role: ${target}, but you are ${role}.`);
      Auth.redirectByRole(role);
      return false;
    }
    return true;
  },

  redirectByRole(role) {
    const userRole = (role || '').toUpperCase();
    const currentPath = window.location.pathname;
    const isSubDir = currentPath.includes('/dashboards/') || currentPath.includes('/pages/');
    const prefix = isSubDir ? '../dashboards/' : 'dashboards/';

    if (userRole === 'ADMIN') {
      window.location.href = prefix + 'admin-dashboard.html';
    } else if (userRole === 'PROVIDER') {
      window.location.href = prefix + 'provider-dashboard.html';
    } else if (userRole === 'RECIPIENT') {
      window.location.href = prefix + 'recipient-dashboard.html';
    } else {
      window.location.href = isSubDir ? '../index.html' : 'index.html';
    }
  },

  async logout() {
    try {
      await API.post('/auth/logout');
    } catch (err) {
      console.warn('Logout API failed or session already cleared', err);
    } finally {
      Auth.setCurrentUser(null);
      const currentPath = window.location.pathname;
      const isSubDir = currentPath.includes('/dashboards/') || currentPath.includes('/pages/');
      window.location.href = isSubDir ? '../login.html' : 'login.html';
    }
  }
};
