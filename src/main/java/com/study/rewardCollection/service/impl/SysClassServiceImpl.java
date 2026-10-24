package com.study.rewardCollection.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.study.rewardCollection.dto.SysClassDTO;
import com.study.rewardCollection.dto.SysClassUpdateDTO;
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
        if(sysClassDTO == null || sysClassDTO.getOrgId() == null || sysClassDTO.getClassCode() == null || sysClassDTO.getClassName() == null || sysClassDTO.getAdmissionYear() == null || sysClassDTO.getTeacherUserId() == null){
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
        wrapper2.eq("id", sysClassDTO.getTeacherUserId())
                .eq("org_id", org.getId())
                .eq("role", "TEACHER")
                .eq("status", 1);
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
        if(classCode.isBlank() || className.isBlank()){
            return -2;
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

    @Override
    public Integer updateClass(SysClassUpdateDTO sysClassUpdateDTO, Long id) {
        SysClass sysClass = baseMapper.selectById(id);
        if(sysClass == null){
            return -4;
        }
        if(sysClassUpdateDTO == null || sysClassUpdateDTO.getClassName() == null || sysClassUpdateDTO.getAdmissionYear() == null || sysClassUpdateDTO.getTeacherUserId() == null || sysClassUpdateDTO.getStatus() == null){
            return -2;
        }
        String className = sysClassUpdateDTO.getClassName().trim();
        Integer admissionYear = sysClassUpdateDTO.getAdmissionYear();
        Long teacherUserId = sysClassUpdateDTO.getTeacherUserId();
        Integer status = sysClassUpdateDTO.getStatus();
        String remark = sysClassUpdateDTO.getRemark() == null ? "" : sysClassUpdateDTO.getRemark().trim();
        if(className.isBlank()){
            return -2;
        }
        if(className.length() > 100 || remark.length() > 500 || admissionYear.compareTo(2020) < 0 ||  admissionYear.compareTo(3000) > 0 || (!Integer.valueOf(1).equals(status) && !Integer.valueOf(0).equals(status))){
            return -3;
        }
        QueryWrapper<SysUser> wrapper2 = new QueryWrapper<SysUser>();
        wrapper2.eq("id", teacherUserId)
                .eq("org_id", sysClass.getOrgId())
                .eq("role", "TEACHER")
                .eq("status", 1);
        SysUser teacher = sysUserMapper.selectOne(wrapper2);
        if (!teacherUserId.equals(sysClass.getTeacherUserId())) {
            // 原来的教师查询与校验代码
             if(teacher == null){
                 return -6;
             }
        }
        sysClass.setClassName(className);
        sysClass.setAdmissionYear(admissionYear);
        sysClass.setTeacherUserId(teacherUserId);
        sysClass.setStatus(status);
        sysClass.setRemark(remark);
        sysClass.setUpdateTime(LocalDateTime.now());
        if(baseMapper.updateById(sysClass) != 1) {
            throw new RuntimeException("更新失败");
        }
        return 1;
    }

    @Override
    public Integer deleteOneClass(Long id) {
        SysClass sysClass = baseMapper.selectById(id);
        if(sysClass == null){
            return -4;
        }
        // TODO 检查用户，奖状是否存在班级

        if(baseMapper.deleteById(id) != 1){
            throw new RuntimeException("删除失败");
        }
        return 1;
    }
}
