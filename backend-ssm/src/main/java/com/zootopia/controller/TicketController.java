package com.zootopia.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zootopia.common.Result;
import com.zootopia.entity.TicketOrder;
import com.zootopia.entity.Train;
import com.zootopia.entity.User;
import com.zootopia.mapper.TicketOrderMapper;
import com.zootopia.mapper.TrainMapper;
import com.zootopia.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/tickets")
public class TicketController {
    @Autowired
    private TicketOrderMapper ticketOrderMapper;
    @Autowired
    private TrainMapper trainMapper;
    @Autowired
    private UserMapper userMapper;

    @PostMapping("/orders")
    @Transactional
    public Result<Map<String, Integer>> createOrder(@RequestBody Map<String, Integer> request, HttpServletRequest httpRequest) {
        String role = (String) httpRequest.getAttribute("userRole");
        if (!"resident".equals(role)) {
            return Result.error(403, "暂无权限");
        }

        Integer trainId = request.get("train_id");
        if (trainId == null) {
            return Result.error("车次信息不完整");
        }

        Train train = trainMapper.selectById(trainId);
        if (train == null) {
            return Result.error(404, "车次不存在");
        }

        if (train.getDepartTime().isBefore(LocalDateTime.now())) {
            return Result.error("该车次已发车，无法购买");
        }

        Integer userId = (Integer) httpRequest.getAttribute("userId");
        TicketOrder order = new TicketOrder();
        order.setUserId(userId);
        order.setTrainId(trainId);
        order.setOrigin(train.getOrigin());
        order.setDestination(train.getDestination());
        order.setDepartTime(train.getDepartTime());
        order.setArriveTime(train.getArriveTime());
        order.setPrice(train.getPrice());
        order.setStatus("已支付");

        ticketOrderMapper.insert(order);

        // 更新车次已售数量
        train.setSold(train.getSold() + 1);
        trainMapper.updateById(train);

        Map<String, Integer> result = new HashMap<>();
        result.put("id", order.getId());
        return Result.success(result);
    }

    @GetMapping("/my-orders")
    public Result<List<TicketOrder>> getMyOrders(HttpServletRequest request) {
        String role = (String) request.getAttribute("userRole");
        if (!"resident".equals(role)) {
            return Result.error(403, "暂无权限");
        }

        Integer userId = (Integer) request.getAttribute("userId");

        // 更新已过期的订单状态
        List<TicketOrder> expiredOrders = ticketOrderMapper.selectList(
                new LambdaQueryWrapper<TicketOrder>()
                        .eq(TicketOrder::getUserId, userId)
                        .eq(TicketOrder::getStatus, "已支付")
                        .le(TicketOrder::getDepartTime, LocalDateTime.now())
        );
        for (TicketOrder order : expiredOrders) {
            order.setStatus("已出行");
            ticketOrderMapper.updateById(order);
        }

        List<TicketOrder> orders = ticketOrderMapper.selectList(
                new LambdaQueryWrapper<TicketOrder>()
                        .eq(TicketOrder::getUserId, userId)
                        .orderByDesc(TicketOrder::getCreatedAt)
        );

        // 关联查询车次号（这里简化处理，实际应该用JOIN查询）
        for (TicketOrder order : orders) {
            Train train = trainMapper.selectById(order.getTrainId());
            if (train != null) {
                order.setTrainCode(train.getCode());
            }
        }

        return Result.success(orders);
    }

    @PostMapping("/orders/{id}/refund")
    @Transactional
    public Result<String> refundOrder(@PathVariable Integer id, HttpServletRequest request) {
        String role = (String) request.getAttribute("userRole");
        if (!"resident".equals(role)) {
            return Result.error(403, "暂无权限");
        }

        Integer userId = (Integer) request.getAttribute("userId");
        TicketOrder order = ticketOrderMapper.selectOne(
                new LambdaQueryWrapper<TicketOrder>()
                        .eq(TicketOrder::getId, id)
                        .eq(TicketOrder::getUserId, userId)
        );

        if (order == null) {
            return Result.error(404, "订单不存在");
        }

        if (!"已支付".equals(order.getStatus())) {
            return Result.error("当前状态不可退票");
        }

        if (order.getDepartTime() != null && order.getDepartTime().isBefore(LocalDateTime.now())) {
            return Result.error("已到出发时间，无法退票");
        }

        order.setStatus("已退票");
        ticketOrderMapper.updateById(order);

        // 更新车次已售数量
        Train train = trainMapper.selectById(order.getTrainId());
        if (train != null && train.getSold() > 0) {
            train.setSold(train.getSold() - 1);
            trainMapper.updateById(train);
        }

        return Result.success("退票成功");
    }

    @GetMapping("/train/{trainId}/orders")
    public Result<List<TicketOrder>> getTrainOrders(@PathVariable Integer trainId, HttpServletRequest request) {
        String role = (String) request.getAttribute("userRole");
        if (!"admin".equals(role) && !"worker".equals(role)) {
            return Result.error(403, "暂无权限");
        }

        List<TicketOrder> orders = ticketOrderMapper.selectList(
                new LambdaQueryWrapper<TicketOrder>()
                        .eq(TicketOrder::getTrainId, trainId)
                        .orderByDesc(TicketOrder::getCreatedAt)
        );

        // 关联查询用户信息
        for (TicketOrder order : orders) {
            User user = userMapper.selectById(order.getUserId());
            if (user != null) {
                order.setName(user.getName());
                order.setAnimalType(user.getAnimalType());
                order.setLiveArea(user.getLiveArea());
            }
        }

        return Result.success(orders);
    }
}

