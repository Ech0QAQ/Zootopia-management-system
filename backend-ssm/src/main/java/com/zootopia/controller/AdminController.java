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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminController {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private SalaryHistoryMapper salaryHistoryMapper;
    @Autowired
    private WorkerApplicationMapper workerApplicationMapper;

    @GetMapping("/workers")
    public Result<List<User>> getWorkers(HttpServletRequest request) {
        String role = (String) request.getAttribute("userRole");
        if (!"admin".equals(role)) {
            return Result.error(403, "暂无权限");
        }

        List<User> workers = userMapper.selectList(
                new LambdaQueryWrapper<User>()
                        .eq(User::getRole, "worker")
        );
        return Result.success(workers);
    }

    @PostMapping("/workers/{id}/approve")
    @Transactional
    public Result<String> approveWorker(@PathVariable Integer id, @RequestBody Map<String, BigDecimal> request) {
        User worker = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getId, id)
                        .eq(User::getRole, "worker")
        );
        if (worker == null) {
            return Result.error(404, "未找到该工作人员");
        }

        BigDecimal salary = request.get("salary");
        worker.setEmploymentStatus("在职");
        if (salary != null) {
            // 检查薪资是否真的发生了变化
            if (worker.getSalary() == null || worker.getSalary().compareTo(salary) != 0) {
                worker.setSalary(salary);
                
                // 检查最新的薪资历史记录，避免重复插入相同的薪资值
                List<SalaryHistory> recentHistory = salaryHistoryMapper.selectList(
                        new LambdaQueryWrapper<SalaryHistory>()
                                .eq(SalaryHistory::getWorkerId, id)
                                .orderByDesc(SalaryHistory::getCreatedAt)
                                .last("LIMIT 1")
                );
                
                // 只有当薪资发生变化，或者最新记录与当前薪资不同时，才插入新记录
                boolean shouldInsert = true;
                if (!recentHistory.isEmpty()) {
                    SalaryHistory latest = recentHistory.get(0);
                    if (latest.getValue().compareTo(salary) == 0) {
                        shouldInsert = false; // 最新记录已经是这个薪资值，不需要重复插入
                    }
                }
                
                if (shouldInsert) {
                    SalaryHistory history = new SalaryHistory();
                    history.setWorkerId(id);
                    history.setValue(salary);
                    salaryHistoryMapper.insert(history);
                }
            }
        }
        userMapper.updateById(worker);

        return Result.success("审批通过");
    }

    @PostMapping("/workers/{id}/fire")
    public Result<String> fireWorker(@PathVariable Integer id) {
        User worker = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getId, id)
                        .eq(User::getRole, "worker")
        );
        if (worker == null) {
            return Result.error(404, "未找到该工作人员");
        }

        worker.setEmploymentStatus("已离职");
        userMapper.updateById(worker);
        return Result.success("已开除该员工");
    }

    @PostMapping("/workers/{id}/update")
    @Transactional
    public Result<String> updateWorker(@PathVariable Integer id, @RequestBody Map<String, Object> request) {
        User worker = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getId, id)
                        .eq(User::getRole, "worker")
        );
        if (worker == null) {
            return Result.error(404, "未找到该工作人员");
        }

        if (request.containsKey("work_area")) {
            worker.setWorkArea((String) request.get("work_area"));
        }
        if (request.containsKey("department")) {
            worker.setDepartment((String) request.get("department"));
        }
        if (request.containsKey("salary")) {
            Object salaryObj = request.get("salary");
            if (salaryObj != null) {
                BigDecimal salary = salaryObj instanceof BigDecimal 
                    ? (BigDecimal) salaryObj 
                    : new BigDecimal(salaryObj.toString());
                
                // 检查薪资是否真的发生了变化
                if (worker.getSalary() == null || worker.getSalary().compareTo(salary) != 0) {
                    worker.setSalary(salary);
                    
                    // 检查最新的薪资历史记录，避免重复插入相同的薪资值
                    List<SalaryHistory> recentHistory = salaryHistoryMapper.selectList(
                            new LambdaQueryWrapper<SalaryHistory>()
                                    .eq(SalaryHistory::getWorkerId, id)
                                    .orderByDesc(SalaryHistory::getCreatedAt)
                                    .last("LIMIT 1")
                    );
                    
                    // 只有当薪资发生变化，或者最新记录与当前薪资不同时，才插入新记录
                    boolean shouldInsert = true;
                    if (!recentHistory.isEmpty()) {
                        SalaryHistory latest = recentHistory.get(0);
                        if (latest.getValue().compareTo(salary) == 0) {
                            shouldInsert = false; // 最新记录已经是这个薪资值，不需要重复插入
                        }
                    }
                    
                    if (shouldInsert) {
                        SalaryHistory history = new SalaryHistory();
                        history.setWorkerId(id);
                        history.setValue(salary);
                        salaryHistoryMapper.insert(history);
                    }
                }
            }
        }
        if (request.containsKey("employment_status")) {
            worker.setEmploymentStatus((String) request.get("employment_status"));
        }

        userMapper.updateById(worker);
        return Result.success("更新成功");
    }

    @GetMapping("/users/residents")
    public Result<List<User>> getResidents(HttpServletRequest request) {
        String role = (String) request.getAttribute("userRole");
        if (!"admin".equals(role) && !"worker".equals(role)) {
            return Result.error(403, "暂无权限");
        }

        List<User> residents = userMapper.selectList(
                new LambdaQueryWrapper<User>()
                        .eq(User::getRole, "resident")
        );
        return Result.success(residents);
    }

    @GetMapping("/worker-applications")
    public Result<List<WorkerApplication>> getWorkerApplications() {
        // 这里需要JOIN查询，简化处理
        List<WorkerApplication> applications = workerApplicationMapper.selectList(
                new LambdaQueryWrapper<WorkerApplication>()
                        .orderByDesc(WorkerApplication::getCreatedAt)
        );
        // 关联用户信息
        for (WorkerApplication app : applications) {
            User user = userMapper.selectById(app.getWorkerId());
            if (user != null) {
                app.setName(user.getName());
                app.setUsername(user.getUsername());
            }
        }
        return Result.success(applications);
    }

    @PostMapping("/worker-applications/{id}/approve")
    @Transactional
    public Result<String> approveApplication(@PathVariable Integer id, @RequestBody Map<String, BigDecimal> request) {
        WorkerApplication application = workerApplicationMapper.selectById(id);
        if (application == null) {
            return Result.error(404, "申请不存在");
        }
        if (!"待审批".equals(application.getStatus())) {
            return Result.error("该申请已处理");
        }

        User worker = userMapper.selectById(application.getWorkerId());
        if (worker == null) {
            return Result.error(404, "员工不存在");
        }

        if ("涨薪申请".equals(application.getType())) {
            BigDecimal salary = request.get("salary");
            if (salary != null) {
                // 检查薪资是否真的发生了变化
                if (worker.getSalary() == null || worker.getSalary().compareTo(salary) != 0) {
                    worker.setSalary(salary);
                    
                    // 检查最新的薪资历史记录，避免重复插入相同的薪资值
                    List<SalaryHistory> recentHistory = salaryHistoryMapper.selectList(
                            new LambdaQueryWrapper<SalaryHistory>()
                                    .eq(SalaryHistory::getWorkerId, worker.getId())
                                    .orderByDesc(SalaryHistory::getCreatedAt)
                                    .last("LIMIT 1")
                    );
                    
                    // 只有当薪资发生变化，或者最新记录与当前薪资不同时，才插入新记录
                    boolean shouldInsert = true;
                    if (!recentHistory.isEmpty()) {
                        SalaryHistory latest = recentHistory.get(0);
                        if (latest.getValue().compareTo(salary) == 0) {
                            shouldInsert = false; // 最新记录已经是这个薪资值，不需要重复插入
                        }
                    }
                    
                    if (shouldInsert) {
                        SalaryHistory history = new SalaryHistory();
                        history.setWorkerId(worker.getId());
                        history.setValue(salary);
                        salaryHistoryMapper.insert(history);
                    }
                }
            }
        } else if ("休假申请".equals(application.getType())) {
            worker.setEmploymentStatus("休假");
        } else if ("离职申请".equals(application.getType())) {
            worker.setEmploymentStatus("已离职");
        }

        userMapper.updateById(worker);
        application.setStatus("已通过");
        workerApplicationMapper.updateById(application);

        return Result.success("已通过");
    }

    @PostMapping("/worker-applications/{id}/reject")
    public Result<String> rejectApplication(@PathVariable Integer id) {
        WorkerApplication application = workerApplicationMapper.selectById(id);
        if (application == null) {
            return Result.error(404, "申请不存在");
        }

        application.setStatus("已拒绝");
        workerApplicationMapper.updateById(application);
        return Result.success("已拒绝");
    }
}

