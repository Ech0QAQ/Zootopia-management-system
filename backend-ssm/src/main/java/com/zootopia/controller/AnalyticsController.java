package com.zootopia.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zootopia.common.Result;
import com.zootopia.entity.ShopOrder;
import com.zootopia.entity.ShopOrderItem;
import com.zootopia.entity.ShopProduct;
import com.zootopia.mapper.ShopOrderItemMapper;
import com.zootopia.mapper.ShopOrderMapper;
import com.zootopia.mapper.ShopProductMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.*;

@RestController
@RequestMapping("/admin/shop")
public class AnalyticsController {
    @Autowired
    private ShopOrderMapper shopOrderMapper;
    @Autowired
    private ShopOrderItemMapper shopOrderItemMapper;
    @Autowired
    private ShopProductMapper shopProductMapper;

    @GetMapping("/analytics")
    public Result<Map<String, Object>> getAnalytics() {
        int year = LocalDateTime.now().getYear();
        
        // 查询当年所有订单
        List<ShopOrder> orders = shopOrderMapper.selectList(
                new LambdaQueryWrapper<ShopOrder>()
                        .apply("YEAR(created_at) = {0}", year)
        );

        // 按月统计
        BigDecimal[] amount = new BigDecimal[12];
        Integer[] count = new Integer[12];
        for (int i = 0; i < 12; i++) {
            amount[i] = BigDecimal.ZERO;
            count[i] = 0;
        }

        for (ShopOrder order : orders) {
            int month = order.getCreatedAt().getMonthValue() - 1;
            amount[month] = amount[month].add(order.getTotalAmount());
            count[month]++;
        }

        // 商品销售额统计
        Map<Integer, BigDecimal> productSalesMap = new HashMap<>();
        Map<Integer, Integer> productQuantityMap = new HashMap<>();
        Map<Integer, String> productNameMap = new HashMap<>();

        List<ShopOrderItem> items = shopOrderItemMapper.selectList(
                new LambdaQueryWrapper<ShopOrderItem>()
        );

        for (ShopOrderItem item : items) {
            ShopOrder order = shopOrderMapper.selectById(item.getOrderId());
            if (order != null && order.getCreatedAt().getYear() == year) {
                Integer productId = item.getProductId();
                BigDecimal sales = item.getPrice().multiply(new BigDecimal(item.getQuantity()));
                
                productSalesMap.put(productId, 
                    productSalesMap.getOrDefault(productId, BigDecimal.ZERO).add(sales));
                productQuantityMap.put(productId, 
                    productQuantityMap.getOrDefault(productId, 0) + item.getQuantity());
                
                if (!productNameMap.containsKey(productId)) {
                    ShopProduct product = shopProductMapper.selectById(productId);
                    if (product != null) {
                        productNameMap.put(productId, product.getName());
                    }
                }
            }
        }

        List<Map<String, Object>> productSales = new ArrayList<>();
        List<Map<String, Object>> productQuantities = new ArrayList<>();

        productSalesMap.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(20)
                .forEach(entry -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("name", productNameMap.get(entry.getKey()));
                    item.put("sales", entry.getValue());
                    productSales.add(item);
                });

        productQuantityMap.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(20)
                .forEach(entry -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("name", productNameMap.get(entry.getKey()));
                    item.put("quantity", entry.getValue());
                    productQuantities.add(item);
                });

        Map<String, Object> result = new HashMap<>();
        result.put("year", year);
        result.put("amount", Arrays.asList(amount));
        result.put("count", Arrays.asList(count));
        result.put("productSales", productSales);
        result.put("productQuantities", productQuantities);

        return Result.success(result);
    }
}

