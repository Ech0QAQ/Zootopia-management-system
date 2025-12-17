package com.zootopia.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("star_candidates")
public class StarCandidate {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String name;
    private String photo;
}
