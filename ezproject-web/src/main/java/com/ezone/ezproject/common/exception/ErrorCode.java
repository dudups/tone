package com.ezone.ezproject.common.exception;

public class ErrorCode {
    /**
     * 关闭卡片状态，需要指定一个合法的目标迁移状态
     */
    public static final int REQUIRED_MIGRATE_STATUS = 5001;

    /**
     * 修改卡片状态，目标状态对应有必填字段尚未填写
     */
    public static final int REQUIRED_FIELDS = 5010;
    /**
     * 修改卡片状态到目标状态，必须发起BPM审批流
     */
    public static final int REQUIRED_BPM_FLOW = 5011;
    /**
     * 卡片发起BPM审批流到目标状态，但当前未绑定审批流
     */
    public static final int NO_BPM_FLOW = 5012;

    /**
     * 修改状态/发审批时，卡片当前关联了进行中的审批流
     */
    public static final int IN_BPM_FLOW = 5013;

    public static final int KEY_CONFLICT = 5020;
    public static final int NAME_CONFLICT = 5021;

    /**
     * 删除角色时，需要迁移角色下的用户。
     */
    public static final int NEED_MIGRATE_ROLE_USER = 5030;

    public static final int INVALID_TOKEN = 5498;
}
