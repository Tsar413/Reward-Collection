package com.study.rewardCollection.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.study.rewardCollection.dto.SysUserRegisterDTO;
import com.study.rewardCollection.entity.SysUser;

public interface SysUserService extends IService<SysUser> {

    Integer registrationOne(SysUserRegisterDTO sysUserRegisterDTO);
}
