package com.study.rewardCollection.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.study.rewardCollection.dto.SysClassDTO;
import com.study.rewardCollection.entity.SysClass;
import com.study.rewardCollection.entity.SysOrg;
import com.study.rewardCollection.mapper.SysClassMapper;
import com.study.rewardCollection.mapper.SysOrgMapper;
import com.study.rewardCollection.service.SysClassService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class SysClassServiceImpl extends ServiceImpl<SysClassMapper, SysClass> implements SysClassService {

    @Resource
    private SysOrgMapper sysOrgMapper;

    @Override
    public Integer addNewClass(SysClassDTO sysClassDTO) {
        if(sysClassDTO == null || sysClassDTO.getOrgId() == null || sysClassDTO.getClassCode() == null || sysClassDTO.getClassName() == null || sysClassDTO.getRemark() == null || sysClassDTO.getAdmissionYear() == null || sysClassDTO.getTeacherUserId() == null){
            return -2;
        }
        SysOrg org = sysOrgMapper.selectById(sysClassDTO.getOrgId());
        if(org == null){
            return -3;
        }
        QueryWrapper<SysClass> wrapper1 = new QueryWrapper<SysClass>();
        wrapper1.eq("org_id", sysClassDTO.getOrgId())
                .eq("class_code", sysClassDTO.getClassCode());
        SysClass sysClass = baseMapper.selectOne(wrapper1);
        if(sysClass != null){
            return -4;
        }
        LocalDateTime time = LocalDateTime.now();
        SysClass sysClassAdd = new SysClass();
        sysClassAdd.setOrgId(sysClassDTO.getOrgId());
        sysClassAdd.setClassName(sysClassDTO.getClassName());
        sysClassAdd.setClassCode(sysClassDTO.getClassCode());
        sysClassAdd.setRemark(sysClassDTO.getRemark());
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
