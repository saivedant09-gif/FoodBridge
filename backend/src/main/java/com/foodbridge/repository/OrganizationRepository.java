package com.foodbridge.repository;

import com.foodbridge.model.Organization;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;
import java.util.Optional;

@Repository
public class OrganizationRepository {

    private final JdbcTemplate jdbcTemplate;

    public OrganizationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Organization> organizationRowMapper = (rs, rowNum) -> Organization.builder()
            .organizationId(rs.getLong("organization_id"))
            .organizationName(rs.getString("organization_name"))
            .organizationType(rs.getString("organization_type"))
            .description(rs.getString("description"))
            .contactEmail(rs.getString("contact_email"))
            .contactPhone(rs.getString("contact_phone"))
            .address(rs.getString("address"))
            .city(rs.getString("city"))
            .state(rs.getString("state"))
            .pincode(rs.getString("pincode"))
            .latitude(rs.getObject("latitude") != null ? rs.getDouble("latitude") : null)
            .longitude(rs.getObject("longitude") != null ? rs.getDouble("longitude") : null)
            .verificationStatus(rs.getString("verification_status"))
            .verifiedAt(rs.getTimestamp("verified_at") != null ? rs.getTimestamp("verified_at").toLocalDateTime() : null)
            .verifiedBy(rs.getObject("verified_by") != null ? rs.getLong("verified_by") : null)
            .createdAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null)
            .updatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null)
            .build();

    public Optional<Organization> findById(Long orgId) {
        String sql = "SELECT * FROM organizations WHERE organization_id = ?";
        try {
            Organization org = jdbcTemplate.queryForObject(sql, organizationRowMapper, orgId);
            return Optional.ofNullable(org);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Long save(Organization org) {
        String sql = "INSERT INTO organizations (organization_name, organization_type, description, contact_email, " +
                     "contact_phone, address, city, state, pincode, latitude, longitude, verification_status, " +
                     "verified_at, verified_by, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, org.getOrganizationName());
            ps.setString(2, org.getOrganizationType());
            ps.setString(3, org.getDescription());
            ps.setString(4, org.getContactEmail());
            ps.setString(5, org.getContactPhone());
            ps.setString(6, org.getAddress());
            ps.setString(7, org.getCity());
            ps.setString(8, org.getState());
            ps.setString(9, org.getPincode());
            if (org.getLatitude() != null) {
                ps.setDouble(10, org.getLatitude());
            } else {
                ps.setNull(10, Types.DECIMAL);
            }
            if (org.getLongitude() != null) {
                ps.setDouble(11, org.getLongitude());
            } else {
                ps.setNull(11, Types.DECIMAL);
            }
            ps.setString(12, org.getVerificationStatus() != null ? org.getVerificationStatus() : "PENDING");
            if (org.getVerifiedAt() != null) {
                ps.setTimestamp(13, java.sql.Timestamp.valueOf(org.getVerifiedAt()));
            } else {
                ps.setNull(13, Types.TIMESTAMP);
            }
            if (org.getVerifiedBy() != null) {
                ps.setLong(14, org.getVerifiedBy());
            } else {
                ps.setNull(14, Types.BIGINT);
            }
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            org.setOrganizationId(key.longValue());
            return key.longValue();
        }
        return null;
    }

    public void update(Organization org) {
        String sql = "UPDATE organizations SET organization_name = ?, organization_type = ?, description = ?, " +
                     "contact_email = ?, contact_phone = ?, address = ?, city = ?, state = ?, pincode = ?, " +
                     "latitude = ?, longitude = ?, updated_at = NOW() WHERE organization_id = ?";
        jdbcTemplate.update(sql,
                org.getOrganizationName(),
                org.getOrganizationType(),
                org.getDescription(),
                org.getContactEmail(),
                org.getContactPhone(),
                org.getAddress(),
                org.getCity(),
                org.getState(),
                org.getPincode(),
                org.getLatitude(),
                org.getLongitude(),
                org.getOrganizationId()
        );
    }

    public void updateVerificationStatus(Long orgId, String status, Long verifiedBy) {
        String sql = "UPDATE organizations SET verification_status = ?, verified_by = ?, verified_at = NOW(), updated_at = NOW() " +
                     "WHERE organization_id = ?";
        jdbcTemplate.update(sql, status, verifiedBy, orgId);
    }

    public List<Organization> findByStatus(String status, int page, int size) {
        int offset = Math.max(0, page) * size;
        String sql = "SELECT * FROM organizations WHERE verification_status = ? ORDER BY created_at DESC LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, organizationRowMapper, status, size, offset);
    }

    public Optional<Organization> findByUserId(Long userId) {
        String sql = "SELECT o.* FROM organizations o JOIN users u ON o.organization_id = u.organization_id WHERE u.user_id = ?";
        try {
            Organization org = jdbcTemplate.queryForObject(sql, organizationRowMapper, userId);
            return Optional.ofNullable(org);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public long countByStatus(String status) {
        String sql = "SELECT COUNT(*) FROM organizations WHERE verification_status = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, status);
        return count != null ? count : 0L;
    }
}
