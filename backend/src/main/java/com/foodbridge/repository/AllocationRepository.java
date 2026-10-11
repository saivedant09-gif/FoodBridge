package com.foodbridge.repository;

import com.foodbridge.model.Allocation;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;
import java.util.Optional;

@Repository
public class AllocationRepository {

    private final JdbcTemplate jdbcTemplate;

    public AllocationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Allocation> allocationRowMapper = (rs, rowNum) -> {
        Allocation a = Allocation.builder()
                .allocationId(rs.getLong("allocation_id"))
                .requestId(rs.getLong("request_id"))
                .listingId(rs.getLong("listing_id"))
                .recipientOrganizationId(rs.getLong("recipient_organization_id"))
                .allocatedQuantity(rs.getBigDecimal("allocated_quantity"))
                .allocationStatus(rs.getString("allocation_status"))
                .confirmedAt(rs.getTimestamp("confirmed_at") != null ? rs.getTimestamp("confirmed_at").toLocalDateTime() : null)
                .cancelledAt(rs.getTimestamp("cancelled_at") != null ? rs.getTimestamp("cancelled_at").toLocalDateTime() : null)
                .cancellationReason(rs.getString("cancellation_reason"))
                .createdAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null)
                .updatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null)
                .build();

        try {
            a.setFoodName(rs.getString("food_name"));
        } catch (SQLException ignored) {}

        try {
            a.setRecipientName(rs.getString("recipient_name"));
        } catch (SQLException ignored) {}

        try {
            a.setProviderName(rs.getString("provider_name"));
        } catch (SQLException ignored) {}

        try {
            a.setUnit(rs.getString("unit"));
        } catch (SQLException ignored) {}

        return a;
    };

    public Long save(Allocation allocation) {
        String sql = "INSERT INTO allocations (request_id, listing_id, recipient_organization_id, " +
                     "allocated_quantity, allocation_status, confirmed_at, cancelled_at, cancellation_reason, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, allocation.getRequestId());
            ps.setLong(2, allocation.getListingId());
            ps.setLong(3, allocation.getRecipientOrganizationId());
            ps.setBigDecimal(4, allocation.getAllocatedQuantity());
            ps.setString(5, allocation.getAllocationStatus() != null ? allocation.getAllocationStatus() : "CONFIRMED");
            if (allocation.getConfirmedAt() != null) {
                ps.setTimestamp(6, java.sql.Timestamp.valueOf(allocation.getConfirmedAt()));
            } else {
                ps.setNull(6, Types.TIMESTAMP);
            }
            if (allocation.getCancelledAt() != null) {
                ps.setTimestamp(7, java.sql.Timestamp.valueOf(allocation.getCancelledAt()));
            } else {
                ps.setNull(7, Types.TIMESTAMP);
            }
            ps.setString(8, allocation.getCancellationReason());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            allocation.setAllocationId(key.longValue());
            return key.longValue();
        }
        return null;
    }

    public Optional<Allocation> findById(Long id) {
        String sql = "SELECT a.*, fl.food_name, fl.unit, po.organization_name AS provider_name, ro.organization_name AS recipient_name " +
                     "FROM allocations a " +
                     "JOIN food_listings fl ON a.listing_id = fl.listing_id " +
                     "JOIN organizations po ON fl.provider_organization_id = po.organization_id " +
                     "JOIN organizations ro ON a.recipient_organization_id = ro.organization_id " +
                     "WHERE a.allocation_id = ?";
        try {
            Allocation allocation = jdbcTemplate.queryForObject(sql, allocationRowMapper, id);
            return Optional.ofNullable(allocation);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<Allocation> findByRequestId(Long requestId) {
        String sql = "SELECT a.*, fl.food_name, fl.unit, po.organization_name AS provider_name, ro.organization_name AS recipient_name " +
                     "FROM allocations a " +
                     "JOIN food_listings fl ON a.listing_id = fl.listing_id " +
                     "JOIN organizations po ON fl.provider_organization_id = po.organization_id " +
                     "JOIN organizations ro ON a.recipient_organization_id = ro.organization_id " +
                     "WHERE a.request_id = ?";
        try {
            Allocation allocation = jdbcTemplate.queryForObject(sql, allocationRowMapper, requestId);
            return Optional.ofNullable(allocation);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public List<Allocation> findByRecipientOrgId(Long orgId, int page, int size) {
        int offset = Math.max(0, page) * size;
        String sql = "SELECT a.*, fl.food_name, fl.unit, po.organization_name AS provider_name, ro.organization_name AS recipient_name " +
                     "FROM allocations a " +
                     "JOIN food_listings fl ON a.listing_id = fl.listing_id " +
                     "JOIN organizations po ON fl.provider_organization_id = po.organization_id " +
                     "JOIN organizations ro ON a.recipient_organization_id = ro.organization_id " +
                     "WHERE a.recipient_organization_id = ? " +
                     "ORDER BY a.created_at DESC LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, allocationRowMapper, orgId, size, offset);
    }

    public List<Allocation> findByListingId(Long listingId) {
        String sql = "SELECT a.*, fl.food_name, fl.unit, po.organization_name AS provider_name, ro.organization_name AS recipient_name " +
                     "FROM allocations a " +
                     "JOIN food_listings fl ON a.listing_id = fl.listing_id " +
                     "JOIN organizations po ON fl.provider_organization_id = po.organization_id " +
                     "JOIN organizations ro ON a.recipient_organization_id = ro.organization_id " +
                     "WHERE a.listing_id = ? " +
                     "ORDER BY a.created_at ASC";
        return jdbcTemplate.query(sql, allocationRowMapper, listingId);
    }

    public void updateStatus(Long allocationId, String status) {
        String sql = "UPDATE allocations SET allocation_status = ?, updated_at = NOW() WHERE allocation_id = ?";
        jdbcTemplate.update(sql, status, allocationId);
    }

    public void cancel(Long allocationId, String reason) {
        String sql = "UPDATE allocations SET allocation_status = 'CANCELLED', cancelled_at = NOW(), " +
                     "cancellation_reason = ?, updated_at = NOW() WHERE allocation_id = ?";
        jdbcTemplate.update(sql, reason, allocationId);
    }

    public long countByStatus(String status) {
        String sql = "SELECT COUNT(*) FROM allocations WHERE allocation_status = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, status);
        return count != null ? count : 0L;
    }

    public BigDecimal sumAllocatedByListingId(Long listingId) {
        String sql = "SELECT COALESCE(SUM(allocated_quantity), 0) FROM allocations " +
                     "WHERE listing_id = ? AND allocation_status IN ('CONFIRMED', 'COMPLETED')";
        BigDecimal sum = jdbcTemplate.queryForObject(sql, BigDecimal.class, listingId);
        return sum != null ? sum : BigDecimal.ZERO;
    }
}
