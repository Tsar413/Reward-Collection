package com.study.rewardCollection.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.study.rewardCollection.dto.StudentImportResultDTO;
import com.study.rewardCollection.dto.SysUserRegisterDTO;
import com.study.rewardCollection.entity.SysClass;
import com.study.rewardCollection.entity.SysOrg;
import com.study.rewardCollection.entity.SysUser;
import com.study.rewardCollection.exception.ExcelImportException;
import com.study.rewardCollection.mapper.SysClassMapper;
import com.study.rewardCollection.mapper.SysOrgMapper;
import com.study.rewardCollection.mapper.SysUserMapper;
import com.study.rewardCollection.service.SysUserService;
import com.study.rewardCollection.util.PasswordUtil;
import com.study.rewardCollection.util.StudentExcelUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    @Resource
    private SysOrgMapper sysOrgMapper;

    @Resource
    private SysClassMapper sysClassMapper;

    @Resource
    private PlatformTransactionManager transactionManager;

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
        if (realName.isBlank() || userNo.isBlank() || role.isBlank()) {
            return -2;
        }
        if(realName.length() > 50 || (password.length() < 8 || password.length() > 64) || userNo.length() > 50 || remark.length() > 500){
            return -3;
        }
        if(!"TEACHER".equals(role) && !"ADMIN".equals(role) && !"STUDENT".equals(role)){
            return -4;
        }
        // 检查组织是否存在
        Long orgCount = sysOrgMapper.selectCount(new QueryWrapper<SysOrg>().eq("id", orgId).eq("status", 1));
        if(orgCount != 1){
            return -5;
        }
        // 检查用户id是否存在
        Long userCount = baseMapper.selectCount(new QueryWrapper<SysUser>().eq("org_id", orgId).eq("user_no", userNo));
        if(userCount != 0){
            return -6;
        }
        if(classId == null && "STUDENT".equals(role)){
            return -2;
        }
        // 检查班级是否存在
        if(classId != null && "STUDENT".equals(role)){
            Long classCount = sysClassMapper.selectCount(new QueryWrapper<SysClass>().eq("org_id", orgId).eq("id", classId).eq("status", 1));
            if(classCount != 1){
                return -7;
            }
        }
        SysUser sysUser = new SysUser();
        sysUser.setUserNo(userNo);
        sysUser.setRemark(remark);
        sysUser.setStatus(1);
        sysUser.setOrgId(orgId);
        sysUser.setRealName(realName);
        sysUser.setRole(role);
        if("TEACHER".equals(role) || "ADMIN".equals(role)){
            sysUser.setClassId(null);
        } else {
            sysUser.setClassId(classId);
        }
        sysUser.setLastLoginTime(null);
        sysUser.setUsername(username);
        String salt = PasswordUtil.generateSalt();
        sysUser.setPasswordSalt(salt);
        sysUser.setPasswordHash(PasswordUtil.hashPassword(password, salt));
        sysUser.setPasswordAlgorithm(PasswordUtil.ALGORITHM);
        LocalDateTime time = LocalDateTime.now();
        sysUser.setUpdateTime(time);
        sysUser.setCreateTime(time);
        if (baseMapper.insert(sysUser) != 1) {
            throw new IllegalStateException("创建用户失败");
        }
        return 1;
    }

    @Override
    public StudentImportResultDTO importStudents(Long orgId, MultipartFile file) {
        if (orgId == null || orgId <= 0) {
            throw new ExcelImportException("请选择组织");
        }
        SysOrg org = sysOrgMapper.selectById(orgId);
        if (org == null || !Integer.valueOf(1).equals(org.getStatus())) {
            throw new ExcelImportException("组织不存在或已停用");
        }
        // 先读取、检查整个文件，再开始写入数据库
        List<StudentExcelUtil.StudentRow> rows = StudentExcelUtil.readStudents(file);
        StudentImportResultDTO report = new StudentImportResultDTO();
        report.setTotal(rows.size());
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        for (StudentExcelUtil.StudentRow row : rows) {
            StudentImportResultDTO.RowResult result;
            if (row.errorMessage() != null) {
                // Excel中的单行格式错误
                result = new StudentImportResultDTO.RowResult(
                        row.rowNumber(),
                        row.userNo(),
                        "FAILED",
                        row.errorMessage()
                );
            } else {
                try {
                    result = template.execute(transactionStatus -> importOneStudent(orgId, row));
                    if (result == null) {
                        throw new IllegalStateException("单行导入结果为空");
                    }
                } catch (RuntimeException e) {
                    // 此时该行事务已回滚，不影响其他行
                    log.error(
                            "学生导入失败，组织ID：{}，Excel行号：{}，学号：{}",
                            orgId, row.rowNumber(), row.userNo(), e
                    );
                    result = new StudentImportResultDTO.RowResult(
                            row.rowNumber(),
                            row.userNo(),
                            "FAILED",
                            "创建失败，该行未成功导入，请检查后端日志"
                    );
                }
            }
            addImportResult(report, result);
        }
        return report;
    }

    /**
     * 在独立事务中处理一名学生。
     */
    private StudentImportResultDTO.RowResult importOneStudent(Long orgId, StudentExcelUtil.StudentRow row) {
        // 所有角色的编号都参加查重，包括停用账号
        Long userCount = baseMapper.selectCount(
                new QueryWrapper<SysUser>()
                        .eq("org_id", orgId)
                        .eq("user_no", row.userNo())
        );
        if (userCount > 0) {
            return new StudentImportResultDTO.RowResult(
                    row.rowNumber(),
                    row.userNo(),
                    "SKIPPED",
                    "用户编号已存在，未修改已有账号"
            );
        }
        // Excel中填写的是班级编号，不是数据库主键ID
        SysClass sysClass = sysClassMapper.selectOne(
                new QueryWrapper<SysClass>()
                        .eq("org_id", orgId)
                        .eq("class_code", row.classCode())
        );
        if (sysClass == null) {
            return new StudentImportResultDTO.RowResult(
                    row.rowNumber(),
                    row.userNo(),
                    "FAILED",
                    "当前组织中不存在班级：" + row.classCode()
            );
        }
        if (!Integer.valueOf(1).equals(sysClass.getStatus())) {
            return new StudentImportResultDTO.RowResult(
                    row.rowNumber(),
                    row.userNo(),
                    "FAILED",
                    "班级已停用：" + row.classCode()
            );
        }
        LocalDateTime now = LocalDateTime.now();
        SysUser user = new SysUser();
        user.setOrgId(orgId);
        user.setUserNo(row.userNo());
        user.setUsername(row.realName());
        user.setRealName(row.realName());
        user.setRole("STUDENT");
        user.setClassId(sysClass.getId());
        user.setRemark("");
        user.setStatus(1);
        user.setLastLoginTime(null);
        user.setCreateTime(now);
        user.setUpdateTime(now);
        // 密码相同，但每人使用独立随机盐
        String salt = PasswordUtil.generateSalt();
        user.setPasswordSalt(salt);
        user.setPasswordHash(
                PasswordUtil.hashPassword("123456", salt)
        );
        user.setPasswordAlgorithm(PasswordUtil.ALGORITHM);
        if (baseMapper.insert(user) != 1) {
            throw new IllegalStateException("创建学生失败");
        }
        return new StudentImportResultDTO.RowResult(
                row.rowNumber(),
                row.userNo(),
                "SUCCESS",
                "导入成功"
        );
    }

    /**
     * 汇总结果；成功行不写入details。
     */
    private void addImportResult(StudentImportResultDTO report, StudentImportResultDTO.RowResult result) {
        switch (result.status()) {
            case "SUCCESS" ->
                    report.setSuccessCount(report.getSuccessCount() + 1);
            case "SKIPPED" -> {
                report.setSkippedCount(report.getSkippedCount() + 1);
                report.getDetails().add(result);
            }
            default -> {
                report.setFailedCount(report.getFailedCount() + 1);
                report.getDetails().add(result);
            }
        }
    }
}
