package com.kinplatform.platform.infrastructure.outbox;

import com.kinplatform.common.eventbus.domain.OutboxRecord;
import com.kinplatform.common.eventbus.domain.OutboxStatus;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.UUID;

public class OutboxRecordRowMapper implements RowMapper<OutboxRecord> {

    @Override
    public OutboxRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
        return OutboxRecord.builder()
                .id(UUID.fromString(rs.getString("id")))
                .aggregateId(UUID.fromString(rs.getString("aggregate_id")))
                .eventType(rs.getString("event_type"))
                .payload(rs.getString("payload"))
                .metadata(rs.getString("metadata"))
                .status(OutboxStatus.valueOf(rs.getString("status")))
                .retryCount(rs.getInt("retry_count"))
                .createdAt(rs.getObject("created_at", OffsetDateTime.class))
                .publishedAt(rs.getObject("published_at", OffsetDateTime.class))
                .lastError(rs.getString("last_error"))
                .build();
    }
}

