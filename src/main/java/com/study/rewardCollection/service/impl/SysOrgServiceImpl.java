package com.study.rewardCollection.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.study.rewardCollection.dto.SysOrgDTO;
import com.study.rewardCollection.entity.AwardRecord;
import com.study.rewardCollection.entity.SysClass;
import com.study.rewardCollection.entity.SysOrg;
import com.study.rewardCollection.entity.SysUser;
import com.study.rewardCollection.mapper.AwardRecordMapper;
import com.study.rewardCollection.mapper.SysClassMapper;
import com.study.rewardCollection.mapper.SysOrgMapper;
import com.study.rewardCollection.mapper.SysUserMapper;
import com.study.rewardCollection.security.DataScopeService;
import com.study.rewardCollection.service.SysOrgService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class SysOrgServiceImpl extends ServiceImpl<SysOrgMapper, SysOrg> implements SysOrgService {

    @Resource
    private SysClassMapper sysClassMapper;

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private DataScopeService dataScope;

    @Resource
    private AwardRecordMapper awardRecordMapper;

    @Override
    public Integer addNewOrg(SysOrgDTO sysOrgDTO) {
        dataScope.checkOrgCreate();
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

    @Override
    public Integer updateOrg(SysOrgDTO sysOrgDTO, Long id) {
        SysOrg sysOrg = baseMapper.selectById(id);
        if(sysOrg == null){
            return -4;
        }
        dataScope.checkOrgWrite(sysOrg.getId());
        if(sysOrgDTO == null || sysOrgDTO.getOrgName() == null){
            return -2;
        }
        String orgName = sysOrgDTO.getOrgName().trim();
        String remark = sysOrgDTO.getRemark() == null ? "" : sysOrgDTO.getRemark().trim();
        if(orgName.isBlank()){
            return -2;
        }
        if(orgName.length() > 100 || remark.length() > 500){
            return -3;
        }
        sysOrg.setOrgName(orgName);
        sysOrg.setRemark(remark);
        sysOrg.setUpdateTime(LocalDateTime.now());
        if(baseMapper.updateById(sysOrg) != 1){
            throw new RuntimeException("修改失败");
        }
        return 1;
    }

    @Override
    public Integer deleteOrg(Long id) {
        SysOrg sysOrg = baseMapper.selectById(id);
        if(sysOrg == null){
            return -4;
        }
        dataScope.checkOrgWrite(sysOrg.getId());
        Long classOrgCount = sysClassMapper.selectCount(new QueryWrapper<SysClass>().eq("org_id", sysOrg.getId()));
        if(classOrgCount > 0){
            return -2;
        }
        Long userCount = sysUserMapper.selectCount(new QueryWrapper<SysUser>().eq("org_id", sysOrg.getId()));
        if(userCount > 0){
            return -3;
        }
        // 检查组织下是否存在未删除的个人奖状、班级奖状。
        Long awardCount = awardRecordMapper.selectCount(new QueryWrapper<AwardRecord>().eq("org_id", id));
        if (awardCount > 0) {
            return -5;
        }
        if(baseMapper.deleteById(id) != 1){
            throw new RuntimeException("删除失败");
        }
        return 1;
    }
}
