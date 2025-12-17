package com.zootopia.controller;

import com.zootopia.common.Result;
import com.zootopia.entity.User;
import com.zootopia.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping
public class UserController {
    @Autowired
    private UserMapper userMapper;

    @GetMapping("/me")
    public Result<User> getCurrentUser(HttpServletRequest request) {
        String role = (String) request.getAttribute("userRole");
        Integer userId = (Integer) request.getAttribute("userId");

        if ("admin".equals(role)) {
            User admin = new User();
            admin.setId(0);
            admin.setRole("admin");
            admin.setUsername("admin");
            admin.setName("动物城管理员");
            return Result.success(admin);
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }
        return Result.success(user);
    }

    @PutMapping("/resident/profile")
    public Result<String> updateResidentProfile(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        String role = (String) httpRequest.getAttribute("userRole");
        if (!"resident".equals(role)) {
            return Result.error(403, "暂无权限");
        }

        Integer userId = (Integer) httpRequest.getAttribute("userId");
        User user = userMapper.selectById(userId);
        if (user == null || !"resident".equals(user.getRole())) {
            return Result.error(404, "未找到该居民");
        }

        boolean updated = false;
        if (request.containsKey("live_area")) {
            user.setLiveArea(request.get("live_area"));
            updated = true;
        }
        if (request.containsKey("household")) {
            user.setHousehold(request.get("household"));
            updated = true;
        }

        if (!updated) {
            return Result.error("没有需要更新的字段");
        }

        userMapper.updateById(user);
        return Result.success("更新成功");
    }

    @PutMapping("/resident/password")
    public Result<String> changePassword(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        String role = (String) httpRequest.getAttribute("userRole");
        if (!"resident".equals(role)) {
            return Result.error(403, "暂无权限");
        }

        String currentPassword = request.get("currentPassword");
        String newPassword = request.get("newPassword");
        if (currentPassword == null || newPassword == null) {
            return Result.error("当前密码和新密码不能为空");
        }

        Integer userId = (Integer) httpRequest.getAttribute("userId");
        User user = userMapper.selectById(userId);
        if (user == null || !"resident".equals(user.getRole())) {
            return Result.error(404, "用户不存在");
        }

        if (!user.getPassword().equals(currentPassword)) {
            return Result.error(401, "当前密码错误");
        }

        user.setPassword(newPassword);
        userMapper.updateById(user);
        return Result.success("密码修改成功");
    }
}

