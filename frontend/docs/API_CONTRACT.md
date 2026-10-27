# 已按源码核对的 API 合约

基准 main / 59c4d3d8df5d13e8748c2eb83601b6473bb8ca62；与已验收版本无差异。读取范围：全部 Controller、DTO、ServiceImpl、DataScopeService、SysLoginInterceptor、GlobalExceptionHandler、AwardImageStorageUtil。

响应使用 Result.data。新增用户/班级/奖状通常为 1；保存后重新查询列表。除登录外请求全部附带 Bearer。权限判定与 Token 失效均由后端完成。

| 资源 | 读取                                                            | 创建                                | 修改                                        | 删除                             |
| ---- | --------------------------------------------------------------- | ----------------------------------- | ------------------------------------------- | -------------------------------- |
| 登录 | GET /sys-login/check                                            | POST /sys-login/login               | —                                           | POST /sys-login/logout           |
| 组织 | GET /sys-org/all, /one/{id}                                     | 关闭，不提供 UI                     | PUT /sys-org/update/{id}                    | DELETE /sys-org/delete/{id}      |
| 用户 | GET /sys-user/all, /one/{id}, /org/{orgId}, /class/{classId}    | POST /sys-user/registration-one     | PUT /sys-user/update/{id}                   | DELETE /sys-user/delete/{id}     |
| 班级 | GET /sys-class/all, /all/{orgId}, /one/{id}                     | POST /sys-class/add                 | PUT /sys-class/update/{id}                  | DELETE /sys-class/delete/{id}    |
| 奖状 | GET /award-record/all, /one/{id}, /students/{id}, /classes/{id} | POST /award-record/add multipart    | PUT /award-record/update/{id} JSON          | DELETE /award-record/delete/{id} |
| 图片 | GET /award-image/one/{awardId} 二进制                           | 随新增奖状                          | PUT /award-image/upload/{awardId} multipart | 随奖状删除                       |
| 导入 | —                                                               | POST /sys-user/import，file + orgId | —                                           | —                                |

以上均以 /api 为前缀。组织详情缺失 404，用户/班级/奖状详情缺失可以 200 + null。图片不存在/不可见 404，JSON 错误不可直接当图片展示。

## DTO 与限制

- SysOrgDTO 修改只取 orgName(100)、remark(500)，没有 status。不开放新增组织、组织停用。
- 用户注册 orgId、realName(50)、userNo(50)、password(8–64)、role 必填，学生 classId 必填，remark(500)。教师工号必填。更新只传 realName、classId、status(0/1)、remark，用户编号/角色/组织不变。
- 班级创建 orgId、classCode(50)、className(100)、admissionYear(2020–3000)、teacherUserId、remark(500)。修改不传 orgId/classCode，增加 status。
- 奖状名称(200)、颁发单位(200)、日期必填；活动(200)、类别/级别/等级(50)、证书编号(100)、描述(5000)。创建 PERSONAL 传 studentUserId，CLASS 传 classId；学生个人上传由后端强制本人。文字更新只传上述文字字段和日期。
- 图片 JPEG/PNG/WebP，最多 5MiB，后端真实解码，单边 <=10000 且 <=2500万像素。存储路径由服务端生成。
- Excel 只导入学生，表头“学号、姓名、班级编号”，最多 1000 行。HTTP 200 也可能部分失败。显示 total、successCount、skippedCount、failedCount 和 details。

## 数据权限

管理员可跨组织；教师只看本人及负责班级学生、自己班级与奖状；学生看本人、当前班级、自身个人奖状和本班集体奖状。前端不以多请求越权补全名字。

账号删除、班级删除、组织删除受真实关联约束；错误消息显示后端原因。500 保留表单和会话以便重试，401 除外。
