package com.study.rewardCollection.controller;

import com.study.rewardCollection.common.Result;
import com.study.rewardCollection.dto.SysUserLoginDTO;
import com.study.rewardCollection.dto.SysUserLoginResultDTO;
import com.study.rewardCollection.service.SysLoginService;
import jakarta.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sys-login")
public class SysLoginController {

    @Resource
    private SysLoginService service;

    @PostMapping("/login")
    public ResponseEntity<Result<SysUserLoginResultDTO>> login(@RequestBody SysUserLoginDTO dto) {
        SysUserLoginResultDTO result = service.login(dto);
        if (result == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new Result<>(401, "用户编号或密码错误，或账号及组织已停用", null));
        }
        return ResponseEntity.ok(Result.success(result));
    }

    /**
     * 能进入此方法，说明Filter已校验通过。
     */
    @GetMapping("/check")
    public ResponseEntity<Result<Integer>> check() {
        return ResponseEntity.ok(Result.success(1));
    }

    @PostMapping("/logout")
    public ResponseEntity<Result<Integer>> logout(@RequestAttribute("loginToken") String token) {
        service.logout(token);
        return ResponseEntity.ok(Result.success(1));
    }
}