package com.study.rewardCollection.security;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.study.rewardCollection.dto.SysUserRegisterDTO;
import com.study.rewardCollection.dto.SysUserUpdateDTO;
import com.study.rewardCollection.entity.AwardRecord;
import com.study.rewardCollection.entity.SysClass;
import com.study.rewardCollection.entity.SysOrg;
import com.study.rewardCollection.entity.SysUser;
import com.study.rewardCollection.mapper.SysClassMapper;
import jakarta.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

@Service
public class DataScopeService {

    @Resource
    private SysClassMapper sysClassMapper;

    /**
     * 获取经过Token校验的当前用户。
     */
    public SysUser currentUser() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        Object value = attributes.getRequest().getAttribute("loginUser");
        if (!(value instanceof SysUser user) || user.getId() == null || user.getOrgId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        if (!isAdmin(user) && !isTeacher(user) && !"STUDENT".equals(user.getRole())) {
            throw forbidden("用户角色不合法");
        }
        return user;
    }

    /**
     * 用户查询范围。
     */
    public QueryWrapper<SysUser> userQuery() {

        SysUser me = currentUser();
        QueryWrapper<SysUser> query = new QueryWrapper<>();

        if (isAdmin(me)) {
            return query;
        }

        query.eq("org_id", me.getOrgId());

        if (isTeacher(me)) {
            List<Long> classIds = teacherClassIds(me);

            if (classIds.isEmpty()) {
                query.eq("id", me.getId());
            } else {
                query.and(w -> w.eq("id", me.getId())
                        .or(s -> s.eq("role", "STUDENT")
                                .in("class_id", classIds)));
            }

            return query;
        }

        return query.eq("id", me.getId());
    }

    /**
     * 班级查询范围。
     */
    public QueryWrapper<SysClass> classQuery() {

        SysUser me = currentUser();
        QueryWrapper<SysClass> query = new QueryWrapper<>();

        if (isAdmin(me)) {
            return query;
        }

        query.eq("org_id", me.getOrgId());

        if (isTeacher(me)) {
            return query.eq("teacher_user_id", me.getId());
        }

        return query.eq(
                "id",
                me.getClassId() == null ? -1L : me.getClassId()
        );
    }

    /**
     * 组织查询范围。
     */
    public QueryWrapper<SysOrg> orgQuery() {

        SysUser me = currentUser();
        QueryWrapper<SysOrg> query = new QueryWrapper<>();

        if (isAdmin(me)) {
            return query;
        }

        return query.eq("id", me.getOrgId());
    }

    /**
     * 注册用户之前调用。
     */
    public void checkCreateUser(SysUserRegisterDTO dto) {
        SysUser me = currentUser();
        checkSameOrg(me, dto.getOrgId());
        if (isAdmin(me)) {
            return;
        }
        String role = dto.getRole() == null ? "" : dto.getRole().trim();
        if (!isTeacher(me) || !"STUDENT".equals(role)) {
            throw forbidden("只能添加自己负责班级的学生");
        }
        checkTeacherClass(me, dto.getClassId());
    }

    /**
     * Excel导入入口检查。
     */
    public void checkImport(Long orgId) {
        SysUser me = currentUser();
        checkSameOrg(me, orgId);
        if (!isAdmin(me) && !isTeacher(me)) {
            throw forbidden("没有导入学生的权限");
        }
    }

    /**
     * Excel每行解析出班级后检查。
     */
    public boolean canImportClass(SysClass sysClass) {

        SysUser me = currentUser();

        if (sysClass == null) {
            return false;
        }

        if (isAdmin(me)) {
            return true;
        }

        if (!Objects.equals(me.getOrgId(), sysClass.getOrgId())) {
            return false;
        }

        return isTeacher(me)
                && Objects.equals(
                me.getId(),
                sysClass.getTeacherUserId()
        );
    }

    /**
     * 修改用户之前调用。
     */
    public void checkUpdateUser(SysUser target, SysUserUpdateDTO dto) {
        SysUser me = currentUser();
        checkSameOrg(me, target.getOrgId());
        if (isAdmin(me)) {
            return;
        }
        if (Objects.equals(me.getId(), target.getId())) {
            // 教师、学生修改本人，只允许姓名和备注。
            if (!Objects.equals(dto.getStatus(), target.getStatus()) || !Objects.equals(dto.getClassId(), target.getClassId())) {
                throw forbidden("不能自行修改状态或所属班级");
            }
            return;
        }
        if (!isTeacher(me) || !"STUDENT".equals(target.getRole())) {
            throw forbidden("没有修改该用户的权限");
        }
        // 原班级、新班级都必须由当前教师负责。
        checkTeacherClass(me, target.getClassId());
        checkTeacherClass(me, dto.getClassId());
    }

    /**
     * 删除用户之前调用。
     */
    public void checkDeleteUser(SysUser target) {
        SysUser me = currentUser();
        checkSameOrg(me, target.getOrgId());
        if (isAdmin(me)) {
            return;
        }
        if (!isTeacher(me) || !"STUDENT".equals(target.getRole())) {
            throw forbidden("没有删除该用户的权限");
        }
        checkTeacherClass(me, target.getClassId());
    }

    /**
     * 新增班级之前调用。
     */
    public void checkCreateClass(Long orgId) {
        SysUser me = currentUser();
        checkSameOrg(me, orgId);
        if (!isAdmin(me)) {
            throw forbidden("只有管理员可以新增班级");
        }
    }

    /**
     * 修改、删除班级之前调用。
     */
    public void checkClassWrite(SysClass target) {
        SysUser me = currentUser();
        checkSameOrg(me, target.getOrgId());
        if (!isAdmin(me)) {
            throw forbidden("只有管理员可以修改或删除班级");
        }
    }

    /**
     * 修改、删除组织之前调用。
     */
    public void checkOrgWrite(Long orgId) {
        SysUser me = currentUser();
        checkSameOrg(me, orgId);
        if (!isAdmin(me)) {
            throw forbidden("只有管理员可以修改或删除组织");
        }
    }

    /**
     * 本阶段不开放创建其他组织。
     */
    public void checkOrgCreate() {
        throw forbidden("当前不开放新增组织，请通过平台初始化建立");
    }

    private List<Long> teacherClassIds(SysUser teacher) {
        return sysClassMapper.selectList(
                new QueryWrapper<SysClass>()
                        .eq("org_id", teacher.getOrgId())
                        .eq("teacher_user_id", teacher.getId())
        ).stream().map(SysClass::getId).toList();
    }

    private void checkTeacherClass(SysUser teacher, Long classId) {
        if (classId == null) {
            throw forbidden("没有该班级的管理权限");
        }
        Long count = sysClassMapper.selectCount(
                new QueryWrapper<SysClass>()
                        .eq("id", classId)
                        .eq("org_id", teacher.getOrgId())
                        .eq("teacher_user_id", teacher.getId())
        );
        if (count == 0) {
            throw forbidden("只能管理自己负责班级的学生");
        }
    }

    private void checkSameOrg(SysUser me, Long orgId) {

        if (isAdmin(me)) {
            return;
        }

        if (!Objects.equals(me.getOrgId(), orgId)) {
            throw forbidden("不能操作其他组织的数据");
        }
    }

    private boolean isAdmin(SysUser user) {
        return "ADMIN".equals(user.getRole());
    }

    private boolean isTeacher(SysUser user) {
        return "TEACHER".equals(user.getRole());
    }

    private ResponseStatusException forbidden(String message) {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, message);
    }

    /**
     * 新增奖状的权限与归属检查。
     * student：个人奖状的获奖学生；班级奖状传 null。
     * targetClass：奖状所属班级。
     */
    public void checkCreateAward(
            String awardType,
            SysUser student,
            SysClass targetClass
    ) {
        SysUser me = currentUser();

        if (targetClass == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "奖状所属班级不存在"
            );
        }

        if (!"PERSONAL".equals(awardType) && !"CLASS".equals(awardType)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "奖状类型错误"
            );
        }

        // ADMIN 可以跨组织；其他角色只能操作本组织。
        checkSameOrg(me, targetClass.getOrgId());

        if ("CLASS".equals(awardType)) {
            if (isAdmin(me)) {
                return;
            }

            if (isTeacher(me)) {
                checkTeacherClass(me, targetClass.getId());
                return;
            }

            throw forbidden("学生不能上传班级奖状");
        }

        // 个人奖状必须归属于学生。
        if (student == null || !"STUDENT".equals(student.getRole())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "获奖用户必须是学生"
            );
        }

        if (!Objects.equals(student.getClassId(), targetClass.getId())
                || !Objects.equals(student.getOrgId(), targetClass.getOrgId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "学生与班级的归属不一致"
            );
        }

        if (isAdmin(me)) {
            return;
        }

        if (isTeacher(me)) {
            checkTeacherClass(me, targetClass.getId());
            return;
        }

        if (!Objects.equals(me.getId(), student.getId())) {
            throw forbidden("只能上传自己的个人奖状");
        }
    }

    /**
     * 奖状查询范围。
     */
    public QueryWrapper<AwardRecord> awardQuery() {
        SysUser me = currentUser();
        QueryWrapper<AwardRecord> query = new QueryWrapper<>();

        // 管理员可以查询各组织奖状。
        if (isAdmin(me)) {
            return query;
        }

        query.eq("org_id", me.getOrgId());

        // 教师可以查询自己负责班级内的个人奖状、班级奖状。
        if (isTeacher(me)) {
            List<Long> classIds = teacherClassIds(me);

            if (classIds.isEmpty()) {
                return query.eq("id", -1L);
            }

            return query.in("class_id", classIds);
        }

        // 没有所属班级时，只查询自己的个人奖状。
        if (me.getClassId() == null) {
            return query.eq("award_type", "PERSONAL")
                    .eq("student_user_id", me.getId());
        }

        // 学生：自己的个人奖状，或当前班级的班级奖状。
        return query.and(w -> w
                .eq("award_type", "PERSONAL")
                .eq("student_user_id", me.getId())
                .or(c -> c
                        .eq("award_type", "CLASS")
                        .eq("class_id", me.getClassId())
                )
        );
    }

    /**
     * 修改、删除奖状之前调用。
     */
    public void checkAwardWrite(AwardRecord target) {
        SysUser me = currentUser();

        checkSameOrg(me, target.getOrgId());

        if (isAdmin(me)) {
            return;
        }

        // 按奖状保存的班级判断教师权限。
        if (isTeacher(me)) {
            checkTeacherClass(me, target.getClassId());
            return;
        }

        // 学生只能修改、删除自己的个人奖状。
        if (!"PERSONAL".equals(target.getAwardType())
                || !Objects.equals(me.getId(), target.getStudentUserId())) {
            throw forbidden("只能修改或删除自己的个人奖状");
        }
    }
}