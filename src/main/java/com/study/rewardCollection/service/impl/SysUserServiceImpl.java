package com.study.rewardCollection.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.study.rewardCollection.dto.SysUserRegisterDTO;
import com.study.rewardCollection.entity.SysClass;
import com.study.rewardCollection.entity.SysOrg;
import com.study.rewardCollection.entity.SysUser;
import com.study.rewardCollection.mapper.SysClassMapper;
import com.study.rewardCollection.mapper.SysOrgMapper;
import com.study.rewardCollection.mapper.SysUserMapper;
import com.study.rewardCollection.service.SysUserService;
import com.study.rewardCollection.util.PasswordUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    @Resource
    private SysOrgMapper sysOrgMapper;

    @Resource
    private SysClassMapper sysClassMapper;

    @Override
    public Integer registrationOne(SysUserRegisterDTO sysUserRegisterDTO) {
        if(sysUserRegisterDTO == null || sysUserRegisterDTO.getOrgId() == null || sysUserRegisterDTO.getRealName() == null || sysUserRegisterDTO.getPassword() == null || sysUserRegisterDTO.getRole() == null || sysUserRegisterDTO.getUserNo() == null){
            return -2;
        }
        Long orgId = sysUserRegisterDTO.getOrgId();
        String realName = sysUserRegisterDTO.getRealName().trim();
        String username = sysUserRegisterDTO.getRealName().trim();
        String password = sysUserRegisterDTO.getPassword();
        String role = sysUserRegisterDTO.getRole().trim();
        Long classId = sysUserRegisterDTO.getClassId() == null ? null : sysUserRegisterDTO.getClassId();
        String userNo = sysUserRegisterDTO.getUserNo().trim();
        String remark = sysUserRegisterDTO.getRemark() == null ? "" : sysUserRegisterDTO.getRemark().trim();
        if(realName.length() > 50 || (password.length() < 8 || password.length() > 64) || userNo.length() > 50 || remark.length() > 500){
            return -3;
        }
        if("TEACHER".equals(role) || "ADMIN".equals(role) || "STUDENT".equals(role)){
            return -4;
        }
        // 检查组织是否存在
        Long orgCount = sysOrgMapper.selectCount(new QueryWrapper<SysOrg>().eq("org_id", orgId).eq("status", 1));
        if(orgCount != 1){
            return -5;
        }
        // 检查用户id是否存在
        Long userCount = baseMapper.selectCount(new QueryWrapper<SysUser>().eq("org_id", orgId).eq("user_no", userNo));
        if(userCount != 0){
            return -6;
        }
        // 检查班级是否存在
        if(classId != null){
            Long classCount = sysClassMapper.selectCount(new QueryWrapper<SysClass>().eq("org_id", orgId).eq("id", classId).eq("status", 1));
            if(classCount != 1){
                return -7;
            }
        }
        SysUser sysUser = new SysUser();
        sysUser.setUserNo(userNo);
        sysUser.setRemark(remark);
        sysUser.setClassId(classId);
        sysUser.setStatus(1);
        sysUser.setOrgId(orgId);
        sysUser.setRealName(realName);
        sysUser.setRole(role);
        sysUser.setLastLoginTime(null);
        sysUser.setUsername(username);
        String salt = PasswordUtil.generateSalt();
        sysUser.setPasswordSalt(salt);
        sysUser.setPasswordHash(PasswordUtil.hashPassword("123456", salt));
        sysUser.setPasswordAlgorithm("SHA-256");
        LocalDateTime time = LocalDateTime.now();
        sysUser.setUpdateTime(time);
        sysUser.setCreateTime(time);
        if (baseMapper.insert(sysUser) != 1) {
            throw new IllegalStateException("创建用户失败");
        }
        return 1;
    }
}
