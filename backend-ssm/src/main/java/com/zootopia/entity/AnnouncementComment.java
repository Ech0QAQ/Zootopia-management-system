package com.zootopia.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("announcement_comments")
public class AnnouncementComment {
    @TableId(type = IdType.AUTO)
    private Integer id;
    @TableField("announcement_id")
    private Integer announcementId;
    @TableField("user_id")
    private Integer userId;
    private String content;
    private Boolean anonymous;
    @TableField("created_at")
    private LocalDateTime createdAt;
    
    // 关联查询字段
    @TableField(exist = false)
    private String name;
}
