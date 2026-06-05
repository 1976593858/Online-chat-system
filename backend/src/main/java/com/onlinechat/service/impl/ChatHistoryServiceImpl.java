package com.onlinechat.service.impl;

import com.onlinechat.common.PageResult;
import com.onlinechat.service.ChatHistoryService;
import com.onlinechat.vo.ChatHistorySearchVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ChatHistoryServiceImpl implements ChatHistoryService {

    private final JdbcTemplate jdbcTemplate;

    public ChatHistoryServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public PageResult<ChatHistorySearchVO> search(Long currentUserId, String keyword, Long userId, String fromDate, String toDate, long pageNo, long pageSize) {
        StringBuilder where = new StringBuilder("WHERE m.deleted = 0");
        Object[] params = buildParams(currentUserId, keyword, userId, fromDate, toDate, where);

        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM private_message m " + where,
                params, Long.class);
        if (total == null) total = 0L;

        long offset = (pageNo - 1) * pageSize;
        String querySql = "SELECT m.* FROM private_message m " + where +
                " ORDER BY m.created_at DESC LIMIT ? OFFSET ?";

        Object[] queryParams = new Object[params.length + 2];
        System.arraycopy(params, 0, queryParams, 0, params.length);
        queryParams[queryParams.length - 2] = pageSize;
        queryParams[queryParams.length - 1] = offset;

        List<ChatHistorySearchVO> records = jdbcTemplate.query(querySql, queryParams, (rs, rowNum) -> {
            ChatHistorySearchVO vo = new ChatHistorySearchVO();
            vo.setId(rs.getLong("id"));
            vo.setMessageId(rs.getLong("id"));
            vo.setFromUserId(rs.getLong("from_user_id"));
            vo.setToUserId(rs.getLong("to_user_id"));
            vo.setContent(rs.getString("content"));
            vo.setMessageType(rs.getString("message_type"));
            vo.setCreatedAt(rs.getTimestamp("created_at") != null ?
                    rs.getTimestamp("created_at").toLocalDateTime() : null);
            return vo;
        });

        long pages = (total + pageSize - 1) / pageSize;
        PageResult<ChatHistorySearchVO> result = new PageResult<>();
        result.setRecords(records);
        result.setTotal(total);
        result.setPageNo(pageNo);
        result.setPageSize(pageSize);
        result.setPages(pages);
        return result;
    }

    private Object[] buildParams(Long currentUserId, String keyword, Long userId, String fromDate, String toDate, StringBuilder where) {
        List<Object> paramList = new ArrayList<>();

        if (keyword != null && !keyword.isBlank()) {
            String escaped = keyword.trim().replaceAll("['\"\\\\]", " ");
            where.append(" AND MATCH(m.content) AGAINST(? IN BOOLEAN MODE)");
            paramList.add(escaped + "*");
        }

        if (userId != null) {
            where.append(" AND (m.from_user_id = ? OR m.to_user_id = ?)");
            paramList.add(userId);
            paramList.add(userId);
        }
        if (fromDate != null && !fromDate.isBlank()) {
            where.append(" AND m.created_at >= ?");
            paramList.add(fromDate.trim());
        }
        if (toDate != null && !toDate.isBlank()) {
            where.append(" AND m.created_at <= ?");
            paramList.add(toDate.trim() + " 23:59:59");
        }

        where.append(" AND (m.from_user_id = ? OR m.to_user_id = ?)");
        paramList.add(currentUserId);
        paramList.add(currentUserId);

        return paramList.toArray();
    }
}
