package com.foodbridge.repository;

import com.foodbridge.model.CollectionRecord;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;
import java.util.Optional;

@Repository
public class PickupRepository {

    private final JdbcTemplate jdbcTemplate;

    public PickupRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<CollectionRecord> collectionRecordRowMapper = (rs, rowNum) -> {
        CollectionRecord record = CollectionRecord.builder()
                .collectionId(rs.getLong("collection_id"))
                .allocationId(rs.getLong("allocation_id"))
                .pickupDeadline(rs.getTimestamp("pickup_deadline") != null ? rs.getTimestamp("pickup_deadline").toLocalDateTime() : null)
                .collectionStatus(rs.getString("collection_status"))
                .providerConfirmed(rs.getObject("provider_confirmed") != null && rs.getBoolean("provider_confirmed"))
                .providerConfirmedAt(rs.getTimestamp("provider_confirmed_at") != null ? rs.getTimestamp("provider_confirmed_at").toLocalDateTime() : null)
                .recipientConfirmed(rs.getObject("recipient_confirmed") != null && rs.getBoolean("recipient_confirmed"))
                .recipientConfirmedAt(rs.getTimestamp("recipient_confirmed_at") != null ? rs.getTimestamp("recipient_confirmed_at").toLocalDateTime() : null)
                .collectedAt(rs.getTimestamp("collected_at") != null ? rs.getTimestamp("collected_at").toLocalDateTime() : null)
                .notes(rs.getString("notes"))
                .createdAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null)
                .updatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null)
                .build();

        try {
            record.setFoodName(rs.getString("food_name"));
        } catch (SQLException ignored) {}

        try {
            record.setProviderName(rs.getString("provider_name"));
        } catch (SQLException ignored) {}

        try {
            record.setRecipientName(rs.getString("recipient_name"));
        } catch (SQLException ignored) {}

        try {
            record.setAllocatedQuantity(rs.getBigDecimal("allocated_quantity"));
        } catch (SQLException ignored) {}

        try {
            record.setUnit(rs.getString("unit"));
        } catch (SQLException ignored) {}

        return record;
    };

    public Long save(CollectionRecord record) {
        String sql = "INSERT INTO collections (allocation_id, pickup_deadline, collection_status, " +
                     "provider_confirmed, provider_confirmed_at, recipient_confirmed, recipient_confirmed_at, " +
                     "collected_at, notes, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, record.getAllocationId());
            if (record.getPickupDeadline() != null) {
                ps.setTimestamp(2, java.sql.Timestamp.valueOf(record.getPickupDeadline()));
            } else {
                ps.setNull(2, Types.TIMESTAMP);
            }
            ps.setString(3, record.getCollectionStatus() != null ? record.getCollectionStatus() : "SCHEDULED");
            ps.setBoolean(4, Boolean.TRUE.equals(record.getProviderConfirmed()));
            if (record.getProviderConfirmedAt() != null) {
                ps.setTimestamp(5, java.sql.Timestamp.valueOf(record.getProviderConfirmedAt()));
            } else {
                ps.setNull(5, Types.TIMESTAMP);
            }
            ps.setBoolean(6, Boolean.TRUE.equals(record.getRecipientConfirmed()));
            if (record.getRecipientConfirmedAt() != null) {
                ps.setTimestamp(7, java.sql.Timestamp.valueOf(record.getRecipientConfirmedAt()));
            } else {
                ps.setNull(7, Types.TIMESTAMP);
            }
            if (record.getCollectedAt() != null) {
                ps.setTimestamp(8, java.sql.Timestamp.valueOf(record.getCollectedAt()));
            } else {
                ps.setNull(8, Types.TIMESTAMP);
            }
            ps.setString(9, record.getNotes());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            record.setCollectionId(key.longValue());
            return key.longValue();
        }
        return null;
    }

    public Optional<CollectionRecord> findById(Long id) {
        String sql = "SELECT c.*, fl.food_name, fl.unit, a.allocated_quantity, po.organization_name AS provider_name, ro.organization_name AS recipient_name " +
                     "FROM collections c " +
                     "JOIN allocations a ON c.allocation_id = a.allocation_id " +
                     "JOIN food_listings fl ON a.listing_id = fl.listing_id " +
                     "JOIN organizations po ON fl.provider_organization_id = po.organization_id " +
                     "JOIN organizations ro ON a.recipient_organization_id = ro.organization_id " +
                     "WHERE c.collection_id = ?";
        try {
            CollectionRecord record = jdbcTemplate.queryForObject(sql, collectionRecordRowMapper, id);
            return Optional.ofNullable(record);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<CollectionRecord> findByAllocationId(Long allocationId) {
        String sql = "SELECT c.*, fl.food_name, fl.unit, a.allocated_quantity, po.organization_name AS provider_name, ro.organization_name AS recipient_name " +
                     "FROM collections c " +
                     "JOIN allocations a ON c.allocation_id = a.allocation_id " +
                     "JOIN food_listings fl ON a.listing_id = fl.listing_id " +
                     "JOIN organizations po ON fl.provider_organization_id = po.organization_id " +
                     "JOIN organizations ro ON a.recipient_organization_id = ro.organization_id " +
                     "WHERE c.allocation_id = ?";
        try {
            CollectionRecord record = jdbcTemplate.queryForObject(sql, collectionRecordRowMapper, allocationId);
            return Optional.ofNullable(record);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public List<CollectionRecord> findByProviderOrgId(Long orgId, int page, int size) {
        int offset = Math.max(0, page) * size;
        String sql = "SELECT c.*, fl.food_name, fl.unit, a.allocated_quantity, po.organization_name AS provider_name, ro.organization_name AS recipient_name " +
                     "FROM collections c " +
                     "JOIN allocations a ON c.allocation_id = a.allocation_id " +
                     "JOIN food_listings fl ON a.listing_id = fl.listing_id " +
                     "JOIN organizations po ON fl.provider_organization_id = po.organization_id " +
                     "JOIN organizations ro ON a.recipient_organization_id = ro.organization_id " +
                     "WHERE fl.provider_organization_id = ? " +
                     "ORDER BY c.created_at DESC LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, collectionRecordRowMapper, orgId, size, offset);
    }

    public List<CollectionRecord> findByRecipientOrgId(Long orgId, int page, int size) {
        int offset = Math.max(0, page) * size;
        String sql = "SELECT c.*, fl.food_name, fl.unit, a.allocated_quantity, po.organization_name AS provider_name, ro.organization_name AS recipient_name " +
                     "FROM collections c " +
                     "JOIN allocations a ON c.allocation_id = a.allocation_id " +
                     "JOIN food_listings fl ON a.listing_id = fl.listing_id " +
                     "JOIN organizations po ON fl.provider_organization_id = po.organization_id " +
                     "JOIN organizations ro ON a.recipient_organization_id = ro.organization_id " +
                     "WHERE a.recipient_organization_id = ? " +
                     "ORDER BY c.created_at DESC LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, collectionRecordRowMapper, orgId, size, offset);
    }

    public void confirmProvider(Long collectionId) {
        String sql = "UPDATE collections SET provider_confirmed = TRUE, provider_confirmed_at = NOW(), updated_at = NOW() WHERE collection_id = ?";
        jdbcTemplate.update(sql, collectionId);
    }

    public void confirmRecipient(Long collectionId) {
        String sql = "UPDATE collections SET recipient_confirmed = TRUE, recipient_confirmed_at = NOW(), updated_at = NOW() WHERE collection_id = ?";
        jdbcTemplate.update(sql, collectionId);
    }

    public void markCollected(Long collectionId) {
        String sql = "UPDATE collections SET collection_status = 'COLLECTED', collected_at = NOW(), updated_at = NOW() WHERE collection_id = ?";
        jdbcTemplate.update(sql, collectionId);
    }

    public void updateStatus(Long collectionId, String status) {
        String sql = "UPDATE collections SET collection_status = ?, updated_at = NOW() WHERE collection_id = ?";
        jdbcTemplate.update(sql, status, collectionId);
    }

    public long countByStatus(String status) {
        String sql = "SELECT COUNT(*) FROM collections WHERE collection_status = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, status);
        return count != null ? count : 0L;
    }
}
