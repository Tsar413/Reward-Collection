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
        SysOrg sysOrg = service.getById(id);
        if (sysOrg == null) {
            return ResponseEntity.status(404).body(new Result<SysOrg>(404, "组织不存在", null));
        }
        return ResponseEntity.ok(Result.success(sysOrg));
    }

    @PostMapping("/add")
    public ResponseEntity<Result<Integer>> addNewOrg(@RequestBody SysOrgDTO sysOrgDTO){
        Integer result = service.addNewOrg(sysOrgDTO);
        if (result == -2) {
            return ResponseEntity.status(400).body(Result.badRequest("组织编号和名称不能为空"));
        }
        if (result == -3) {
            return ResponseEntity.status(400).body(Result.badRequest("组织编号最多50字、名称最多100字、备注最多500字"));
        }
        if (result == -4) {
            return ResponseEntity.status(409).body(new Result<Integer>(409, "组织编号已存在", null));
        }
        return ResponseEntity.ok(Result.success(result));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<Result<Integer>> updateOrg(@RequestBody SysOrgDTO sysOrgDTO, @PathVariable Long id){
        Integer result = service.updateOrg(sysOrgDTO, id);
        if (result == -2) {
            return ResponseEntity.status(400).body(Result.badRequest("名称不能为空"));
        }
        if (result == -3) {
            return ResponseEntity.status(400).body(Result.badRequest("名称最多100字、备注最多500字"));
        }
        if (result == -4) {
            return ResponseEntity.status(404).body(new Result<Integer>(404, "组织不存在", null));
        }
        return ResponseEntity.ok(Result.success(result));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Result<Integer>> updateOrg(@PathVariable Long id){
        Integer result = service.deleteOrg(id);
        if (result == -4) {
            return ResponseEntity.status(404).body(new Result<Integer>(404, "组织不存在", null));
        }
        if (result == -2) {
            return ResponseEntity.status(400).body(new Result<Integer>(400, "组织中存在班级", null));
        }
        return ResponseEntity.ok(Result.success(result));
    }
}
