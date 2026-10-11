package com.foodbridge.repository;

import com.foodbridge.model.Notification;
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
public class NotificationRepository {

    private final JdbcTemplate jdbcTemplate;

    public NotificationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Notification> notificationRowMapper = (rs, rowNum) -> Notification.builder()
            .notificationId(rs.getLong("notification_id"))
            .userId(rs.getLong("user_id"))
            .title(rs.getString("title"))
            .message(rs.getString("message"))
            .notificationType(rs.getString("notification_type"))
            .isRead(rs.getBoolean("is_read"))
            .entityType(rs.getString("entity_type"))
            .entityId(rs.getObject("entity_id") != null ? rs.getLong("entity_id") : null)
            .createdAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null)
            .build();

    public Long save(Notification notification) {
        String sql = "INSERT INTO notifications (user_id, title, message, notification_type, is_read, " +
                     "entity_type, entity_id, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, NOW())";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, notification.getUserId());
            ps.setString(2, notification.getTitle());
            ps.setString(3, notification.getMessage());
            ps.setString(4, notification.getNotificationType());
            ps.setBoolean(5, Boolean.TRUE.equals(notification.getIsRead()));
            ps.setString(6, notification.getEntityType());
            if (notification.getEntityId() != null) {
                ps.setLong(7, notification.getEntityId());
            } else {
                ps.setNull(7, Types.BIGINT);
            }
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            notification.setNotificationId(key.longValue());
            return key.longValue();
        }
        return null;
    }

    public Optional<Notification> findById(Long id) {
        String sql = "SELECT * FROM notifications WHERE notification_id = ?";
        try {
            Notification notification = jdbcTemplate.queryForObject(sql, notificationRowMapper, id);
            return Optional.ofNullable(notification);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public List<Notification> findByUserId(Long userId, int page, int size) {
        int offset = Math.max(0, page) * size;
        String sql = "SELECT * FROM notifications WHERE user_id = ? ORDER BY created_at DESC LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, notificationRowMapper, userId, size, offset);
    }

    public long countByUserId(Long userId) {
        String sql = "SELECT COUNT(*) FROM notifications WHERE user_id = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, userId);
        return count != null ? count : 0L;
    }

    public long countUnreadByUserId(Long userId) {
        String sql = "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = FALSE";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, userId);
        return count != null ? count : 0L;
    }

    public void markRead(Long notificationId, Long userId) {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE notification_id = ? AND user_id = ?";
        jdbcTemplate.update(sql, notificationId, userId);
    }

    public void markAllRead(Long userId) {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE user_id = ?";
        jdbcTemplate.update(sql, userId);
    }
}
