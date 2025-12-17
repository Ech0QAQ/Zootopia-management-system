package com.zootopia.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("trains")
public class Train {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String code;
    private String origin;
    private String destination;
    @TableField("depart_time")
    private LocalDateTime departTime;
    @TableField("arrive_time")
    private LocalDateTime arriveTime;
    private String duration;
    private BigDecimal price;
    private Integer capacity;
    private Integer sold;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
