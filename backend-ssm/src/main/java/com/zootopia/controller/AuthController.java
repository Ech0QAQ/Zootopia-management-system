package com.zootopia.controller;

import com.zootopia.common.Result;
import com.zootopia.config.AdminConfig;
import com.zootopia.entity.User;
import com.zootopia.mapper.UserMapper;
import com.zootopia.util.JwtUtil;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping
public class AuthController {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private AdminConfig adminConfig;

    @GetMapping("/health")
    public Result<Map<String, String>> health() {
        Map<String, String> data = new HashMap<>();
        data.put("status", "ok");
        return Result.success(data);
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginRequest request) {
        String username = request.getUsername();
        String password = request.getPassword();

        if (username == null || password == null) {
            return Result.error("用户名和密码不能为空");
        }

        // 管理员登录
        if (adminConfig.getUsername().equals(username)) {
            if (!adminConfig.getPassword().equals(password)) {
                return Result.error(401, "用户名或密码错误");
            }
            Map<String, Object> tokenData = new HashMap<>();
            tokenData.put("id", 0);
            tokenData.put("role", "admin");
            tokenData.put("username", adminConfig.getUsername());
            tokenData.put("name", "动物城管理员");

            String token = jwtUtil.generateToken(tokenData);
            Map<String, Object> result = new HashMap<>();
            result.put("token", token);
            result.put("id", 0);
            result.put("role", "admin");
            result.put("username", adminConfig.getUsername());
            result.put("name", "动物城管理员");
            return Result.success(result);
        }

        // 普通用户登录
        User user = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .eq(User::getUsername, username)
        );

        if (user == null || !user.getPassword().equals(password)) {
            return Result.error(401, "用户名或密码错误");
        }

        if ("worker".equals(user.getRole()) && "已离职".equals(user.getEmploymentStatus())) {
            return Result.error(403, "抱歉，您已离职，账号已被回收！");
        }

        Map<String, Object> tokenData = new HashMap<>();
        tokenData.put("id", user.getId());
        tokenData.put("role", user.getRole());
        tokenData.put("username", user.getUsername());
        tokenData.put("name", user.getName());
        tokenData.put("employment_status", user.getEmploymentStatus());

        String token = jwtUtil.generateToken(tokenData);
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("id", user.getId());
        result.put("role", user.getRole());
        result.put("username", user.getUsername());
        result.put("name", user.getName());
        // 同时返回两种格式，确保兼容性
        result.put("employment_status", user.getEmploymentStatus());
        result.put("employmentStatus", user.getEmploymentStatus());
        result.put("work_area", user.getWorkArea());
        result.put("live_area", user.getLiveArea());
        return Result.success(result);
    }

    @PostMapping("/register")
    public Result<String> register(@RequestBody RegisterRequest request) {
        if (request.getUsername() == null || request.getPassword() == null || request.getRole() == null) {
            return Result.error("用户名、密码和角色必填");
        }

        if (!"worker".equals(request.getRole()) && !"resident".equals(request.getRole())) {
            return Result.error("只能注册工作人员或居民");
        }

        // 检查用户名是否已存在
        User existUser = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .eq(User::getUsername, request.getUsername())
        );
        if (existUser != null) {
            return Result.error(409, "该账户已存在，请更换用户名");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        user.setRole(request.getRole());
        user.setName(request.getName() != null ? request.getName() : "");
        user.setAge(request.getAge());
        user.setGender(request.getGender() != null ? request.getGender() : "");
        user.setAnimalType(request.getAnimalType() != null ? request.getAnimalType() : "");
        user.setLiveArea(request.getLiveArea() != null ? request.getLiveArea() : "");
        user.setHousehold(request.getHousehold() != null ? request.getHousehold() : "");
        user.setWorkArea(request.getWorkArea() != null ? request.getWorkArea() : "");
        user.setDepartment(request.getDepartment() != null ? request.getDepartment() : "");
        user.setEmploymentStatus("worker".equals(request.getRole()) ? "待审批" : "在职");

        userMapper.insert(user);
        return Result.success("注册成功");
    }

    @Data
    static class LoginRequest {
        private String username;
        private String password;
    }

    @Data
    static class RegisterRequest {
        private String username;
        private String password;
        private String role;
        private String name;
        private Integer age;
        private String gender;
        private String animalType;
        private String liveArea;
        private String household;
        private String workArea;
        private String department;
    }
}

