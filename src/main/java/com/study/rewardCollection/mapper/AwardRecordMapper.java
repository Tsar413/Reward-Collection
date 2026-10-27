package com.study.rewardCollection.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.study.rewardCollection.entity.AwardRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AwardRecordMapper extends BaseMapper<AwardRecord> {
    @Select("SELECT * FROM award_record WHERE id = #{id} AND is_deleted = 0 FOR UPDATE")
    AwardRecord selectForUpdate(@Param("id") Long id);
}
