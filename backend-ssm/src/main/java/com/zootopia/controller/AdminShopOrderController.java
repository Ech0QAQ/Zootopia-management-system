package com.zootopia.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zootopia.common.Result;
import com.zootopia.entity.ShopOrder;
import com.zootopia.entity.ShopOrderItem;
import com.zootopia.mapper.ShopOrderItemMapper;
import com.zootopia.mapper.ShopOrderMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/shop")
public class AdminShopOrderController {
    @Autowired
    private ShopOrderMapper shopOrderMapper;
    @Autowired
    private ShopOrderItemMapper shopOrderItemMapper;

    @GetMapping("/orders")
    public Result<List<ShopOrder>> getOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String month) {
        LambdaQueryWrapper<ShopOrder> wrapper = new LambdaQueryWrapper<>();
        
        if (status != null && !status.isEmpty()) {
            wrapper.eq(ShopOrder::getStatus, status);
        }
        if (month != null && !month.isEmpty()) {
            String[] parts = month.split("-");
            if (parts.length == 2) {
                int year = Integer.parseInt(parts[0]);
                int mon = Integer.parseInt(parts[1]);
                wrapper.apply("YEAR(created_at) = {0} AND MONTH(created_at) = {1}", year, mon);
            }
        }
        wrapper.orderByDesc(ShopOrder::getCreatedAt);

        List<ShopOrder> orders = shopOrderMapper.selectList(wrapper);
        for (ShopOrder order : orders) {
            // 关联用户名（简化处理）
            List<ShopOrderItem> items = shopOrderItemMapper.selectList(
                    new LambdaQueryWrapper<ShopOrderItem>()
                            .eq(ShopOrderItem::getOrderId, order.getId())
            );
            order.setItems(items);
        }
        return Result.success(orders);
    }

    @PostMapping("/orders/{id}/ship")
    public Result<String> shipOrder(@PathVariable Integer id, @RequestBody Map<String, String> request) {
        ShopOrder order = shopOrderMapper.selectById(id);
        if (order == null) {
            return Result.error(404, "订单不存在");
        }
        if (!"待发货".equals(order.getStatus())) {
            return Result.error("当前状态不可发货");
        }

        String company = request.get("company");
        String expressNo = request.get("express_no");
        if (company == null || expressNo == null) {
            return Result.error("快递信息不完整");
        }

        order.setStatus("已发货");
        order.setExpressCompany(company);
        order.setExpressNo(expressNo);
        shopOrderMapper.updateById(order);
        return Result.success("已发货");
    }

    @PostMapping("/orders/{id}/complete")
    public Result<String> completeOrder(@PathVariable Integer id) {
        ShopOrder order = shopOrderMapper.selectById(id);
        if (order == null) {
            return Result.error(404, "订单不存在");
        }
        if (!"已收货".equals(order.getStatus())) {
            return Result.error("当前状态不可完成订单");
        }

        order.setStatus("已完成");
        shopOrderMapper.updateById(order);
        return Result.success("已完成");
    }
}

