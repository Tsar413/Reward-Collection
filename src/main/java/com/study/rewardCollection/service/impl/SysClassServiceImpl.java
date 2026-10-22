package com.study.rewardCollection.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.study.rewardCollection.dto.SysClassDTO;
import com.study.rewardCollection.entity.SysClass;
import com.study.rewardCollection.entity.SysOrg;
import com.study.rewardCollection.entity.SysUser;
import com.study.rewardCollection.mapper.SysClassMapper;
import com.study.rewardCollection.mapper.SysOrgMapper;
import com.study.rewardCollection.mapper.SysUserMapper;
import com.study.rewardCollection.service.SysClassService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class SysClassServiceImpl extends ServiceImpl<SysClassMapper, SysClass> implements SysClassService {

    @Resource
    private SysOrgMapper sysOrgMapper;

    @Resource
    private SysUserMapper sysUserMapper;

    @Override
    public Integer addNewClass(SysClassDTO sysClassDTO) {
        if(sysClassDTO == null || sysClassDTO.getOrgId() == null || sysClassDTO.getClassCode() == null || sysClassDTO.getClassName() == null || sysClassDTO.getRemark() == null || sysClassDTO.getAdmissionYear() == null || sysClassDTO.getTeacherUserId() == null){
            return -2;
        }
        SysOrg org = sysOrgMapper.selectById(sysClassDTO.getOrgId());
        if(org == null){
            return -3;
        }
        if (!Integer.valueOf(1).equals(org.getStatus())) {
            return -5;
        }
        QueryWrapper<SysUser> wrapper2 = new QueryWrapper<SysUser>();
        wrapper2.eq("id", sysClassDTO.getTeacherUserId());
        wrapper2.eq("org_id", org.getId());
        wrapper2.eq("role", "TEACHER");
        SysUser teacher = sysUserMapper.selectOne(wrapper2);
        if(teacher == null){
            return -6;
        }
        String classCode = sysClassDTO.getClassCode().trim();
        String className = sysClassDTO.getClassName().trim();
        String remark = sysClassDTO.getRemark() == null ? "" : sysClassDTO.getRemark().trim();
        if(classCode.length() > 50 || className.length() > 100 || remark.length() > 500 || sysClassDTO.getAdmissionYear() < 2020 || sysClassDTO.getAdmissionYear() > 3000){
            return -3;
        }
        QueryWrapper<SysClass> wrapper1 = new QueryWrapper<SysClass>();
        wrapper1.eq("org_id", sysClassDTO.getOrgId())
                .eq("class_code", classCode);
        SysClass sysClass = baseMapper.selectOne(wrapper1);
        if(sysClass != null){
            return -4;
        }
        LocalDateTime time = LocalDateTime.now();
        SysClass sysClassAdd = new SysClass();
        sysClassAdd.setOrgId(sysClassDTO.getOrgId());
        sysClassAdd.setClassName(className);
        sysClassAdd.setClassCode(classCode);
        sysClassAdd.setRemark(remark);
        sysClassAdd.setStatus(1);
        sysClassAdd.setCreateTime(time);
        sysClassAdd.setUpdateTime(time);
        sysClassAdd.setAdmissionYear(sysClassDTO.getAdmissionYear());
        sysClassAdd.setTeacherUserId(sysClassDTO.getTeacherUserId());
        if(baseMapper.insert(sysClassAdd) != 1){
            throw new RuntimeException("保存失败");
        }
        return 1;
    }
}
