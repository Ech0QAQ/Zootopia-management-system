package com.zootopia.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("shop_products")
public class ShopProduct {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String code;
    private String name;
    private BigDecimal price;
    private Integer stock;
    private String description;
    private byte[] image;
    @TableField("image_type")
    private String imageType;
    @TableField("image_size")
    private Integer imageSize;
    private String status; // on, off
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
