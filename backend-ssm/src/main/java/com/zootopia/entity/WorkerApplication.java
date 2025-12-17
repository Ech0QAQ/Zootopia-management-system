package com.zootopia.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("worker_applications")
public class WorkerApplication {
    @TableId(type = IdType.AUTO)
    private Integer id;
    @TableField("worker_id")
    private Integer workerId;
    private String type;
    private String reason;
    private String status;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
    
    // 关联查询字段
    @TableField(exist = false)
    private String name;
    @TableField(exist = false)
    private String username;
}
