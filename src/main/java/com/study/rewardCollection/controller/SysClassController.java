package com.study.rewardCollection.controller;

import com.study.rewardCollection.common.Result;
import com.study.rewardCollection.dto.SysClassDTO;
import com.study.rewardCollection.entity.SysClass;
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

    @GetMapping("/all")
    public ResponseEntity<Result<List<SysClass>>> getAllClasses(){
        return ResponseEntity.ok(Result.success(service.list()));
    }

    @GetMapping("/one/{id}")
    public ResponseEntity<Result<SysClass>> getOneClass(@PathVariable Long id){
        return ResponseEntity.ok(Result.success(service.getById(id)));
    }

    @PostMapping("/add")
    public ResponseEntity<Result<Integer>> addOneClass(@RequestBody SysClassDTO sysClassDTO){
        Integer result = service.addNewClass(sysClassDTO);

        return null;
    }
}
