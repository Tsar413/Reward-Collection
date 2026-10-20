package com.study.rewardCollection.controller;

import com.study.rewardCollection.common.Result;
import com.study.rewardCollection.dto.SysOrgDTO;
import com.study.rewardCollection.entity.SysOrg;
import com.study.rewardCollection.service.SysOrgService;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sys-org")
public class SysOrgController {

    @Resource
    private SysOrgService service;

    @GetMapping("/all")
    public ResponseEntity<Result<List<SysOrg>>> getAllOrg(){
        return ResponseEntity.ok(Result.success(service.list()));
    }

    @GetMapping("/one/{id}")
    public ResponseEntity<Result<SysOrg>> getOneOrg(@PathVariable Long id){
        return ResponseEntity.ok(Result.success(service.getById(id)));
    }

    @PostMapping("/add")
    public ResponseEntity<Result<Integer>> addNewOrg(@RequestBody SysOrgDTO sysOrgDTO){
        Integer result = service.addNewOrg(sysOrgDTO);
        if(result == -2){
            return ResponseEntity.status(500).body(Result.error("存在空数据"));
        }
        return ResponseEntity.ok(Result.success(1));
    }
}
