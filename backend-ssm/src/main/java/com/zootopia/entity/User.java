package com.zootopia.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("users")
public class User {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String username;
    private String password;
    private String role; // admin, worker, resident
    private String name;
    private Integer age;
    private String gender;
    @TableField("animal_type")
    private String animalType;
    @TableField("live_area")
    private String liveArea;
    private String household;
    @TableField("work_area")
    private String workArea;
    private String department;
    private BigDecimal salary;
    @TableField("employment_status")
    private String employmentStatus;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
