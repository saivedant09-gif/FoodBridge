package com.foodbridge.repository;

import com.foodbridge.dto.ImpactSummaryResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class AnalyticsRepository {

    private final JdbcTemplate jdbcTemplate;

    public AnalyticsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public ImpactSummaryResponse getPlatformSummary() {
        Long verifiedOrgs = queryCount("SELECT COUNT(*) FROM organizations WHERE verification_status = 'APPROVED'");
        Long totalListings = queryCount("SELECT COUNT(*) FROM food_listings");
        Long activeListings = queryCount("SELECT COUNT(*) FROM food_listings WHERE listing_status = 'ACTIVE'");
        Long completedListings = queryCount("SELECT COUNT(*) FROM food_listings WHERE listing_status = 'COMPLETED'");
        Long expiredListings = queryCount("SELECT COUNT(*) FROM food_listings WHERE listing_status = 'EXPIRED'");
        Long totalRequests = queryCount("SELECT COUNT(*) FROM food_requests");
        Long fulfilledRequests = queryCount("SELECT COUNT(*) FROM food_requests WHERE request_status = 'APPROVED'");

        String sumAllocSql = "SELECT COALESCE(SUM(allocated_quantity), 0) FROM allocations WHERE allocation_status IN ('CONFIRMED', 'COMPLETED')";
        BigDecimal totalAllocated = jdbcTemplate.queryForObject(sumAllocSql, BigDecimal.class);
        if (totalAllocated == null) {
            totalAllocated = BigDecimal.ZERO;
        }

        Long completedCollections = queryCount("SELECT COUNT(*) FROM collections WHERE collection_status = 'COLLECTED'");
        Long providerUsers = queryCount("SELECT COUNT(*) FROM users WHERE role = 'PROVIDER'");
        Long recipientUsers = queryCount("SELECT COUNT(*) FROM users WHERE role = 'RECIPIENT'");

        double fulfillmentRate = totalRequests > 0
                ? (fulfilledRequests.doubleValue() / totalRequests.doubleValue()) * 100.0
                : 0.0;

        return ImpactSummaryResponse.builder()
                .verifiedOrgs(verifiedOrgs)
                .totalListings(totalListings)
                .activeListings(activeListings)
                .completedListings(completedListings)
                .expiredListings(expiredListings)
                .totalRequests(totalRequests)
                .fulfilledRequests(fulfilledRequests)
                .totalAllocated(totalAllocated)
                .completedCollections(completedCollections)
                .providerUsers(providerUsers)
                .recipientUsers(recipientUsers)
                .fulfillmentRate(Math.round(fulfillmentRate * 100.0) / 100.0)
                .build();
    }

    public List<Map<String, Object>> getCategoryDistribution() {
        String sql = "SELECT category, COUNT(*) AS count, COALESCE(SUM(total_quantity), 0) AS total_quantity " +
                     "FROM food_listings GROUP BY category ORDER BY count DESC";
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> getMonthlyCollections() {
        String sql = "SELECT DATE_FORMAT(c.collected_at, '%Y-%m') AS month, COUNT(*) AS count, " +
                     "COALESCE(SUM(a.allocated_quantity), 0) AS total_quantity " +
                     "FROM collections c " +
                     "JOIN allocations a ON c.allocation_id = a.allocation_id " +
                     "WHERE c.collection_status = 'COLLECTED' AND c.collected_at IS NOT NULL " +
                     "GROUP BY DATE_FORMAT(c.collected_at, '%Y-%m') " +
                     "ORDER BY month ASC";
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> getRequestStatusDistribution() {
        String sql = "SELECT request_status AS status, COUNT(*) AS count " +
                     "FROM food_requests GROUP BY request_status ORDER BY count DESC";
        return jdbcTemplate.queryForList(sql);
    }

    public Map<String, Object> getProviderStats(Long orgId) {
        Map<String, Object> stats = new HashMap<>();

        Long totalListings = queryCount("SELECT COUNT(*) FROM food_listings WHERE provider_organization_id = " + orgId);
        Long activeListings = queryCount("SELECT COUNT(*) FROM food_listings WHERE provider_organization_id = " + orgId + " AND listing_status = 'ACTIVE'");

        String sumQtySql = "SELECT COALESCE(SUM(total_quantity), 0) FROM food_listings WHERE provider_organization_id = ?";
        BigDecimal totalQuantityDonated = jdbcTemplate.queryForObject(sumQtySql, BigDecimal.class, orgId);

        String sumAllocSql = "SELECT COALESCE(SUM(a.allocated_quantity), 0) FROM allocations a " +
                             "JOIN food_listings fl ON a.listing_id = fl.listing_id " +
                             "WHERE fl.provider_organization_id = ? AND a.allocation_status IN ('CONFIRMED', 'COMPLETED')";
        BigDecimal totalAllocated = jdbcTemplate.queryForObject(sumAllocSql, BigDecimal.class, orgId);

        String compColSql = "SELECT COUNT(*) FROM collections c " +
                            "JOIN allocations a ON c.allocation_id = a.allocation_id " +
                            "JOIN food_listings fl ON a.listing_id = fl.listing_id " +
                            "WHERE fl.provider_organization_id = ? AND c.collection_status = 'COLLECTED'";
        Long completedCollections = jdbcTemplate.queryForObject(compColSql, Long.class, orgId);

        stats.put("totalListings", totalListings);
        stats.put("activeListings", activeListings);
        stats.put("totalQuantityDonated", totalQuantityDonated != null ? totalQuantityDonated : BigDecimal.ZERO);
        stats.put("totalAllocated", totalAllocated != null ? totalAllocated : BigDecimal.ZERO);
        stats.put("completedCollections", completedCollections != null ? completedCollections : 0L);

        return stats;
    }

    public Map<String, Object> getRecipientStats(Long orgId) {
        Map<String, Object> stats = new HashMap<>();

        Long totalRequests = queryCount("SELECT COUNT(*) FROM food_requests WHERE recipient_organization_id = " + orgId);
        Long approvedRequests = queryCount("SELECT COUNT(*) FROM food_requests WHERE recipient_organization_id = " + orgId + " AND request_status = 'APPROVED'");

        String sumAllocSql = "SELECT COALESCE(SUM(allocated_quantity), 0) FROM allocations " +
                             "WHERE recipient_organization_id = ? AND allocation_status IN ('CONFIRMED', 'COMPLETED')";
        BigDecimal totalAllocated = jdbcTemplate.queryForObject(sumAllocSql, BigDecimal.class, orgId);

        String compColSql = "SELECT COUNT(*) FROM collections c " +
                            "JOIN allocations a ON c.allocation_id = a.allocation_id " +
                            "WHERE a.recipient_organization_id = ? AND c.collection_status = 'COLLECTED'";
        Long completedCollections = jdbcTemplate.queryForObject(compColSql, Long.class, orgId);

        stats.put("totalRequests", totalRequests);
        stats.put("approvedRequests", approvedRequests);
        stats.put("totalQuantityReceived", totalAllocated != null ? totalAllocated : BigDecimal.ZERO);
        stats.put("completedCollections", completedCollections != null ? completedCollections : 0L);

        return stats;
    }

    private Long queryCount(String sql) {
        Long val = jdbcTemplate.queryForObject(sql, Long.class);
        return val != null ? val : 0L;
    }
}
