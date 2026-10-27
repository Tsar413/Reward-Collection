package com.study.rewardCollection.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.study.rewardCollection.dto.StudentImportResultDTO;
import com.study.rewardCollection.dto.SysUserRegisterDTO;
import com.study.rewardCollection.dto.SysUserUpdateDTO;
import com.study.rewardCollection.entity.SysUser;
import org.springframework.web.multipart.MultipartFile;

public interface SysUserService extends IService<SysUser> {

    Integer registrationOne(SysUserRegisterDTO sysUserRegisterDTO);

    StudentImportResultDTO importStudents(Long orgId, MultipartFile file);

    Integer updateOneUser(SysUserUpdateDTO sysUserUpdateDTO, Long id);

    Integer deleteOneUser(Long id);
}
