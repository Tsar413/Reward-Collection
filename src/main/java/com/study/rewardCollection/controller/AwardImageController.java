package com.study.rewardCollection.controller;

import com.study.rewardCollection.common.Result;
import com.study.rewardCollection.service.AwardImageService;
import jakarta.annotation.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/award-image")
public class AwardImageController {

    @Resource
    private AwardImageService service;

    @PutMapping(value = "/upload/{awardId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Result<Integer>> uploadImage(@PathVariable Long awardId, @RequestPart("file") MultipartFile file) {
        service.saveImage(awardId, file);
        return ResponseEntity.ok(Result.success(1));
    }

    @GetMapping("/one/{awardId}")
    public ResponseEntity<byte[]> getAwardImage(@PathVariable Long awardId) {
        AwardImageService.ImageContent content = service.readImage(awardId);
        return ResponseEntity.ok()
                .contentType(
                        MediaType.parseMediaType(content.mimeType())
                )
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .body(content.data());
    }
}