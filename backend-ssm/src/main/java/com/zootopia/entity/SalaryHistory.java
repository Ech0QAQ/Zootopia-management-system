package com.zootopia.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("salary_history")
public class SalaryHistory {
    @TableId(type = IdType.AUTO)
    private Integer id;
    @TableField("worker_id")
    private Integer workerId;
    private BigDecimal value;
    @TableField("created_at")
    private LocalDateTime createdAt;
}
