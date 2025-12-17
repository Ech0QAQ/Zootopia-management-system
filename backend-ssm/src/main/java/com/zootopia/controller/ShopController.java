package com.zootopia.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zootopia.common.Result;
import com.zootopia.entity.ShopProduct;
import com.zootopia.mapper.ShopProductMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/shop")
public class ShopController {
    @Autowired
    private ShopProductMapper shopProductMapper;

    @GetMapping("/products")
    public Result<List<Map<String, Object>>> getProducts(@RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<ShopProduct> wrapper = new LambdaQueryWrapper<ShopProduct>()
                .eq(ShopProduct::getStatus, "on")
                .orderByDesc(ShopProduct::getCreatedAt);
        
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(ShopProduct::getName, keyword);
        }
        
        List<ShopProduct> products = shopProductMapper.selectList(wrapper);
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (ShopProduct product : products) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", product.getId());
            item.put("code", product.getCode());
            item.put("name", product.getName());
            item.put("price", product.getPrice());
            item.put("stock", product.getStock());
            item.put("description", product.getDescription());
            item.put("status", product.getStatus());
            item.put("imageUrl", "http://localhost:3000/api/shop/products/" + product.getId() + "/image");
            result.add(item);
        }
        return Result.success(result);
    }

    @GetMapping("/products/{id}")
    public Result<Map<String, Object>> getProduct(@PathVariable Integer id) {
        ShopProduct product = shopProductMapper.selectById(id);
        if (product == null) {
            return Result.error(404, "商品不存在");
        }
        Map<String, Object> result = new HashMap<>();
        result.put("id", product.getId());
        result.put("code", product.getCode());
        result.put("name", product.getName());
        result.put("price", product.getPrice());
        result.put("stock", product.getStock());
        result.put("description", product.getDescription());
        result.put("status", product.getStatus());
        result.put("imageUrl", "http://localhost:3000/api/shop/products/" + product.getId() + "/image");
        return Result.success(result);
    }

    @GetMapping("/products/{id}/image")
    public org.springframework.http.ResponseEntity<byte[]> getProductImage(@PathVariable Integer id) {
        ShopProduct product = shopProductMapper.selectById(id);
        if (product == null || product.getImage() == null) {
            return org.springframework.http.ResponseEntity.notFound().build();
        }
        return org.springframework.http.ResponseEntity.ok()
                .header("Content-Type", product.getImageType() != null ? product.getImageType() : "image/jpeg")
                .body(product.getImage());
    }
}

