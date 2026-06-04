package com.chat.websocket;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.chat.mapper.GroupMemberMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ServerEndpoint("/ws/{userId}")
public class GroupChatWebSocket {

    private static final Logger log = LoggerFactory.getLogger(GroupChatWebSocket.class);

    private static final ConcurrentHashMap<String, Session> ONLINE_USERS = new ConcurrentHashMap<>();

    private static GroupMemberMapper groupMemberMapper;

    @Autowired
    public void setGroupMemberMapper(GroupMemberMapper mapper) {
        GroupChatWebSocket.groupMemberMapper = mapper;
    }

    public static boolean isUserOnline(String userId) {
        Session session = ONLINE_USERS.get(userId);
        return session != null && session.isOpen();
    }

    public static boolean isUserInGroup(String userId, String groupId) {
        if (groupMemberMapper == null) {
            return true;
        }
        try {
            return groupMemberMapper.isMember(Long.valueOf(groupId), Long.valueOf(userId));
        } catch (Exception e) {
            log.warn("查询群成员失败 groupId={} userId={}", groupId, userId, e);
            return true;
        }
    }

    public static List<String> getOnlineMembers(String groupId) {
        List<String> onlineMembers = new ArrayList<>();
        ONLINE_USERS.forEach((uid, session) -> {
            if (session.isOpen() && isUserInGroup(uid, groupId)) {
                onlineMembers.add(uid);
            }
        });
        return onlineMembers;
    }

    public static void broadcast(String groupId, String jsonMsg) {
        ONLINE_USERS.forEach((uid, session) -> {
            if (!session.isOpen()) {
                ONLINE_USERS.remove(uid);
                return;
            }
            if (isUserInGroup(uid, groupId)) {
                try {
                    session.getBasicRemote().sendText(jsonMsg);
                } catch (IOException e) {
                    log.warn("推送失败 userId={}", uid, e);
                }
            }
        });
    }

    /**
     * 向指定用户发送消息（用于语音通话信令转发）
     */
    public static boolean sendToUser(String userId, String jsonMsg) {
        Session session = ONLINE_USERS.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.getBasicRemote().sendText(jsonMsg);
                return true;
            } catch (IOException e) {
                log.warn("向用户 {} 发送消息失败", userId, e);
                return false;
            }
        }
        return false;
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("userId") String userId) {
        ONLINE_USERS.put(userId, session);
        log.info("用户连接: {} | 在线数: {}", userId, ONLINE_USERS.size());
    }

    @OnClose
    public void onClose(@PathParam("userId") String userId) {
        ONLINE_USERS.remove(userId);
        log.info("用户断开: {} | 在线数: {}", userId, ONLINE_USERS.size());
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error("WebSocket 错误", error);
    }

    @OnMessage
    public void onMessage(String message, @PathParam("userId") String userId) {
        log.debug("收到 WebSocket 消息 userId={} length={}", userId, message.length());
        try {
            JSONObject msg = JSON.parseObject(message);
            String type = msg.getString("type");
            String toUserId = msg.getString("toUserId");

            if (toUserId != null && !toUserId.isEmpty()) {
                // 信令消息：转发给目标用户
                if (!msg.containsKey("fromUserId")) {
                    msg.put("fromUserId", userId);
                }
                boolean delivered = sendToUser(toUserId, msg.toJSONString());
                if (delivered) {
                    log.debug("信令消息已转发 type={} {} -> {}", type, userId, toUserId);
                } else {
                    log.debug("目标用户不在线 type={} {} -> {}", type, userId, toUserId);
                    // 通知发送方目标用户不在线
                    JSONObject offlineMsg = new JSONObject();
                    offlineMsg.put("type", "call_failed");
                    offlineMsg.put("fromUserId", toUserId);
                    offlineMsg.put("toUserId", userId);
                    offlineMsg.put("reason", "用户不在线");
                    sendToUser(userId, offlineMsg.toJSONString());
                }
            } else {
                log.debug("未识别的消息类型: {}", type);
            }
        } catch (Exception e) {
            log.warn("解析 WebSocket 消息失败 userId={}", userId, e);
        }
    }
}
