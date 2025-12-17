package com.zootopia.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("shop_orders")
public class ShopOrder {
    @TableId(type = IdType.AUTO)
    private Integer id;
    @TableField("order_no")
    private String orderNo;
    @TableField("user_id")
    private Integer userId;
    @TableField("total_amount")
    private BigDecimal totalAmount;
    private String status; // 待发货, 已发货, 已收货, 已完成
    @TableField("receiver_name")
    private String receiverName;
    @TableField("receiver_phone")
    private String receiverPhone;
    @TableField("receiver_address")
    private String receiverAddress;
    @TableField("express_company")
    private String expressCompany;
    @TableField("express_no")
    private String expressNo;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
    
    // 关联查询字段
    @TableField(exist = false)
    private String username;
    @TableField(exist = false)
    private List<ShopOrderItem> items;
}
