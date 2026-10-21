package com.study.rewardCollection.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.study.rewardCollection.dto.SysOrgDTO;
import com.study.rewardCollection.entity.SysOrg;

public interface SysOrgService extends IService<SysOrg> {

    Integer addNewOrg(SysOrgDTO sysOrgDTO);

    Integer updateOrg(SysOrgDTO sysOrgDTO, Long id);

    Integer deleteOrg(Long id);
}
