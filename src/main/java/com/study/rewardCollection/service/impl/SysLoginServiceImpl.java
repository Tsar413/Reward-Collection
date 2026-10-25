package com.study.rewardCollection.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.study.rewardCollection.dto.SysUserLoginDTO;
import com.study.rewardCollection.dto.SysUserLoginResultDTO;
import com.study.rewardCollection.entity.SysLoginToken;
import com.study.rewardCollection.entity.SysUser;
import com.study.rewardCollection.mapper.SysLoginTokenMapper;
import com.study.rewardCollection.mapper.SysUserMapper;
import com.study.rewardCollection.service.SysLoginService;
import com.study.rewardCollection.util.PasswordUtil;
import com.study.rewardCollection.util.TokenUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class SysLoginServiceImpl extends ServiceImpl<SysLoginTokenMapper, SysLoginToken> implements SysLoginService {

    @Resource
    private SysUserMapper sysUserMapper;

    @Override
    public SysUserLoginResultDTO login(SysUserLoginDTO sysUserLoginDTO) {
        if(sysUserLoginDTO == null || sysUserLoginDTO.getUserNo() == null || sysUserLoginDTO.getPassword() == null){
            return null;
        }
        String userNo = sysUserLoginDTO.getUserNo().trim();
        String password = sysUserLoginDTO.getPassword();
        SysUser sysUser = sysUserMapper.selectOne(new QueryWrapper<SysUser>().eq("user_no", userNo).eq("status", 1));
        if(sysUser == null){
            return null;
        }
        boolean b = PasswordUtil.verifyPassword(password, sysUser.getPasswordSalt(), sysUser.getPasswordHash(), sysUser.getPasswordAlgorithm());
        if(!b){
            return null;
        }
        SysLoginToken record = baseMapper.selectOne(new QueryWrapper<SysLoginToken>().eq("user_id", sysUser.getId()));
        LocalDateTime now = LocalDateTime.now();
        if(record != null){
            record.setCreateTime(now);
            record.setExpireTime(now.plusDays(3));
            record.setUpdateTime(now);
            record.setRevokeTime(null);
            record.setTokenHash(TokenUtil.generateToken());
            if(baseMapper.updateById(record) != 1){
                throw new RuntimeException("生成token失败");
            }
        } else {
            record = new SysLoginToken();
            record.setUserId(sysUser.getId());
            record.setCreateTime(now);
            record.setExpireTime(now.plusDays(3));
            record.setUpdateTime(now);
            record.setRevokeTime(null);
            record.setTokenHash(TokenUtil.generateToken());
            if(baseMapper.insert(record) != 1){
                throw new RuntimeException("生成token失败");
            }
        }
        SysUserLoginResultDTO resultDTO = new SysUserLoginResultDTO();
        resultDTO.setToken(record.getTokenHash());
        SysUserLoginResultDTO.UserInfo userInfo = new SysUserLoginResultDTO.UserInfo();
        userInfo.setId(sysUser.getId());
        userInfo.setRole(sysUser.getRole());
        userInfo.setStatus(sysUser.getStatus());
        userInfo.setUserNo(sysUser.getUserNo());
        userInfo.setOrgId(sysUser.getOrgId());
        userInfo.setClassId(sysUser.getClassId());
        userInfo.setUsername(sysUser.getUsername());
        userInfo.setRealName(sysUser.getRealName());
        resultDTO.setUser(userInfo);
        return resultDTO;
    }
}
