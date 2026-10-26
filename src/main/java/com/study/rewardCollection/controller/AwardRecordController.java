package com.study.rewardCollection.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.study.rewardCollection.common.Result;
import com.study.rewardCollection.dto.AwardRecordAddDTO;
import com.study.rewardCollection.entity.AwardRecord;
import com.study.rewardCollection.service.AwardRecordService;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/award-record")
public class AwardRecordController {

    @Resource
    private AwardRecordService service;

    @GetMapping("/all")
    public ResponseEntity<Result<List<AwardRecord>>> getAllAwardRecords(){
        return ResponseEntity.ok(Result.success(service.list()));
    }

    @GetMapping("/classes/{classId}")
    public ResponseEntity<Result<List<AwardRecord>>> getByClassId(@PathVariable Long classId){
        return ResponseEntity.ok(Result.success(service.list(new QueryWrapper<AwardRecord>().eq("class_id", classId))));
    }

    @GetMapping("/students/{studentUserId}")
    public ResponseEntity<Result<List<AwardRecord>>> getByStudentId(@PathVariable Long studentUserId){
        return ResponseEntity.ok(Result.success(service.list(new QueryWrapper<AwardRecord>().eq("student_user_id", studentUserId))));
    }

    @PostMapping("/add")
    public ResponseEntity<Result<Integer>> addNewRewardRecord(@RequestBody AwardRecordAddDTO recordAddDTO){
        Integer result = service.addAwardRecord(recordAddDTO);
        if (result == -2) {
            return ResponseEntity.status(400).body(Result.badRequest("必填数据不完整"));
        }
        if (result == -3) {
            return ResponseEntity.status(400).body(Result.badRequest("类型错误"));
        }
        if (result == -4) {
            return ResponseEntity.status(404).body(new Result<Integer>(404, "该学生不存在", null));
        }
        if (result == -5) {
            return ResponseEntity.status(404).body(new Result<Integer>(404, "该班级不存在", null));
        }
        if (result == -6) {
            return ResponseEntity.status(400).body(Result.badRequest("数据长度有误"));
        }
        return ResponseEntity.ok(Result.success(result));
    }
}
