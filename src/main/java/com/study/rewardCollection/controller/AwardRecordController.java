package com.study.rewardCollection.controller;

import com.study.rewardCollection.common.Result;
import com.study.rewardCollection.dto.AwardRecordAddDTO;
import com.study.rewardCollection.dto.AwardRecordUpdateDTO;
import com.study.rewardCollection.entity.AwardRecord;
import com.study.rewardCollection.security.DataScopeService;
import com.study.rewardCollection.service.AwardRecordService;
import jakarta.annotation.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/award-record")
public class AwardRecordController {

    @Resource
    private AwardRecordService service;

    @Resource
    private DataScopeService dataScopeService;

    @GetMapping("/all")
    public ResponseEntity<Result<List<AwardRecord>>> getAllAwardRecords() {
        return ResponseEntity.ok(Result.success(service.list(dataScopeService.awardQuery())));
    }

    @GetMapping("/classes/{classId}")
    public ResponseEntity<Result<List<AwardRecord>>> getByClassId(@PathVariable Long classId) {
        return ResponseEntity.ok(Result.success(service.list(dataScopeService.awardQuery().eq("class_id", classId))));
    }

    @GetMapping("/students/{studentUserId}")
    public ResponseEntity<Result<List<AwardRecord>>> getByStudentId(@PathVariable Long studentUserId) {
        return ResponseEntity.ok(Result.success(service.list(dataScopeService.awardQuery().eq("student_user_id", studentUserId))));
    }

    @GetMapping("/one/{id}")
    public ResponseEntity<Result<AwardRecord>> getOneAwardRecord(@PathVariable Long id) {
        AwardRecord awardRecord = service.getOne(dataScopeService.awardQuery().eq("id", id));
        // 按此前约定：不存在或不在可见范围内，返回成功和 null。
        return ResponseEntity.ok(Result.success(awardRecord));
    }

    @PostMapping(value = "/add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Result<Integer>> addNewRewardRecord(@RequestPart("data") AwardRecordAddDTO recordAddDTO, @RequestPart("file") MultipartFile file){
        Integer result = service.addAwardRecord(recordAddDTO, file);
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

    @PutMapping("/update/{id}")
    public ResponseEntity<Result<Integer>> updateAwardRecord(@PathVariable Long id, @RequestBody AwardRecordUpdateDTO recordUpdateDTO) {
        Integer result = service.updateAwardRecord(id, recordUpdateDTO);
        if (result == -2) {
            return ResponseEntity.status(400).body(Result.badRequest("必填数据不完整"));
        }
        if (result == -6) {
            return ResponseEntity.status(400).body(Result.badRequest("数据长度有误"));
        }
        if (result == -7) {
            return ResponseEntity.status(404).body(new Result<Integer>(404, "该奖状不存在", null));
        }
        return ResponseEntity.ok(Result.success(result));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Result<Integer>> deleteAwardRecord(@PathVariable Long id) {
        Integer result = service.deleteAwardRecord(id);
        if (result == -2) {
            return ResponseEntity.status(400).body(Result.badRequest("奖状ID不能为空"));
        }
        if (result == -7) {
            return ResponseEntity.status(404).body(new Result<Integer>(404, "该奖状不存在", null));
        }
        return ResponseEntity.ok(Result.success(result));
    }
}
