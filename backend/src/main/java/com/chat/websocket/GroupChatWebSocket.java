package com.chat.websocket;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 成员D：群聊 WebSocket
 * 路径：ws://localhost:8080/ws/{userId}
 */
@Component
@ServerEndpoint("/ws/{userId}")
public class GroupChatWebSocket {

    private static final Logger log = LoggerFactory.getLogger(GroupChatWebSocket.class);

    /** 在线用户：userId -> Session */
    private static final ConcurrentHashMap<String, Session> ONLINE_USERS = new ConcurrentHashMap<>();

    /** 判断用户是否在群里（成员B做完后改成查库） */
    public static boolean isUserInGroup(String userId, String groupId) {
        return true; // 先放行，方便测试
    }

    /** ✅ 新增：获取群内在线成员（Controller 要用） */
    public static List<String> getOnlineMembers(String groupId) {
        List<String> onlineMembers = new ArrayList<>();
        ONLINE_USERS.forEach((uid, session) -> {
            if (session.isOpen() && isUserInGroup(uid, groupId)) {
                onlineMembers.add(uid);
            }
        });
        return onlineMembers;
    }

    /** 群聊广播 */
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

    @OnOpen
    public void onOpen(Session session, @PathParam("userId") String userId) {
        ONLINE_USERS.put(userId, session);
        log.info("🟢 用户连接: {} | 在线数: {}", userId, ONLINE_USERS.size());
    }

    @OnClose
    public void onClose(@PathParam("userId") String userId) {
        ONLINE_USERS.remove(userId);
        log.info("🔴 用户断开: {} | 在线数: {}", userId, ONLINE_USERS.size());
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error("WebSocket 错误", error);
    }

    @OnMessage
    public void onMessage(String message, @PathParam("userId") String userId) {
        log.info("收到消息 userId={} msg={}", userId, message);
    }
}