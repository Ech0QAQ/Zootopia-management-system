package com.zootopia.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zootopia.common.Result;
import com.zootopia.entity.ShopOrder;
import com.zootopia.entity.ShopOrderItem;
import com.zootopia.entity.ShopProduct;
import com.zootopia.mapper.ShopOrderItemMapper;
import com.zootopia.mapper.ShopOrderMapper;
import com.zootopia.mapper.ShopProductMapper;
import com.zootopia.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/shop/orders")
public class ShopOrderController {
    @Autowired
    private ShopOrderMapper shopOrderMapper;
    @Autowired
    private ShopOrderItemMapper shopOrderItemMapper;
    @Autowired
    private ShopProductMapper shopProductMapper;
    @Autowired
    private UserMapper userMapper;

    private String generateOrderNo() {
        Random rand = new Random();
        StringBuilder num = new StringBuilder();
        for (int i = 0; i < 15; i++) {
            num.append(rand.nextInt(10));
        }
        return "ON" + num;
    }

    @PostMapping
    @Transactional
    public Result<Map<String, Object>> createOrder(@RequestBody Map<String, Object> request, HttpServletRequest httpRequest) {
        String role = (String) httpRequest.getAttribute("userRole");
        if (!"resident".equals(role)) {
            return Result.error(403, "暂无权限");
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) request.get("items");
        String receiverName = (String) request.get("receiver_name");
        String receiverPhone = (String) request.get("receiver_phone");
        String receiverAddress = (String) request.get("receiver_address");

        if (items == null || items.isEmpty() || receiverName == null || receiverPhone == null || receiverAddress == null) {
            return Result.error("信息不完整");
        }

        Integer userId = (Integer) httpRequest.getAttribute("userId");
        BigDecimal total = BigDecimal.ZERO;

        // 检查库存
        for (Map<String, Object> item : items) {
            Integer productId = Integer.parseInt(item.get("product_id").toString());
            Integer quantity = Integer.parseInt(item.get("quantity").toString());
            
            ShopProduct product = shopProductMapper.selectById(productId);
            if (product == null || !"on".equals(product.getStatus())) {
                return Result.error("商品不存在或未上架");
            }
            if (product.getStock() < quantity) {
                return Result.error("库存不足：" + product.getName());
            }
            total = total.add(product.getPrice().multiply(new BigDecimal(quantity)));
        }

        // 生成订单号
        String orderNo = generateOrderNo();
        for (int i = 0; i < 3; i++) {
            ShopOrder exist = shopOrderMapper.selectOne(
                    new LambdaQueryWrapper<ShopOrder>().eq(ShopOrder::getOrderNo, orderNo)
            );
            if (exist == null) break;
            orderNo = generateOrderNo();
        }

        // 创建订单
        ShopOrder order = new ShopOrder();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setTotalAmount(total);
        order.setStatus("待发货");
        order.setReceiverName(receiverName);
        order.setReceiverPhone(receiverPhone);
        order.setReceiverAddress(receiverAddress);
        shopOrderMapper.insert(order);

        // 创建订单项并扣减库存
        for (Map<String, Object> item : items) {
            Integer productId = Integer.parseInt(item.get("product_id").toString());
            Integer quantity = Integer.parseInt(item.get("quantity").toString());
            
            ShopProduct product = shopProductMapper.selectById(productId);
            
            ShopOrderItem orderItem = new ShopOrderItem();
            orderItem.setOrderId(order.getId());
            orderItem.setProductId(productId);
            orderItem.setProductName(product.getName());
            orderItem.setPrice(product.getPrice());
            orderItem.setQuantity(quantity);
            orderItem.setSubtotal(product.getPrice().multiply(new BigDecimal(quantity)));
            shopOrderItemMapper.insert(orderItem);

            // 扣减库存
            product.setStock(product.getStock() - quantity);
            shopProductMapper.updateById(product);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("order_no", orderNo);
        result.put("order_id", order.getId());
        return Result.success(result);
    }

    @GetMapping("/my")
    public Result<List<ShopOrder>> getMyOrders(
            @RequestParam(required = false) String status,
            HttpServletRequest request) {
        String role = (String) request.getAttribute("userRole");
        if (!"resident".equals(role)) {
            return Result.error(403, "暂无权限");
        }

        Integer userId = (Integer) request.getAttribute("userId");
        LambdaQueryWrapper<ShopOrder> wrapper = new LambdaQueryWrapper<ShopOrder>()
                .eq(ShopOrder::getUserId, userId)
                .orderByDesc(ShopOrder::getCreatedAt);
        
        if (status != null && !status.isEmpty()) {
            wrapper.eq(ShopOrder::getStatus, status);
        }

        List<ShopOrder> orders = shopOrderMapper.selectList(wrapper);
        for (ShopOrder order : orders) {
            List<ShopOrderItem> items = shopOrderItemMapper.selectList(
                    new LambdaQueryWrapper<ShopOrderItem>()
                            .eq(ShopOrderItem::getOrderId, order.getId())
            );
            order.setItems(items);
        }
        return Result.success(orders);
    }

    @PostMapping("/{id}/receive")
    public Result<String> receiveOrder(@PathVariable Integer id, HttpServletRequest request) {
        String role = (String) request.getAttribute("userRole");
        if (!"resident".equals(role)) {
            return Result.error(403, "暂无权限");
        }

        Integer userId = (Integer) request.getAttribute("userId");
        ShopOrder order = shopOrderMapper.selectOne(
                new LambdaQueryWrapper<ShopOrder>()
                        .eq(ShopOrder::getId, id)
                        .eq(ShopOrder::getUserId, userId)
        );

        if (order == null) {
            return Result.error(404, "订单不存在");
        }
        if (!"已发货".equals(order.getStatus())) {
            return Result.error("当前状态不可确认收货");
        }

        order.setStatus("已收货");
        shopOrderMapper.updateById(order);
        return Result.success("已确认收货");
    }
}

