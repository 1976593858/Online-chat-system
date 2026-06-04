package com.onlinechat.service.impl;

import com.onlinechat.common.PageResult;
import com.onlinechat.entity.PrivateMessage;
import com.onlinechat.mapper.PrivateMessageMapper;
import com.onlinechat.mapper.UserMapper;
import com.onlinechat.service.ChatHistoryService;
import com.onlinechat.vo.ChatHistorySearchVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ChatHistoryServiceImpl implements ChatHistoryService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JdbcTemplate sqliteJdbcTemplate;
    private final UserMapper userMapper;
    private final PrivateMessageMapper privateMessageMapper;

    public ChatHistoryServiceImpl(@Qualifier("sqliteJdbcTemplate") JdbcTemplate sqliteJdbcTemplate,
                                   UserMapper userMapper,
                                   PrivateMessageMapper privateMessageMapper) {
        this.sqliteJdbcTemplate = sqliteJdbcTemplate;
        this.userMapper = userMapper;
        this.privateMessageMapper = privateMessageMapper;
    }

    @Override
    public void syncMessage(PrivateMessage message) {
        try {
            String now = LocalDateTime.now().format(FMT);
            String createdAt = message.getCreatedAt() != null ? message.getCreatedAt().format(FMT) : now;
            sqliteJdbcTemplate.update(
                "INSERT OR IGNORE INTO chat_message (message_id, from_user_id, to_user_id, content, message_type, created_at, synced_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                message.getId(), message.getFromUserId(), message.getToUserId(),
                message.getContent(), message.getMessageType(), createdAt, now
            );
        } catch (Exception e) {
            log.warn("sync message to sqlite failed, messageId={}", message.getId(), e);
        }
    }

    @Override
    public PageResult<ChatHistorySearchVO> search(Long currentUserId, String keyword, Long userId, String fromDate, String toDate, long pageNo, long pageSize) {
        List<Object> params = new ArrayList<>();
        StringBuilder ftsQuery = new StringBuilder();
        StringBuilder where = new StringBuilder();

        if (keyword != null && !keyword.isBlank()) {
            String escaped = keyword.trim().replaceAll("['\"\\\\]", " ");
            ftsQuery.append("chat_message_fts MATCH ?");
            params.add(escaped + "*");
        } else {
            ftsQuery.append("1=1");
        }

        if (userId != null) {
            where.append(" AND (m.from_user_id = ? OR m.to_user_id = ?)");
            params.add(userId);
            params.add(userId);
        }
        if (fromDate != null && !fromDate.isBlank()) {
            where.append(" AND m.created_at >= ?");
            params.add(fromDate.trim());
        }
        if (toDate != null && !toDate.isBlank()) {
            where.append(" AND m.created_at <= ?");
            params.add(toDate.trim() + " 23:59:59");
        }

        String baseSql = "FROM chat_message m INNER JOIN chat_message_fts fts ON m.id = fts.rowid WHERE " + ftsQuery + where;

        Long total = sqliteJdbcTemplate.queryForObject("SELECT COUNT(*) " + baseSql, params.toArray(), Long.class);
        if (total == null) total = 0L;

        long offset = (pageNo - 1) * pageSize;
        List<Object> pageParams = new ArrayList<>(params);
        pageParams.add(pageSize);
        pageParams.add(offset);

        String querySql = "SELECT m.* " + baseSql + " ORDER BY m.created_at DESC LIMIT ? OFFSET ?";
        List<Map<String, Object>> rows = sqliteJdbcTemplate.queryForList(querySql, pageParams.toArray());

        List<ChatHistorySearchVO> records = rows.stream().map(this::mapRow).collect(Collectors.toList());

        enrichUserInfo(records);

        long pages = (total + pageSize - 1) / pageSize;
        PageResult<ChatHistorySearchVO> result = new PageResult<>();
        result.setRecords(records);
        result.setTotal(total);
        result.setPageNo(pageNo);
        result.setPageSize(pageSize);
        result.setPages(pages);
        return result;
    }

    @Override
    public long syncExistingMessages() {
        long count = 0;
        try {
            List<PrivateMessage> messages = privateMessageMapper.selectList(null);
            for (PrivateMessage msg : messages) {
                syncMessage(msg);
                count++;
            }
            log.info("synced {} existing messages to sqlite", count);
        } catch (Exception e) {
            log.warn("sync existing messages failed", e);
        }
        return count;
    }

    private ChatHistorySearchVO mapRow(Map<String, Object> row) {
        ChatHistorySearchVO vo = new ChatHistorySearchVO();
        vo.setId(row.get("id") != null ? ((Number) row.get("id")).longValue() : null);
        vo.setMessageId(row.get("message_id") != null ? ((Number) row.get("message_id")).longValue() : null);
        vo.setFromUserId(row.get("from_user_id") != null ? ((Number) row.get("from_user_id")).longValue() : null);
        vo.setToUserId(row.get("to_user_id") != null ? ((Number) row.get("to_user_id")).longValue() : null);
        vo.setContent(row.get("content") != null ? (String) row.get("content") : null);
        vo.setMessageType(row.get("message_type") != null ? (String) row.get("message_type") : null);
        if (row.get("created_at") != null) {
            vo.setCreatedAt(LocalDateTime.parse((String) row.get("created_at"), FMT));
        }
        return vo;
    }

    private void enrichUserInfo(List<ChatHistorySearchVO> records) {
        for (ChatHistorySearchVO vo : records) {
            if (vo.getFromUserId() != null) {
                var user = userMapper.selectById(vo.getFromUserId());
                if (user != null) {
                    vo.setFromUserUsername(user.getUsername());
                    vo.setFromUserNickname(user.getNickname());
                }
            }
            if (vo.getToUserId() != null) {
                var user = userMapper.selectById(vo.getToUserId());
                if (user != null) {
                    vo.setToUserUsername(user.getUsername());
                    vo.setToUserNickname(user.getNickname());
                }
            }
        }
    }
}
