package com.foodbridge.repository;

import com.foodbridge.model.FoodListing;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class FoodListingRepository {

    private final JdbcTemplate jdbcTemplate;

    public FoodListingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<FoodListing> foodListingRowMapper = (rs, rowNum) -> {
        FoodListing listing = FoodListing.builder()
                .listingId(rs.getLong("listing_id"))
                .providerOrganizationId(rs.getLong("provider_organization_id"))
                .foodName(rs.getString("food_name"))
                .description(rs.getString("description"))
                .category(rs.getString("category"))
                .totalQuantity(rs.getBigDecimal("total_quantity"))
                .availableQuantity(rs.getBigDecimal("available_quantity"))
                .unit(rs.getString("unit"))
                .dietaryInfo(rs.getString("dietary_info"))
                .allergenNotes(rs.getString("allergen_notes"))
                .preparedAt(rs.getTimestamp("prepared_at") != null ? rs.getTimestamp("prepared_at").toLocalDateTime() : null)
                .expiresAt(rs.getTimestamp("expires_at") != null ? rs.getTimestamp("expires_at").toLocalDateTime() : null)
                .pickupDeadline(rs.getTimestamp("pickup_deadline") != null ? rs.getTimestamp("pickup_deadline").toLocalDateTime() : null)
                .pickupAddress(rs.getString("pickup_address"))
                .pickupCity(rs.getString("pickup_city"))
                .listingStatus(rs.getString("listing_status"))
                .createdAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null)
                .updatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null)
                .build();

        try {
            listing.setProviderName(rs.getString("provider_name"));
        } catch (SQLException ignored) {
            // provider_name column not present in this query
        }

        return listing;
    };

    public Long save(FoodListing listing) {
        String sql = "INSERT INTO food_listings (provider_organization_id, food_name, description, category, " +
                     "total_quantity, available_quantity, unit, dietary_info, allergen_notes, prepared_at, " +
                     "expires_at, pickup_deadline, pickup_address, pickup_city, listing_status, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, listing.getProviderOrganizationId());
            ps.setString(2, listing.getFoodName());
            ps.setString(3, listing.getDescription());
            ps.setString(4, listing.getCategory());
            ps.setBigDecimal(5, listing.getTotalQuantity());
            ps.setBigDecimal(6, listing.getAvailableQuantity() != null ? listing.getAvailableQuantity() : listing.getTotalQuantity());
            ps.setString(7, listing.getUnit());
            ps.setString(8, listing.getDietaryInfo());
            ps.setString(9, listing.getAllergenNotes());
            if (listing.getPreparedAt() != null) {
                ps.setTimestamp(10, java.sql.Timestamp.valueOf(listing.getPreparedAt()));
            } else {
                ps.setNull(10, Types.TIMESTAMP);
            }
            if (listing.getExpiresAt() != null) {
                ps.setTimestamp(11, java.sql.Timestamp.valueOf(listing.getExpiresAt()));
            } else {
                ps.setNull(11, Types.TIMESTAMP);
            }
            if (listing.getPickupDeadline() != null) {
                ps.setTimestamp(12, java.sql.Timestamp.valueOf(listing.getPickupDeadline()));
            } else {
                ps.setNull(12, Types.TIMESTAMP);
            }
            ps.setString(13, listing.getPickupAddress());
            ps.setString(14, listing.getPickupCity());
            ps.setString(15, listing.getListingStatus() != null ? listing.getListingStatus() : "ACTIVE");
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            listing.setListingId(key.longValue());
            return key.longValue();
        }
        return null;
    }

    public Optional<FoodListing> findById(Long id) {
        String sql = "SELECT fl.*, o.organization_name AS provider_name " +
                     "FROM food_listings fl " +
                     "LEFT JOIN organizations o ON fl.provider_organization_id = o.organization_id " +
                     "WHERE fl.listing_id = ?";
        try {
            FoodListing listing = jdbcTemplate.queryForObject(sql, foodListingRowMapper, id);
            return Optional.ofNullable(listing);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public List<FoodListing> findActive(String category, String city, int page, int size) {
        int offset = Math.max(0, page) * size;
        StringBuilder sql = new StringBuilder(
                "SELECT fl.*, o.organization_name AS provider_name " +
                "FROM food_listings fl " +
                "JOIN organizations o ON fl.provider_organization_id = o.organization_id " +
                "WHERE fl.listing_status = 'ACTIVE' " +
                "  AND fl.expires_at > NOW() " +
                "  AND fl.pickup_deadline > NOW() " +
                "  AND fl.available_quantity > 0 "
        );

        List<Object> params = new ArrayList<>();
        if (category != null && !category.trim().isEmpty()) {
            sql.append(" AND fl.category = ? ");
            params.add(category.trim());
        }
        if (city != null && !city.trim().isEmpty()) {
            sql.append(" AND fl.pickup_city = ? ");
            params.add(city.trim());
        }

        sql.append("ORDER BY fl.pickup_deadline ASC LIMIT ? OFFSET ?");
        params.add(size);
        params.add(offset);

        return jdbcTemplate.query(sql.toString(), foodListingRowMapper, params.toArray());
    }

    public List<FoodListing> findByProviderOrgId(Long orgId, int page, int size) {
        int offset = Math.max(0, page) * size;
        String sql = "SELECT fl.*, o.organization_name AS provider_name " +
                     "FROM food_listings fl " +
                     "LEFT JOIN organizations o ON fl.provider_organization_id = o.organization_id " +
                     "WHERE fl.provider_organization_id = ? " +
                     "ORDER BY fl.created_at DESC LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, foodListingRowMapper, orgId, size, offset);
    }

    public void update(FoodListing listing) {
        String sql = "UPDATE food_listings SET food_name = ?, description = ?, category = ?, total_quantity = ?, " +
                     "available_quantity = ?, unit = ?, dietary_info = ?, allergen_notes = ?, prepared_at = ?, " +
                     "expires_at = ?, pickup_deadline = ?, pickup_address = ?, pickup_city = ?, listing_status = ?, " +
                     "updated_at = NOW() WHERE listing_id = ?";
        jdbcTemplate.update(sql,
                listing.getFoodName(),
                listing.getDescription(),
                listing.getCategory(),
                listing.getTotalQuantity(),
                listing.getAvailableQuantity(),
                listing.getUnit(),
                listing.getDietaryInfo(),
                listing.getAllergenNotes(),
                listing.getPreparedAt() != null ? java.sql.Timestamp.valueOf(listing.getPreparedAt()) : null,
                listing.getExpiresAt() != null ? java.sql.Timestamp.valueOf(listing.getExpiresAt()) : null,
                listing.getPickupDeadline() != null ? java.sql.Timestamp.valueOf(listing.getPickupDeadline()) : null,
                listing.getPickupAddress(),
                listing.getPickupCity(),
                listing.getListingStatus(),
                listing.getListingId()
        );
    }

    public int decrementAvailableQuantity(Long listingId, BigDecimal qty) {
        String sql = "UPDATE food_listings SET available_quantity = available_quantity - ?, updated_at = NOW() " +
                     "WHERE listing_id = ? AND available_quantity >= ? AND listing_status = 'ACTIVE'";
        return jdbcTemplate.update(sql, qty, listingId, qty);
    }

    public int incrementAvailableQuantity(Long listingId, BigDecimal qty) {
        String sql = "UPDATE food_listings SET available_quantity = available_quantity + ?, updated_at = NOW() " +
                     "WHERE listing_id = ?";
        return jdbcTemplate.update(sql, qty, listingId);
    }

    public void updateStatus(Long listingId, String status) {
        String sql = "UPDATE food_listings SET listing_status = ?, updated_at = NOW() WHERE listing_id = ?";
        jdbcTemplate.update(sql, status, listingId);
    }

    public int expireOverdueListings() {
        String sql = "UPDATE food_listings SET listing_status = 'EXPIRED', updated_at = NOW() " +
                     "WHERE (expires_at < NOW() OR pickup_deadline < NOW()) AND listing_status = 'ACTIVE'";
        return jdbcTemplate.update(sql);
    }

    public long countByStatus(String status) {
        String sql = "SELECT COUNT(*) FROM food_listings WHERE listing_status = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, status);
        return count != null ? count : 0L;
    }

    public long countByProviderOrgId(Long orgId) {
        String sql = "SELECT COUNT(*) FROM food_listings WHERE provider_organization_id = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, orgId);
        return count != null ? count : 0L;
    }
}
