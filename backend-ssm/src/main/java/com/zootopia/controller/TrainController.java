package com.zootopia.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zootopia.common.Result;
import com.zootopia.entity.Train;
import com.zootopia.entity.TicketOrder;
import com.zootopia.mapper.TrainMapper;
import com.zootopia.mapper.TicketOrderMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/trains")
public class TrainController {
    @Autowired
    private TrainMapper trainMapper;
    @Autowired
    private TicketOrderMapper ticketOrderMapper;

    @PostMapping
    public Result<Map<String, Integer>> createTrain(@RequestBody Map<String, Object> requestData, HttpServletRequest request) {
        String role = (String) request.getAttribute("userRole");
        if (!"admin".equals(role) && !"worker".equals(role)) {
            return Result.error(403, "暂无权限");
        }

        // 从请求中提取数据，支持驼峰和下划线两种格式
        String code = (String) requestData.get("code");
        String origin = (String) requestData.get("origin");
        String destination = (String) requestData.get("destination");
        Object departTimeObj = requestData.get("departTime") != null ? requestData.get("departTime") : requestData.get("depart_time");
        Object arriveTimeObj = requestData.get("arriveTime") != null ? requestData.get("arriveTime") : requestData.get("arrive_time");
        Object priceObj = requestData.get("price");
        Object capacityObj = requestData.get("capacity");

        if (code == null || origin == null || destination == null ||
            departTimeObj == null || arriveTimeObj == null ||
            priceObj == null || capacityObj == null) {
            return Result.error("车次信息不完整");
        }

        // 解析时间字符串
        LocalDateTime departTime;
        LocalDateTime arriveTime;
        try {
            String departTimeStr = departTimeObj.toString();
            String arriveTimeStr = arriveTimeObj.toString();
            
            // 处理 datetime-local 格式 (yyyy-MM-ddTHH:mm) 或 ISO 格式
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
            if (departTimeStr.length() > 16) {
                // ISO 格式，去掉秒和时区
                departTimeStr = departTimeStr.substring(0, 16);
            }
            if (arriveTimeStr.length() > 16) {
                arriveTimeStr = arriveTimeStr.substring(0, 16);
            }
            
            departTime = LocalDateTime.parse(departTimeStr, formatter);
            arriveTime = LocalDateTime.parse(arriveTimeStr, formatter);
        } catch (Exception e) {
            return Result.error("时间格式不正确: " + e.getMessage());
        }

        if (departTime.isAfter(arriveTime)) {
            return Result.error("出发时间不能晚于到达时间");
        }

        // 创建 Train 对象
        Train train = new Train();
        train.setCode(code);
        train.setOrigin(origin);
        train.setDestination(destination);
        train.setDepartTime(departTime);
        train.setArriveTime(arriveTime);
        
        // 解析价格和容量
        BigDecimal price;
        Integer capacity;
        try {
            if (priceObj instanceof BigDecimal) {
                price = (BigDecimal) priceObj;
            } else if (priceObj instanceof Number) {
                price = BigDecimal.valueOf(((Number) priceObj).doubleValue());
            } else {
                price = new BigDecimal(priceObj.toString());
            }
            
            if (capacityObj instanceof Integer) {
                capacity = (Integer) capacityObj;
            } else if (capacityObj instanceof Number) {
                capacity = ((Number) capacityObj).intValue();
            } else {
                capacity = Integer.parseInt(capacityObj.toString());
            }
        } catch (Exception e) {
            return Result.error("价格或容量格式不正确");
        }
        
        train.setPrice(price);
        train.setCapacity(capacity);

        // 计算行驶时间
        Duration duration = Duration.between(departTime, arriveTime);
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        train.setDuration(hours + "小时" + minutes + "分钟");
        train.setSold(0);

        trainMapper.insert(train);
        Map<String, Integer> result = new HashMap<>();
        result.put("id", train.getId());
        return Result.success(result);
    }

    @GetMapping
    public Result<List<Train>> getTrains(
            @RequestParam(required = false) String origin,
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDateTime date) {
        
        LambdaQueryWrapper<Train> wrapper = new LambdaQueryWrapper<>();
        if (origin != null && !origin.isEmpty()) {
            wrapper.like(Train::getOrigin, origin);
        }
        if (destination != null && !destination.isEmpty()) {
            wrapper.like(Train::getDestination, destination);
        }
        if (date != null) {
            wrapper.apply("DATE(depart_time) = DATE({0})", date);
        }
        wrapper.orderByAsc(Train::getDepartTime);
        
        List<Train> trains = trainMapper.selectList(wrapper);
        
        // 计算已售数量
        for (Train train : trains) {
            Long soldCount = ticketOrderMapper.selectCount(
                    new LambdaQueryWrapper<TicketOrder>()
                            .eq(TicketOrder::getTrainId, train.getId())
                            .in(TicketOrder::getStatus, "已支付", "已出行")
            );
            train.setSold(soldCount.intValue());
        }
        
        return Result.success(trains);
    }

    @GetMapping("/{id}")
    public Result<Train> getTrain(@PathVariable Integer id) {
        Train train = trainMapper.selectById(id);
        if (train == null) {
            return Result.error(404, "车次不存在");
        }
        return Result.success(train);
    }
}

