package com.zootopia.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zootopia.common.Result;
import com.zootopia.entity.Message;
import com.zootopia.mapper.MessageMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/messages")
public class MessageController {
    @Autowired
    private MessageMapper messageMapper;

    @PostMapping
    public Result<Map<String, Integer>> createMessage(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        String role = (String) httpRequest.getAttribute("userRole");
        if (!"resident".equals(role)) {
            return Result.error(403, "暂无权限");
        }

        String content = request.get("content");
        if (content == null || content.isEmpty()) {
            return Result.error("内容不能为空");
        }

        Integer userId = (Integer) httpRequest.getAttribute("userId");
        String userName = (String) httpRequest.getAttribute("userName");
        String username = (String) httpRequest.getAttribute("username");

        Message message = new Message();
        message.setUserId(userId);
        message.setUserName(userName != null ? userName : username);
        message.setContent(content);
        message.setStatus("待受理");

        messageMapper.insert(message);
        return Result.success(Map.of("id", message.getId()));
    }

    @GetMapping("/my")
    public Result<List<Message>> getMyMessages(HttpServletRequest request) {
        String role = (String) request.getAttribute("userRole");
        if (!"resident".equals(role)) {
            return Result.error(403, "暂无权限");
        }

        Integer userId = (Integer) request.getAttribute("userId");
        List<Message> messages = messageMapper.selectList(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getUserId, userId)
                        .orderByDesc(Message::getCreatedAt)
        );
        return Result.success(messages);
    }

    @GetMapping("/all")
    public Result<List<Message>> getAllMessages(HttpServletRequest request) {
        String role = (String) request.getAttribute("userRole");
        if (!"admin".equals(role) && !"worker".equals(role)) {
            return Result.error(403, "暂无权限");
        }

        List<Message> messages = messageMapper.selectList(
                new LambdaQueryWrapper<Message>()
                        .orderByDesc(Message::getCreatedAt)
        );
        return Result.success(messages);
    }

    @PostMapping("/{id}/accept")
    public Result<String> acceptMessage(@PathVariable Integer id, HttpServletRequest request) {
        String role = (String) request.getAttribute("userRole");
        if (!"admin".equals(role) && !"worker".equals(role)) {
            return Result.error(403, "暂无权限");
        }

        Message message = messageMapper.selectById(id);
        if (message == null) {
            return Result.error(404, "留言不存在");
        }

        message.setStatus("处理中");
        messageMapper.updateById(message);
        return Result.success("已受理");
    }

    @PostMapping("/{id}/save")
    public Result<String> saveReply(@PathVariable Integer id, @RequestBody Map<String, String> request) {
        Message message = messageMapper.selectById(id);
        if (message == null) {
            return Result.error(404, "留言不存在");
        }

        message.setReply(request.get("reply"));
        messageMapper.updateById(message);
        return Result.success("已保存草稿");
    }

    @PostMapping("/{id}/submit")
    public Result<String> submitReply(@PathVariable Integer id, @RequestBody Map<String, String> request) {
        Message message = messageMapper.selectById(id);
        if (message == null) {
            return Result.error(404, "留言不存在");
        }

        message.setReply(request.get("reply"));
        message.setStatus("已解决");
        messageMapper.updateById(message);
        return Result.success("已提交答复");
    }

    @PostMapping("/{id}/score")
    public Result<String> scoreMessage(@PathVariable Integer id, @RequestBody Map<String, Integer> request, HttpServletRequest httpRequest) {
        String role = (String) httpRequest.getAttribute("userRole");
        if (!"resident".equals(role)) {
            return Result.error(403, "暂无权限");
        }

        Integer userId = (Integer) httpRequest.getAttribute("userId");
        Message message = messageMapper.selectOne(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getId, id)
                        .eq(Message::getUserId, userId)
        );

        if (message == null) {
            return Result.error(404, "留言不存在");
        }

        message.setScore(request.get("score"));
        messageMapper.updateById(message);
        return Result.success("评分成功");
    }
}

