package com.study.rewardCollection.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.study.rewardCollection.dto.SysOrgDTO;
import com.study.rewardCollection.entity.SysOrg;
import com.study.rewardCollection.mapper.SysOrgMapper;
import com.study.rewardCollection.service.SysOrgService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class SysOrgServiceImpl extends ServiceImpl<SysOrgMapper, SysOrg> implements SysOrgService {

    @Override
    public Integer addNewOrg(SysOrgDTO sysOrgDTO) {
        if(sysOrgDTO == null || sysOrgDTO.getOrgCode() == null || sysOrgDTO.getOrgName() == null){
            return -2;
        }
        String orgCode = sysOrgDTO.getOrgCode().trim();
        String orgName = sysOrgDTO.getOrgName().trim();
        String remark = sysOrgDTO.getRemark() == null ? "" : sysOrgDTO.getRemark().trim();
        if(orgCode.isBlank() || orgName.isBlank()){
            return -2;
        }
        if(orgCode.length() > 50 || orgName.length() > 100 || remark.length() > 500){
            return -3;
        }
        QueryWrapper<SysOrg> wrapper1 = new QueryWrapper<SysOrg>();
        wrapper1.eq("org_code", orgCode);
        SysOrg sysOrg1 = baseMapper.selectOne(wrapper1);
        if(sysOrg1 != null){
            return -4;
        }
        LocalDateTime time = LocalDateTime.now();
        SysOrg sysOrg = new SysOrg();
        sysOrg.setOrgCode(orgCode);
        sysOrg.setOrgName(orgName);
        sysOrg.setStatus(1);
        sysOrg.setRemark(remark);
        sysOrg.setCreateTime(time);
        sysOrg.setUpdateTime(time);
        if(baseMapper.insert(sysOrg) != 1){
            throw new RuntimeException("保存失败");
        }
        return 1;
    }
}
