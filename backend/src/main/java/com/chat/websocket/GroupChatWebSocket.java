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
        if (groupMemberMapper == null) return false;
        try {
            return groupMemberMapper.isMember(Long.valueOf(groupId), Long.valueOf(userId));
        } catch (Exception e) {
            return false;
        }
    }

    /** Check if a group member has muted the group */
    public static boolean isMemberMuted(String userId, String groupId) {
        if (groupMemberMapper == null) return false;
        try {
            return groupMemberMapper.isMuted(Long.valueOf(groupId), Long.valueOf(userId));
        } catch (Exception e) {
            return false;
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

    /**
     * Broadcast a message to all online group members, skipping muted members.
     */
    public static void broadcast(String groupId, String jsonMsg) {
        ONLINE_USERS.forEach((uid, session) -> {
            if (!session.isOpen()) {
                ONLINE_USERS.remove(uid);
                return;
            }
            if (!isUserInGroup(uid, groupId)) return;
            // Skip muted members for regular messages
            if (isMemberMuted(uid, groupId)) {
                log.debug("跳过免打扰成员 userId={} groupId={}", uid, groupId);
                return;
            }
            try {
                session.getBasicRemote().sendText(jsonMsg);
            } catch (IOException e) {
                log.warn("推送失败 userId={}", uid, e);
            }
        });
    }

    /**
     * Broadcast to ALL online group members regardless of mute status.
     * Used for group voice call announcements.
     */
    public static void broadcastAll(String groupId, String jsonMsg) {
        ONLINE_USERS.forEach((uid, session) -> {
            if (!session.isOpen()) {
                ONLINE_USERS.remove(uid);
                return;
            }
            if (!isUserInGroup(uid, groupId)) return;
            try {
                session.getBasicRemote().sendText(jsonMsg);
            } catch (IOException e) {
                log.warn("推送失败 userId={}", uid, e);
            }
        });
    }

    /**
     * Send a message to a specific user. Returns true if delivered.
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

            // Group voice call signaling
            if ("group_call_start".equals(type)) {
                String groupId = msg.getString("groupId");
                String callRoomId = msg.getString("callRoomId");
                msg.put("fromUserId", userId);
                // Broadcast call start to all online group members (ignoring mute — calls override mute)
                broadcastAll(groupId, msg.toJSONString());
                log.info("群语音通话发起 groupId={} callRoomId={} by={}", groupId, callRoomId, userId);
                return;
            }

            if ("group_call_accept".equals(type)) {
                String toUserId = msg.getString("toUserId");
                msg.put("fromUserId", userId);
                if (toUserId != null && !toUserId.isEmpty()) {
                    sendToUser(toUserId, msg.toJSONString());
                }
                log.info("群语音通话接受 callRoomId={} by={}", msg.getString("callRoomId"), userId);
                return;
            }

            if ("group_call_reject".equals(type)) {
                String toUserId = msg.getString("toUserId");
                msg.put("fromUserId", userId);
                if (toUserId != null && !toUserId.isEmpty()) {
                    sendToUser(toUserId, msg.toJSONString());
                }
                log.info("群语音通话拒绝 callRoomId={} by={}", msg.getString("callRoomId"), userId);
                return;
            }

            if ("group_call_join".equals(type)) {
                String toUserId = msg.getString("toUserId");
                msg.put("fromUserId", userId);
                if (toUserId != null) {
                    sendToUser(toUserId, msg.toJSONString());
                }
                return;
            }

            if ("group_call_leave".equals(type) || "group_call_end".equals(type)) {
                String groupId = msg.getString("groupId");
                msg.put("fromUserId", userId);
                // Notify all online members in the group
                broadcastAll(groupId, msg.toJSONString());
                return;
            }

            // Voice call signaling relay (1-on-1 WebRTC)
            String toUserId = msg.getString("toUserId");
            if (toUserId != null && !toUserId.isEmpty()) {
                if (!msg.containsKey("fromUserId")) {
                    msg.put("fromUserId", userId);
                }
                boolean delivered = sendToUser(toUserId, msg.toJSONString());
                if (delivered) {
                    log.debug("信令消息已转发 type={} {} -> {}", type, userId, toUserId);
                } else {
                    log.debug("目标用户不在线 type={} {} -> {}", type, userId, toUserId);
                    JSONObject offlineMsg = new JSONObject();
                    offlineMsg.put("type", "call_failed");
                    offlineMsg.put("fromUserId", toUserId);
                    offlineMsg.put("toUserId", userId);
                    offlineMsg.put("reason", "用户不在线");
                    sendToUser(userId, offlineMsg.toJSONString());
                }
                return;
            }

            log.debug("未识别的消息类型: {}", type);
        } catch (Exception e) {
            log.warn("解析 WebSocket 消息失败 userId={}", userId, e);
        }
    }
}
