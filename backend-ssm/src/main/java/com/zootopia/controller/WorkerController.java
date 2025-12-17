package com.zootopia.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zootopia.common.Result;
import com.zootopia.entity.SalaryHistory;
import com.zootopia.entity.User;
import com.zootopia.entity.WorkerApplication;
import com.zootopia.mapper.SalaryHistoryMapper;
import com.zootopia.mapper.UserMapper;
import com.zootopia.mapper.WorkerApplicationMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/worker")
public class WorkerController {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private SalaryHistoryMapper salaryHistoryMapper;
    @Autowired
    private WorkerApplicationMapper workerApplicationMapper;

    @GetMapping("/profile")
    public Result<User> getProfile(HttpServletRequest request) {
        String role = (String) request.getAttribute("userRole");
        if (!"worker".equals(role)) {
            return Result.error(403, "暂无权限");
        }

        Integer userId = (Integer) request.getAttribute("userId");
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getId, userId)
                        .eq(User::getRole, "worker")
        );
        if (user == null) {
            return Result.error(404, "用户不存在");
        }
        return Result.success(user);
    }

    @GetMapping("/salary-history")
    public Result<List<SalaryHistory>> getSalaryHistory(HttpServletRequest request) {
        String role = (String) request.getAttribute("userRole");
        if (!"worker".equals(role)) {
            return Result.error(403, "暂无权限");
        }

        Integer userId = (Integer) request.getAttribute("userId");
        List<SalaryHistory> history = salaryHistoryMapper.selectList(
                new LambdaQueryWrapper<SalaryHistory>()
                        .eq(SalaryHistory::getWorkerId, userId)
                        .orderByAsc(SalaryHistory::getCreatedAt)
        );
        return Result.success(history);
    }

    @GetMapping("/applications")
    public Result<List<WorkerApplication>> getApplications(HttpServletRequest request) {
        String role = (String) request.getAttribute("userRole");
        if (!"worker".equals(role)) {
            return Result.error(403, "暂无权限");
        }

        Integer userId = (Integer) request.getAttribute("userId");
        List<WorkerApplication> applications = workerApplicationMapper.selectList(
                new LambdaQueryWrapper<WorkerApplication>()
                        .eq(WorkerApplication::getWorkerId, userId)
                        .orderByDesc(WorkerApplication::getCreatedAt)
        );
        return Result.success(applications);
    }

    @PostMapping("/apply")
    public Result<Map<String, Integer>> createApplication(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        String role = (String) httpRequest.getAttribute("userRole");
        if (!"worker".equals(role)) {
            return Result.error(403, "暂无权限");
        }

        String type = request.get("type");
        if (type == null || type.isEmpty()) {
            return Result.error("申请类型必填");
        }

        Integer userId = (Integer) httpRequest.getAttribute("userId");
        WorkerApplication application = new WorkerApplication();
        application.setWorkerId(userId);
        application.setType(type);
        application.setReason(request.get("reason"));
        application.setStatus("待审批");

        workerApplicationMapper.insert(application);
        return Result.success(Map.of("id", application.getId()));
    }
}

