package com.study.rewardCollection.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.study.rewardCollection.dto.SysUserLoginDTO;
import com.study.rewardCollection.dto.SysUserLoginResultDTO;
import com.study.rewardCollection.entity.SysLoginToken;

public interface SysLoginService extends IService<SysLoginToken> {
    SysUserLoginResultDTO login(SysUserLoginDTO sysUserLoginDTO);
}
