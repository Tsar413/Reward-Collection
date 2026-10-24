package com.study.rewardCollection.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.study.rewardCollection.dto.SysClassDTO;
import com.study.rewardCollection.dto.SysClassUpdateDTO;
import com.study.rewardCollection.entity.SysClass;

public interface SysClassService extends IService<SysClass> {
    Integer addNewClass(SysClassDTO sysClassDTO);

    Integer updateClass(SysClassUpdateDTO sysClassUpdateDTO, Long id);

    Integer deleteOneClass(Long id);
}
