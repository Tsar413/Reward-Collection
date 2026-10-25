package com.study.rewardCollection.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.study.rewardCollection.dto.SysUserLoginDTO;
import com.study.rewardCollection.dto.SysUserLoginResultDTO;
import com.study.rewardCollection.entity.SysLoginToken;
import com.study.rewardCollection.entity.SysOrg;
import com.study.rewardCollection.entity.SysUser;
import com.study.rewardCollection.mapper.SysLoginTokenMapper;
import com.study.rewardCollection.mapper.SysOrgMapper;
import com.study.rewardCollection.mapper.SysUserMapper;
import com.study.rewardCollection.service.SysLoginService;
import com.study.rewardCollection.util.PasswordUtil;
import com.study.rewardCollection.util.TokenUtil;
import jakarta.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
public class SysLoginServiceImpl extends ServiceImpl<SysLoginTokenMapper, SysLoginToken> implements SysLoginService {

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private SysOrgMapper sysOrgMapper;

    @Override
    @Transactional
    public SysUserLoginResultDTO login(SysUserLoginDTO dto) {
        if (dto == null || dto.getUserNo() == null || dto.getPassword() == null) {
            return null;
        }
        String userNo = dto.getUserNo().trim();
        String password = dto.getPassword();
        if (userNo.isBlank() || userNo.length() > 50 || password.isEmpty() || password.length() > 64) {
            return null;
        }
        SysUser user = sysUserMapper.selectOne(new QueryWrapper<SysUser>().eq("user_no", userNo).eq("status", 1));
        if (user == null) {
            return null;
        }
        boolean matched = PasswordUtil.verifyPassword(password, user.getPasswordSalt(), user.getPasswordHash(), user.getPasswordAlgorithm());
        if (!matched) {
            return null;
        }
        if (!isOrgEnabled(user.getOrgId())) {
            return null;
        }

        SysLoginToken record = baseMapper.selectOne(new QueryWrapper<SysLoginToken>().eq("user_id", user.getId()));
        LocalDateTime now = LocalDateTime.now();
        String token = TokenUtil.generateToken();
        String tokenHash = TokenUtil.hashToken(token);
        if (record == null) {
            record = new SysLoginToken();
            record.setUserId(user.getId());
            record.setCreateTime(now);
            record.setUpdateTime(now);
            record.setExpireTime(now.plusDays(3));
            record.setRevokeTime(null);
            record.setTokenHash(tokenHash);
            if (baseMapper.insert(record) != 1) {
                throw new IllegalStateException("生成登录令牌失败");
            }

        } else {
            // 保留原记录创建时间，替换旧Token。
            record.setExpireTime(now.plusDays(3));
            record.setUpdateTime(now);
            record.setRevokeTime(null);
            record.setTokenHash(tokenHash);
            if (baseMapper.updateById(record) != 1) {
                throw new IllegalStateException("更新登录令牌失败");
            }
        }
        // 只更新登录时间，避免覆盖其他用户资料。
        int userResult = sysUserMapper.update(null,
                new UpdateWrapper<SysUser>().eq("id", user.getId()).set("last_login_time", now).set("update_time", now)
        );
        if (userResult != 1) {
            throw new IllegalStateException("更新登录时间失败");
        }
        SysUserLoginResultDTO result = new SysUserLoginResultDTO();
        result.setToken(token);
        result.setUser(toUserInfo(user));
        return result;
    }

    /**
     * 校验原始Token，成功后返回用户。
     */
    @Override
    public SysUser checkToken(String token) {
        checkTokenFormat(token);
        SysLoginToken record = baseMapper.selectOne(new QueryWrapper<SysLoginToken>().eq("token_hash", TokenUtil.hashToken(token)));
        if (record == null) {
            throw unauthorized("登录令牌无效，请重新登录");
        }
        if (record.getRevokeTime() != null) {
            throw unauthorized("登录已退出，请重新登录");
        }

        // 到期时间为空，或到期时间不晚于当前时间，均视为失效。
        if (record.getExpireTime() == null
                || !record.getExpireTime().isAfter(LocalDateTime.now())) {
            throw unauthorized("登录已过期，请重新登录");
        }

        SysUser user = sysUserMapper.selectById(record.getUserId());

        if (user == null || !Integer.valueOf(1).equals(user.getStatus())) {
            throw unauthorized("账号不存在或已停用");
        }

        if (!isOrgEnabled(user.getOrgId())) {
            throw unauthorized("所属组织不存在或已停用");
        }

        return user;
    }

    /**
     * 撤销当前请求携带的Token。
     */
    @Override
    @Transactional
    public void logout(String token) {

        checkTokenFormat(token);

        LocalDateTime now = LocalDateTime.now();

        baseMapper.update(
                null,
                new UpdateWrapper<SysLoginToken>()
                        .eq("token_hash", TokenUtil.hashToken(token))
                        .isNull("revoke_time")
                        .set("revoke_time", now)
                        .set("update_time", now)
        );

        // 如果Token已被新的登录替换，无需操作新Token。
    }

    private void checkTokenFormat(String token) {
        // TokenUtil生成64位小写十六进制字符串。
        if (token == null || !token.matches("[0-9a-f]{64}")) {
            throw unauthorized("登录令牌格式错误，请重新登录");
        }
    }

    private boolean isOrgEnabled(Long orgId) {
        if (orgId == null) {
            return false;
        }
        // sys_org主键是id，不是org_id。
        SysOrg org = sysOrgMapper.selectById(orgId);
        return org != null && Integer.valueOf(1).equals(org.getStatus());
    }

    private ResponseStatusException unauthorized(String message) {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, message);
    }

    private SysUserLoginResultDTO.UserInfo toUserInfo(SysUser user) {
        SysUserLoginResultDTO.UserInfo info = new SysUserLoginResultDTO.UserInfo();
        info.setId(user.getId());
        info.setOrgId(user.getOrgId());
        info.setClassId(user.getClassId());
        info.setUserNo(user.getUserNo());
        info.setUsername(user.getUsername());
        info.setRealName(user.getRealName());
        info.setRole(user.getRole());
        info.setStatus(user.getStatus());
        return info;
    }
}