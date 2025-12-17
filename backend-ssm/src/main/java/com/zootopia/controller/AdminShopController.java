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
import java.util.List;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/admin/shop")
public class AdminShopController {
    @Autowired
    private ShopProductMapper shopProductMapper;

    private String generateProductCode() {
        String ts = String.valueOf(Instant.now().toEpochMilli()).substring(5);
        int rand = new Random().nextInt(9000) + 1000;
        return "PD" + ts + rand;
    }

    private byte[] parseImageBase64(String base64) {
        if (base64 == null || base64.isEmpty()) {
            return null;
        }
        try {
            String data = base64;
            if (base64.contains(",")) {
                data = base64.substring(base64.indexOf(",") + 1);
            }
            byte[] imageBytes = Base64.getDecoder().decode(data);
            if (imageBytes.length > 500 * 1024) {
                throw new RuntimeException("图片超过500KB限制");
            }
            return imageBytes;
        } catch (Exception e) {
            throw new RuntimeException("图片数据无效: " + e.getMessage());
        }
    }

    @GetMapping("/products")
    public Result<List<ShopProduct>> getAdminProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        LambdaQueryWrapper<ShopProduct> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(ShopProduct::getName, keyword);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(ShopProduct::getStatus, status);
        }
        wrapper.orderByDesc(ShopProduct::getCreatedAt);
        
        List<ShopProduct> products = shopProductMapper.selectList(wrapper);
        return Result.success(products);
    }

    @PostMapping("/products")
    public Result<String> createProduct(@RequestBody Map<String, Object> request, HttpServletRequest httpRequest) {
        String role = (String) httpRequest.getAttribute("userRole");
        if (!"admin".equals(role) && !"worker".equals(role)) {
            return Result.error(403, "暂无权限");
        }

        String name = (String) request.get("name");
        Object priceObj = request.get("price");
        Object stockObj = request.get("stock");
        String description = (String) request.get("description");
        String imageBase64 = (String) request.get("imageBase64");

        if (name == null || priceObj == null || stockObj == null || description == null || imageBase64 == null) {
            return Result.error("信息不完整");
        }

        try {
            byte[] imageBytes = parseImageBase64(imageBase64);
            if (imageBytes == null) {
                return Result.error("图片数据无效");
            }

            String code = generateProductCode();
            for (int i = 0; i < 3; i++) {
                ShopProduct exist = shopProductMapper.selectOne(
                        new LambdaQueryWrapper<ShopProduct>().eq(ShopProduct::getCode, code)
                );
                if (exist == null) break;
                code = generateProductCode();
            }

            ShopProduct product = new ShopProduct();
            product.setCode(code);
            product.setName(name);
            product.setPrice(new java.math.BigDecimal(priceObj.toString()));
            product.setStock(Integer.parseInt(stockObj.toString()));
            product.setDescription(description);
            product.setImage(imageBytes);
            product.setImageType("image/jpeg");
            product.setImageSize(imageBytes.length);
            product.setStatus("on");

            shopProductMapper.insert(product);
            return Result.success("创建成功");
        } catch (Exception e) {
            return Result.error("创建失败: " + e.getMessage());
        }
    }

    @PutMapping("/products/{id}")
    public Result<String> updateProduct(@PathVariable Integer id, @RequestBody Map<String, Object> request) {
        ShopProduct product = shopProductMapper.selectById(id);
        if (product == null) {
            return Result.error(404, "商品不存在");
        }

        if (request.containsKey("name")) {
            product.setName((String) request.get("name"));
        }
        if (request.containsKey("price")) {
            product.setPrice(new java.math.BigDecimal(request.get("price").toString()));
        }
        if (request.containsKey("stock")) {
            product.setStock(Integer.parseInt(request.get("stock").toString()));
        }
        if (request.containsKey("description")) {
            product.setDescription((String) request.get("description"));
        }
        if (request.containsKey("status")) {
            product.setStatus((String) request.get("status"));
        }
        if (request.containsKey("imageBase64")) {
            try {
                byte[] imageBytes = parseImageBase64((String) request.get("imageBase64"));
                if (imageBytes != null) {
                    product.setImage(imageBytes);
                    product.setImageType("image/jpeg");
                    product.setImageSize(imageBytes.length);
                }
            } catch (Exception e) {
                return Result.error("图片处理失败: " + e.getMessage());
            }
        }

        shopProductMapper.updateById(product);
        return Result.success("已更新");
    }

    @PostMapping("/products/{id}/on")
    public Result<String> onProduct(@PathVariable Integer id) {
        ShopProduct product = shopProductMapper.selectById(id);
        if (product == null) {
            return Result.error(404, "商品不存在");
        }
        product.setStatus("on");
        shopProductMapper.updateById(product);
        return Result.success("已上架");
    }

    @PostMapping("/products/{id}/off")
    public Result<String> offProduct(@PathVariable Integer id) {
        ShopProduct product = shopProductMapper.selectById(id);
        if (product == null) {
            return Result.error(404, "商品不存在");
        }
        product.setStatus("off");
        shopProductMapper.updateById(product);
        return Result.success("已下架");
    }

    @DeleteMapping("/products/{id}")
    public Result<String> deleteProduct(@PathVariable Integer id) {
        shopProductMapper.deleteById(id);
        return Result.success("已删除");
    }
}

