package com.study.rewardCollection.controller;

import com.study.rewardCollection.common.Result;
import com.study.rewardCollection.dto.StudentImportResultDTO;
import com.study.rewardCollection.dto.SysUserRegisterDTO;
import com.study.rewardCollection.service.SysUserService;
import jakarta.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/sys-user")
public class SysUserController {
    @Resource
    private SysUserService service;

    @PostMapping("/registration-one")
    public ResponseEntity<Result<Integer>> registrationOne(@RequestBody SysUserRegisterDTO sysUserRegisterDTO){
        Integer result = service.registrationOne(sysUserRegisterDTO);
        if (result == -2) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Result<>(400, "基本信息不能为空", null));
        }
        if (result == -3) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Result<>(400, "姓名、用户编号、备注长度或密码长度不符合要求", null));
        }
        if (result == -4) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Result<>(400, "角色只能为 TEACHER、ADMIN 或 STUDENT", null));
        }
        if (result == -5) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Result<>(404, "组织不存在或已停用", null));
        }
        if (result == -6) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new Result<>(409, "用户编号已存在", null));
        }
        if (result == -7) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Result<>(404, "班级不存在、已停用或不属于该组织", null));
        }
        if (result != 1) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new Result<>(500, "创建用户失败", null));
        }
        return ResponseEntity.ok(Result.success(result));
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Result<StudentImportResultDTO>> importStudents(@RequestParam("orgId") Long orgId, @RequestParam("file") MultipartFile file) {
        StudentImportResultDTO report = service.importStudents(orgId, file);
        String message = "处理完成：成功" + report.getSuccessCount() + "人，跳过"
                + report.getSkippedCount() + "人，失败" + report.getFailedCount() + "人";
        return ResponseEntity.ok(new Result<>(200, message, report));
    }

    
}
