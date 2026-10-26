package com.study.rewardCollection.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.study.rewardCollection.common.Result;
import com.study.rewardCollection.dto.SysClassDTO;
import com.study.rewardCollection.dto.SysClassUpdateDTO;
import com.study.rewardCollection.entity.SysClass;
import com.study.rewardCollection.security.DataScopeService;
import com.study.rewardCollection.service.SysClassService;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sys-class")
public class SysClassController {

    @Resource
    private SysClassService service;

    @Resource
    private DataScopeService dataScope;

    @GetMapping("/all")
    public ResponseEntity<Result<List<SysClass>>> getAllClasses() {
        return ResponseEntity.ok(Result.success(service.list(dataScope.classQuery())));
    }

    @GetMapping("/all/{orgId}")
    public ResponseEntity<Result<List<SysClass>>> getClassesByOrgId(@PathVariable Long orgId) {
        return ResponseEntity.ok(Result.success(service.list(dataScope.classQuery().eq("org_id", orgId))));
    }

    @GetMapping("/one/{id}")
    public ResponseEntity<Result<SysClass>> getOneClass(@PathVariable Long id) {
        return ResponseEntity.ok(Result.success(service.getOne(dataScope.classQuery().eq("id", id))));
    }

    /**
     * 新增班级
     */
    @PostMapping("/add")
    public ResponseEntity<Result<Integer>> addOneClass(@RequestBody SysClassDTO sysClassDTO) {
        Integer result = service.addNewClass(sysClassDTO);
        if (result == -2) {
            return ResponseEntity.status(400).body(Result.badRequest("必填数据不完整"));
        }
        if (result == -3) {
            return ResponseEntity.status(400).body(Result.badRequest("组织不存在，或字段长度、入学年份不符合要求"));
        }
        if (result == -4) {
            return ResponseEntity.status(409).body(new Result<Integer>(409, "该组织内班级编号已存在", null));
        }
        if (result == -5) {
            return ResponseEntity.status(400).body(Result.badRequest("组织已停用，无法新增班级"));
        }
        if (result == -6) {
            return ResponseEntity.status(400).body(Result.badRequest("教师不存在、已停用或不属于该组织"));
        }
        return ResponseEntity.ok(Result.success(result));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<Result<Integer>> updateOneClass(@RequestBody SysClassUpdateDTO sysClassUpdateDTO, @PathVariable Long id){
        Integer result = service.updateClass(sysClassUpdateDTO, id);
        if (result == -2) {
            return ResponseEntity.status(400).body(Result.badRequest("必填数据不完整"));
        }
        if (result == -3) {
            return ResponseEntity.status(400).body(Result.badRequest("名称、备注、入学年份或状态不符合要求"));
        }
        if (result == -4) {
            return ResponseEntity.status(404).body(new Result<Integer>(404, "班级不存在", null));
        }
        if (result == -6) {
            return ResponseEntity.status(400).body(Result.badRequest("教师不存在、已停用或不属于该组织"));
        }
        return ResponseEntity.ok(Result.success(result));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Result<Integer>> deleteOneClass(@PathVariable Long id) {
        Integer result = service.deleteOneClass(id);
        if (result == -4) {
            return ResponseEntity.status(404).body(new Result<Integer>(404, "班级不存在", null));
        }
        if (result == -3) {
            return ResponseEntity.status(400).body(Result.badRequest("班级中存在学生，无法删除"));
        }
        if (result == -5) {
            return ResponseEntity.status(400).body(Result.badRequest("班级中存在奖状，无法删除"));
        }
        return ResponseEntity.ok(Result.success(result));
    }
}