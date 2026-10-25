package com.study.rewardCollection.controller;

import com.study.rewardCollection.common.Result;
import com.study.rewardCollection.dto.SysUserLoginDTO;
import com.study.rewardCollection.dto.SysUserLoginResultDTO;
import com.study.rewardCollection.service.SysLoginService;
import jakarta.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sys-login")
public class SysLoginController {

    @Resource
    private SysLoginService service;

    @PostMapping("/login")
    public ResponseEntity<Result<SysUserLoginResultDTO>> login(@RequestBody SysUserLoginDTO sysUserLoginDTO){
        SysUserLoginResultDTO result = service.login(sysUserLoginDTO);
        if(result == null){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Result<>(400, "用户名与密码组合错误或用户不存在/已停用或token生成失败", null));
        }
        return ResponseEntity.ok(Result.success(result));
    }


}
