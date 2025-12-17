package com.zootopia.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zootopia.entity.Announcement;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AnnouncementMapper extends BaseMapper<Announcement> {
}

