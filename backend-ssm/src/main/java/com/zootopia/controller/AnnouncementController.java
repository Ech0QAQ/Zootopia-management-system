package com.zootopia.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zootopia.common.Result;
import com.zootopia.entity.Announcement;
import com.zootopia.entity.AnnouncementComment;
import com.zootopia.entity.User;
import com.zootopia.mapper.AnnouncementCommentMapper;
import com.zootopia.mapper.AnnouncementMapper;
import com.zootopia.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/announcements")
public class AnnouncementController {
    @Autowired
    private AnnouncementMapper announcementMapper;
    @Autowired
    private AnnouncementCommentMapper announcementCommentMapper;
    @Autowired
    private UserMapper userMapper;

    @GetMapping
    public Result<List<Announcement>> getAnnouncements() {
        List<Announcement> announcements = announcementMapper.selectList(
                new LambdaQueryWrapper<Announcement>()
                        .orderByDesc(Announcement::getCreatedAt)
        );
        return Result.success(announcements);
    }

    @PostMapping
    public Result<Map<String, Integer>> createAnnouncement(@RequestBody Announcement announcement, HttpServletRequest request) {
        String role = (String) request.getAttribute("userRole");
        if (!"admin".equals(role) && !"worker".equals(role)) {
            return Result.error(403, "暂无权限");
        }

        if (announcement.getTitle() == null || announcement.getContent() == null || announcement.getCategory() == null) {
            return Result.error("字段不完整");
        }

        String username = (String) request.getAttribute("username");
        announcement.setCreatedBy(username);
        announcement.setArea(announcement.getArea() != null ? announcement.getArea() : "ALL");
        announcement.setEmergency(announcement.getEmergency() != null ? announcement.getEmergency() : false);
        announcement.setExpired(false);

        announcementMapper.insert(announcement);
        return Result.success(Map.of("id", announcement.getId()));
    }

    @PutMapping("/{id}")
    public Result<String> updateAnnouncement(@PathVariable Integer id, @RequestBody Announcement announcement, HttpServletRequest request) {
        String role = (String) request.getAttribute("userRole");
        if (!"admin".equals(role) && !"worker".equals(role)) {
            return Result.error(403, "暂无权限");
        }

        Announcement existing = announcementMapper.selectById(id);
        if (existing == null) {
            return Result.error(404, "未找到公告");
        }

        if (announcement.getTitle() != null) existing.setTitle(announcement.getTitle());
        if (announcement.getContent() != null) existing.setContent(announcement.getContent());
        if (announcement.getCategory() != null) existing.setCategory(announcement.getCategory());
        if (announcement.getArea() != null) existing.setArea(announcement.getArea());
        if (announcement.getEmergency() != null) existing.setEmergency(announcement.getEmergency());
        if (announcement.getExpired() != null) existing.setExpired(announcement.getExpired());

        announcementMapper.updateById(existing);
        return Result.success("更新成功");
    }

    @DeleteMapping("/{id}")
    public Result<String> deleteAnnouncement(@PathVariable Integer id, HttpServletRequest request) {
        String role = (String) request.getAttribute("userRole");
        if (!"admin".equals(role) && !"worker".equals(role)) {
            return Result.error(403, "暂无权限");
        }

        announcementMapper.deleteById(id);
        return Result.success("已删除");
    }

    @GetMapping("/{id}/comments")
    public Result<List<AnnouncementComment>> getComments(@PathVariable Integer id) {
        List<AnnouncementComment> comments = announcementCommentMapper.selectList(
                new LambdaQueryWrapper<AnnouncementComment>()
                        .eq(AnnouncementComment::getAnnouncementId, id)
                        .orderByAsc(AnnouncementComment::getCreatedAt)
        );
        // 关联用户名
        for (AnnouncementComment comment : comments) {
            User user = userMapper.selectById(comment.getUserId());
            if (user != null) {
                comment.setName(user.getName());
            }
        }
        return Result.success(comments);
    }

    @PostMapping("/{id}/comments")
    public Result<Map<String, Integer>> createComment(@PathVariable Integer id, 
            @RequestBody Map<String, Object> request, HttpServletRequest httpRequest) {
        String role = (String) httpRequest.getAttribute("userRole");
        if (!"resident".equals(role)) {
            return Result.error(403, "暂无权限");
        }

        String content = (String) request.get("content");
        if (content == null || content.isEmpty()) {
            return Result.error("评论不能为空");
        }

        AnnouncementComment comment = new AnnouncementComment();
        comment.setAnnouncementId(id);
        comment.setUserId((Integer) httpRequest.getAttribute("userId"));
        comment.setContent(content);
        comment.setAnonymous(request.get("anonymous") != null && (Boolean) request.get("anonymous"));

        announcementCommentMapper.insert(comment);
        return Result.success(Map.of("id", comment.getId()));
    }
}

