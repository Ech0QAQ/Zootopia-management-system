package com.zootopia.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zootopia.common.Result;
import com.zootopia.entity.LeaveRequest;
import com.zootopia.entity.User;
import com.zootopia.mapper.LeaveRequestMapper;
import com.zootopia.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping
public class LeaveRequestController {
    @Autowired
    private LeaveRequestMapper leaveRequestMapper;
    @Autowired
    private UserMapper userMapper;

    @PostMapping("/admin/leave-requests/{workerId}")
    @Transactional
    public Result<String> createLeaveRequestByAdmin(@PathVariable Integer workerId) {
        User worker = userMapper.selectById(workerId);
        if (worker == null) {
            return Result.error(404, "员工不存在");
        }
        if (!"休假".equals(worker.getEmploymentStatus())) {
            return Result.error("该员工不在休假状态，无法发起结束休假请求");
        }

        LeaveRequest request = new LeaveRequest();
        request.setWorkerId(workerId);
        request.setInitiator("admin");
        request.setStatus("待员工确认");
        request.setNote("");
        leaveRequestMapper.insert(request);

        return Result.success("已发起结束休假请求");
    }

    @PostMapping("/worker/leave-requests")
    @Transactional
    public Result<String> createLeaveRequestByWorker(HttpServletRequest httpRequest) {
        Integer userId = (Integer) httpRequest.getAttribute("userId");
        User worker = userMapper.selectById(userId);
        if (worker == null) {
            return Result.error(404, "用户不存在");
        }
        if (!"休假".equals(worker.getEmploymentStatus())) {
            return Result.error("当前不在休假状态，无需申请结束休假");
        }

        // 员工主动结束休假，直接更新状态为在职，不需要管理员确认
        worker.setEmploymentStatus("在职");
        userMapper.updateById(worker);

        return Result.success("已结束休假，状态已更新为在职");
    }

    @GetMapping("/worker/leave-requests")
    public Result<List<LeaveRequest>> getWorkerLeaveRequests(HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        User currentUser = userMapper.selectById(userId);
        
        List<LeaveRequest> requests = leaveRequestMapper.selectList(
                new LambdaQueryWrapper<LeaveRequest>()
                        .eq(LeaveRequest::getWorkerId, userId)
                        .orderByDesc(LeaveRequest::getCreatedAt)
        );
        
        // 过滤掉无效的请求：如果员工已经不在休假状态，但请求状态还是"待管理员确认"（旧逻辑的遗留），则过滤掉
        requests = requests.stream().filter(req -> {
            // 如果是员工发起的"待管理员确认"请求，但员工当前不在休假状态，说明已经通过其他方式结束了休假，过滤掉
            if ("worker".equals(req.getInitiator()) && "待管理员确认".equals(req.getStatus())) {
                if (currentUser != null && !"休假".equals(currentUser.getEmploymentStatus())) {
                    return false; // 员工已经不在休假状态，这个请求已经无效
                }
            }
            return true;
        }).collect(Collectors.toList());
        
        for (LeaveRequest req : requests) {
            User user = userMapper.selectById(req.getWorkerId());
            if (user != null) {
                req.setName(user.getName());
                req.setUsername(user.getUsername());
            }
        }
        return Result.success(requests);
    }

    @GetMapping("/admin/leave-requests")
    public Result<List<LeaveRequest>> getAdminLeaveRequests() {
        List<LeaveRequest> requests = leaveRequestMapper.selectList(
                new LambdaQueryWrapper<LeaveRequest>()
                        .orderByDesc(LeaveRequest::getCreatedAt)
        );
        
        for (LeaveRequest req : requests) {
            User user = userMapper.selectById(req.getWorkerId());
            if (user != null) {
                req.setName(user.getName());
                req.setUsername(user.getUsername());
            }
        }
        return Result.success(requests);
    }

    @PostMapping("/worker/leave-requests/{id}/respond")
    @Transactional
    public Result<String> respondByWorker(@PathVariable Integer id, @RequestBody Map<String, Boolean> request, HttpServletRequest httpRequest) {
        Integer userId = (Integer) httpRequest.getAttribute("userId");
        LeaveRequest leaveRequest = leaveRequestMapper.selectOne(
                new LambdaQueryWrapper<LeaveRequest>()
                        .eq(LeaveRequest::getId, id)
                        .eq(LeaveRequest::getWorkerId, userId)
        );

        if (leaveRequest == null) {
            return Result.error(404, "请求不存在");
        }
        if (!"待员工确认".equals(leaveRequest.getStatus())) {
            return Result.error("当前状态不可回应");
        }

        Boolean agree = request.get("agree");
        leaveRequest.setStatus(agree ? "员工同意" : "员工拒绝");
        leaveRequestMapper.updateById(leaveRequest);

        if (agree != null && agree) {
            User worker = userMapper.selectById(userId);
            worker.setEmploymentStatus("在职");
            userMapper.updateById(worker);
        }

        return Result.success("已处理");
    }

    @PostMapping("/admin/leave-requests/{id}/respond")
    @Transactional
    public Result<String> respondByAdmin(@PathVariable Integer id, @RequestBody Map<String, Boolean> request) {
        LeaveRequest leaveRequest = leaveRequestMapper.selectById(id);
        if (leaveRequest == null) {
            return Result.error(404, "请求不存在");
        }
        if (!"待管理员确认".equals(leaveRequest.getStatus())) {
            return Result.error("当前状态不可处理");
        }

        Boolean agree = request.get("agree");
        leaveRequest.setStatus(agree ? "管理员同意" : "管理员拒绝");
        leaveRequestMapper.updateById(leaveRequest);

        if (agree != null && agree) {
            User worker = userMapper.selectById(leaveRequest.getWorkerId());
            worker.setEmploymentStatus("在职");
            userMapper.updateById(worker);
        }

        return Result.success("已处理");
    }
}

