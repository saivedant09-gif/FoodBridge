package com.foodbridge.repository;

import com.foodbridge.model.Issue;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class IssueRepository {

    private final JdbcTemplate jdbcTemplate;

    public IssueRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Issue> issueRowMapper = (rs, rowNum) -> Issue.builder()
            .issueId(rs.getLong("issue_id"))
            .reportedBy(rs.getLong("reported_by"))
            .relatedListingId(rs.getObject("related_listing_id") != null ? rs.getLong("related_listing_id") : null)
            .relatedAllocationId(rs.getObject("related_allocation_id") != null ? rs.getLong("related_allocation_id") : null)
            .issueType(rs.getString("issue_type"))
            .description(rs.getString("description"))
            .issueStatus(rs.getString("issue_status"))
            .resolutionNotes(rs.getString("resolution_notes"))
            .createdAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null)
            .resolvedAt(rs.getTimestamp("resolved_at") != null ? rs.getTimestamp("resolved_at").toLocalDateTime() : null)
            .build();

    public List<Issue> findAll(int page, int size) {
        int offset = Math.max(0, page) * size;
        String sql = "SELECT * FROM issues ORDER BY created_at DESC LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, issueRowMapper, size, offset);
    }

    public List<Issue> findAll() {
        String sql = "SELECT * FROM issues ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, issueRowMapper);
    }

    public Optional<Issue> findById(Long issueId) {
        String sql = "SELECT * FROM issues WHERE issue_id = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, issueRowMapper, issueId));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public long count() {
        String sql = "SELECT COUNT(*) FROM issues";
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count != null ? count : 0L;
    }
}
