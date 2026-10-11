package com.foodbridge.repository;

import com.foodbridge.model.FoodRequest;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class RequestRepository {

    private final JdbcTemplate jdbcTemplate;

    public RequestRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<FoodRequest> foodRequestRowMapper = (rs, rowNum) -> {
        FoodRequest req = FoodRequest.builder()
                .requestId(rs.getLong("request_id"))
                .listingId(rs.getLong("listing_id"))
                .recipientOrganizationId(rs.getLong("recipient_organization_id"))
                .requestedQuantity(rs.getBigDecimal("requested_quantity"))
                .requestStatus(rs.getString("request_status"))
                .message(rs.getString("message"))
                .rejectionReason(rs.getString("rejection_reason"))
                .createdAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null)
                .updatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null)
                .build();

        try {
            req.setFoodName(rs.getString("food_name"));
        } catch (SQLException ignored) {}

        try {
            req.setRecipientName(rs.getString("recipient_name"));
        } catch (SQLException ignored) {}

        try {
            req.setUnit(rs.getString("unit"));
        } catch (SQLException ignored) {}

        return req;
    };

    public Long save(FoodRequest request) {
        String sql = "INSERT INTO food_requests (listing_id, recipient_organization_id, requested_quantity, " +
                     "request_status, message, rejection_reason, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, request.getListingId());
            ps.setLong(2, request.getRecipientOrganizationId());
            ps.setBigDecimal(3, request.getRequestedQuantity());
            ps.setString(4, request.getRequestStatus() != null ? request.getRequestStatus() : "PENDING");
            ps.setString(5, request.getMessage());
            ps.setString(6, request.getRejectionReason());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            request.setRequestId(key.longValue());
            return key.longValue();
        }
        return null;
    }

    public Optional<FoodRequest> findById(Long id) {
        String sql = "SELECT fr.*, fl.food_name, fl.unit, o.organization_name AS recipient_name " +
                     "FROM food_requests fr " +
                     "JOIN food_listings fl ON fr.listing_id = fl.listing_id " +
                     "JOIN organizations o ON fr.recipient_organization_id = o.organization_id " +
                     "WHERE fr.request_id = ?";
        try {
            FoodRequest req = jdbcTemplate.queryForObject(sql, foodRequestRowMapper, id);
            return Optional.ofNullable(req);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public List<FoodRequest> findByListingId(Long listingId) {
        String sql = "SELECT fr.*, fl.food_name, fl.unit, o.organization_name AS recipient_name " +
                     "FROM food_requests fr " +
                     "JOIN food_listings fl ON fr.listing_id = fl.listing_id " +
                     "JOIN organizations o ON fr.recipient_organization_id = o.organization_id " +
                     "WHERE fr.listing_id = ? " +
                     "ORDER BY fr.created_at ASC";
        return jdbcTemplate.query(sql, foodRequestRowMapper, listingId);
    }

    public List<FoodRequest> findByRecipientOrgId(Long orgId, int page, int size) {
        int offset = Math.max(0, page) * size;
        String sql = "SELECT fr.*, fl.food_name, fl.unit, o.organization_name AS recipient_name " +
                     "FROM food_requests fr " +
                     "JOIN food_listings fl ON fr.listing_id = fl.listing_id " +
                     "JOIN organizations o ON fr.recipient_organization_id = o.organization_id " +
                     "WHERE fr.recipient_organization_id = ? " +
                     "ORDER BY fr.created_at DESC LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, foodRequestRowMapper, orgId, size, offset);
    }

    public void updateStatus(Long requestId, String status, String rejectionReason) {
        String sql = "UPDATE food_requests SET request_status = ?, rejection_reason = ?, updated_at = NOW() WHERE request_id = ?";
        jdbcTemplate.update(sql, status, rejectionReason, requestId);
    }

    public boolean existsActiveRequestForListingByOrg(Long listingId, Long orgId) {
        String sql = "SELECT COUNT(*) FROM food_requests WHERE listing_id = ? AND recipient_organization_id = ? " +
                     "AND request_status IN ('PENDING', 'APPROVED')";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, listingId, orgId);
        return count != null && count > 0;
    }

    public long countPendingByProviderOrgId(Long providerOrgId) {
        String sql = "SELECT COUNT(*) FROM food_requests fr " +
                     "JOIN food_listings fl ON fr.listing_id = fl.listing_id " +
                     "WHERE fl.provider_organization_id = ? AND fr.request_status = 'PENDING'";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, providerOrgId);
        return count != null ? count : 0L;
    }

    public List<FoodRequest> findPendingByProviderOrgId(Long orgId, int page, int size) {
        int offset = Math.max(0, page) * size;
        String sql = "SELECT fr.*, fl.food_name, fl.unit, o.organization_name AS recipient_name " +
                     "FROM food_requests fr " +
                     "JOIN food_listings fl ON fr.listing_id = fl.listing_id " +
                     "JOIN organizations o ON fr.recipient_organization_id = o.organization_id " +
                     "WHERE fl.provider_organization_id = ? AND fr.request_status = 'PENDING' " +
                     "ORDER BY fr.created_at ASC LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, foodRequestRowMapper, orgId, size, offset);
    }
}
