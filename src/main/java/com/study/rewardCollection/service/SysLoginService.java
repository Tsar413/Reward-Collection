package com.study.rewardCollection.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.study.rewardCollection.dto.SysUserLoginDTO;
import com.study.rewardCollection.dto.SysUserLoginResultDTO;
import com.study.rewardCollection.entity.SysLoginToken;
import com.study.rewardCollection.entity.SysUser;

public interface SysLoginService extends IService<SysLoginToken> {
    SysUserLoginResultDTO login(SysUserLoginDTO sysUserLoginDTO);

    SysUser checkToken(String token);

    void logout(String token);
}
