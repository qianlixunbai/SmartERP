package com.smartoa.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartoa.entity.JournalEntry;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface JournalEntryMapper extends BaseMapper<JournalEntry> {
}
