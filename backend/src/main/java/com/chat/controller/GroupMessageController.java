package com.chat.controller;

import com.alibaba.fastjson.JSONObject;
import com.chat.websocket.GroupChatWebSocket;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequestMapping("/api/group")
public class GroupMessageController {

    /** 发送群消息 */
    @PostMapping("/send")
    public JSONObject send(@RequestBody JSONObject body) {
        String groupId = body.getString("groupId");
        String userId  = body.getString("userId");
        String content = body.getString("content");

        JSONObject msg = new JSONObject();
        msg.put("type", "group_message");
        msg.put("groupId", groupId);
        msg.put("userId", userId);
        msg.put("content", content);
        msg.put("timestamp", System.currentTimeMillis());

        // 广播给群内所有在线用户
        GroupChatWebSocket.broadcast(groupId, msg.toJSONString());

        JSONObject res = new JSONObject();
        res.put("code", 200);
        res.put("msg", "ok");
        return res;
    }

    /** 获取群在线成员（调试用） */
    @GetMapping("/online/{groupId}")
    public JSONObject online(@PathVariable String groupId) {
        JSONObject res = new JSONObject();
        res.put("onlineMembers", GroupChatWebSocket.getOnlineMembers(groupId));
        return res;
    }
}