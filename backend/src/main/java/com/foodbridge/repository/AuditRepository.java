package com.foodbridge.repository;

import com.foodbridge.model.AuditLog;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.util.List;

@Repository
public class AuditRepository {

    private final JdbcTemplate jdbcTemplate;

    public AuditRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<AuditLog> auditLogRowMapper = (rs, rowNum) -> AuditLog.builder()
            .auditId(rs.getLong("audit_id"))
            .actorUserId(rs.getObject("actor_user_id") != null ? rs.getLong("actor_user_id") : null)
            .action(rs.getString("action"))
            .entityType(rs.getString("entity_type"))
            .entityId(rs.getString("entity_id"))
            .metadata(rs.getString("metadata"))
            .ipAddress(rs.getString("ip_address"))
            .createdAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null)
            .build();

    public void save(AuditLog log) {
        String meta = log.getMetadata();
        if (meta == null || meta.isBlank()) {
            meta = "{}";
        } else if (!meta.trim().startsWith("{") && !meta.trim().startsWith("[")) {
            meta = "{\"note\":\"" + meta.replace("\"", "\\\"") + "\"}";
        }
        String sql = "INSERT INTO audit_log (actor_user_id, action, entity_type, entity_id, metadata, ip_address, created_at) " +
                     "VALUES (?, ?, ?, ?, CAST(? AS JSON), ?, NOW())";
        jdbcTemplate.update(sql,
                log.getActorUserId(),
                log.getAction(),
                log.getEntityType(),
                log.getEntityId(),
                meta,
                log.getIpAddress()
        );
    }

    public List<AuditLog> findAll(int page, int size) {
        int offset = Math.max(0, page) * size;
        String sql = "SELECT * FROM audit_log ORDER BY created_at DESC LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, auditLogRowMapper, size, offset);
    }

    public List<AuditLog> findByEntityType(String entityType, int page, int size) {
        int offset = Math.max(0, page) * size;
        String sql = "SELECT * FROM audit_log WHERE entity_type = ? ORDER BY created_at DESC LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, auditLogRowMapper, entityType, size, offset);
    }

    public long count() {
        String sql = "SELECT COUNT(*) FROM audit_log";
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count != null ? count : 0L;
    }
}
