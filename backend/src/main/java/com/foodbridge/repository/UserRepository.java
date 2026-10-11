package com.foodbridge.repository;

import com.foodbridge.model.User;
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
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<User> userRowMapper = (rs, rowNum) -> User.builder()
            .userId(rs.getLong("user_id"))
            .organizationId(rs.getObject("organization_id") != null ? rs.getLong("organization_id") : null)
            .fullName(rs.getString("full_name"))
            .email(rs.getString("email"))
            .passwordHash(rs.getString("password_hash"))
            .phone(rs.getString("phone"))
            .role(rs.getString("role"))
            .accountStatus(rs.getString("account_status"))
            .createdAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null)
            .updatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null)
            .build();

    public Optional<User> findById(Long userId) {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        try {
            User user = jdbcTemplate.queryForObject(sql, userRowMapper, userId);
            return Optional.ofNullable(user);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<User> findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        try {
            User user = jdbcTemplate.queryForObject(sql, userRowMapper, email);
            return Optional.ofNullable(user);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Long save(User user) {
        String sql = "INSERT INTO users (organization_id, full_name, email, password_hash, phone, role, " +
                     "account_status, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            if (user.getOrganizationId() != null) {
                ps.setLong(1, user.getOrganizationId());
            } else {
                ps.setNull(1, Types.BIGINT);
            }
            ps.setString(2, user.getFullName());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getPasswordHash());
            ps.setString(5, user.getPhone());
            ps.setString(6, user.getRole());
            ps.setString(7, user.getAccountStatus() != null ? user.getAccountStatus() : "ACTIVE");
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            user.setUserId(key.longValue());
            return key.longValue();
        }
        return null;
    }

    public void update(User user) {
        String sql = "UPDATE users SET full_name = ?, phone = ?, role = ?, account_status = ?, updated_at = NOW() " +
                     "WHERE user_id = ?";
        jdbcTemplate.update(sql,
                user.getFullName(),
                user.getPhone(),
                user.getRole(),
                user.getAccountStatus(),
                user.getUserId()
        );
    }

    public List<User> findByOrganizationId(Long orgId) {
        String sql = "SELECT * FROM users WHERE organization_id = ?";
        return jdbcTemplate.query(sql, userRowMapper, orgId);
    }

    public long countByRole(String role) {
        String sql = "SELECT COUNT(*) FROM users WHERE role = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, role);
        return count != null ? count : 0L;
    }

    public void updateStatus(Long userId, String status) {
        String sql = "UPDATE users SET account_status = ?, updated_at = NOW() WHERE user_id = ?";
        jdbcTemplate.update(sql, status, userId);
    }

    public List<User> findAll(int page, int size) {
        int offset = Math.max(0, page) * size;
        String sql = "SELECT * FROM users ORDER BY created_at DESC LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, userRowMapper, size, offset);
    }

    public long count() {
        String sql = "SELECT COUNT(*) FROM users";
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count != null ? count : 0L;
    }
}
