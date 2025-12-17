package com.zootopia.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("ticket_orders")
public class TicketOrder {
    @TableId(type = IdType.AUTO)
    private Integer id;
    @TableField("user_id")
    private Integer userId;
    @TableField("train_id")
    private Integer trainId;
    private String origin;
    private String destination;
    @TableField("depart_time")
    private LocalDateTime departTime;
    @TableField("arrive_time")
    private LocalDateTime arriveTime;
    private BigDecimal price;
    private String status;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
    
    // 关联查询字段
    @TableField(exist = false)
    private String trainCode;
    @TableField(exist = false)
    private String name;
    @TableField(exist = false)
    private String animalType;
    @TableField(exist = false)
    private String liveArea;
}
