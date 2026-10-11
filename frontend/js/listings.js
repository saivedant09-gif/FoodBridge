// FoodBridge — Food Listings Client Utilities

const Listings = {
    async fetchActiveListings(filters = {}) {
        const { category = '', city = '', page = 0, size = 10 } = filters;
        let url = `/listings?page=${page}&size=${size}`;
        if (category) url += `&category=${encodeURIComponent(category)}`;
        if (city) url += `&city=${encodeURIComponent(city)}`;
        return await API.get(url);
    },

    async fetchListingById(id) {
        return await API.get(`/listings/${id}`);
    },

    renderListingCard(item) {
        return `
            <div class="col-md-6 col-lg-4">
                <div class="fb-food-card h-100 p-3 position-relative d-flex flex-column">
                    <span class="badge bg-secondary mb-2 align-self-start">${item.category}</span>
                    <h5 class="fw-bold mb-1">${item.foodName}</h5>
                    <p class="text-muted small mb-2"><i class="fa-solid fa-building me-1"></i>${item.providerName || 'Provider'}</p>
                    <div class="my-2">
                        <span class="fs-5 fw-bold text-success">${item.availableQuantity}</span>
                        <span class="text-muted small">${item.unit} available</span>
                    </div>
                    <div class="small text-danger mb-3">
                        <i class="fa-regular fa-clock me-1"></i>Deadline: ${formatDateTime(item.pickupDeadline)}
                    </div>
                    <div class="mt-auto">
                        <a href="listing-details.html?id=${item.listingId}" class="btn btn-outline-success btn-sm w-100">
                            View Details & Request
                        </a>
                    </div>
                </div>
            </div>
        `;
    }
};
