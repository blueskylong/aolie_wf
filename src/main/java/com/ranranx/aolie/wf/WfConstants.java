package com.ranranx.aolie.wf;

import com.ranranx.aolie.core.common.CommonUtils;
import com.ranranx.aolie.core.common.Constants;

/**
 * @author xxl
 * @version V0.0.1
 * @date 2021/3/8 0008 12:25
 **/
public class WfConstants {
    /**
     * 默认的流程方案
     */
    public static final Long DEFAULT_WF_SCHEMA = 4L;

    /**
     * 流程条件过滤字段的前缀
     */
    public static final String FLOW_FILTER_PREFIX = "WF_STATE";

    /**
     * 流程放入操作全局参数中的条件名.
     */
    public static final String FLOW_GLOBAL_CONDITION_OPER_NAME = "FLOW_GLOBAL_CONDITION_NAME";
    public static final String FLOW_GLOBAL_CONDITION_FIELD_NAME = "FLOW_GLOBAL_CONDITION_FIELD_NAME";
    public static final String FLOW_GLOBAL_CONDITION_TABLE_ID = "FLOW_GLOBAL_CONDITION_TABLE_ID";
    public static final String FLOW_GLOBAL_CONDITION_FLOW_ID = "FLOW_GLOBAL_CONDITION_FLOW_ID";

    /**
     * 流程中的虚拟节点
     */
    public static class WfTypeNode {
        /*待办*/
        public static final String NODE_TODO = "_TODO";
        /*全部*/
        public static final String NODE_ALL = "_ALL";
        /*已审核*/
        public static final String NODE_PASS = "_PASS";
    }

    /**
     * 审核表中的几个流程相关字段
     */
    public static class WfAuditField {
        public static final String wfIdField = "wf_id";
        public static final String procInstId = "proc_inst_id";
        public static final String tableId = "table_id";
        public static final String bizField = "business_id";
        public static final String actField = "act_id";


    }

    public static class WfNodeConstAttr {
        /**
         * 排序参数，值要是数字型，如果没有设置，则取一个顺序 号
         */
        public static final String order = "STATUS_ORDER";
        /**
         * 状态名称参数，如果没有设置，则取节点名称
         */
        public static final String name = "STATUS_NAME";
        public static final String code = "STATUS_CODE";
        /**
         * 指定节点是不是不算数据状态，如果不是，则不会在流程节点里选择到
         */
        public static final String ignore = "IGNORE_NODE";
        /**
         * 审核类型
         */
        public static final String auditType = "AUDIT_TYPE";
    }

    /**
     * 流程操作类型
     */
    public static class WfOperType {
        /**
         * 提交
         */
        public static final short TYPE_COMMIT = 1;
        /**
         * 被退回
         */
        public static final short TYPE_BACK = -1;

        public static final short TYPE_CREATE = 0;
        /**
         * 主动退回
         */
        public static final short ROLL_BACK = -2;
    }

    /**
     * 固定的前后节点的定义
     */
    public static class FixNodeIds {
        /**
         * 流程已完成节点的虚拟ID
         */
        public static final String finished = "1";

    }

    /**
     * 客户端显示列的名称
     */
    public final static String CLIENT_DISPLAY_FIELD_NAME = Constants.FixColumnName.PLUG_FILTER_PREFIX+"WF_STATE";

    public final static String KEY_ASSIGNEE = "assignee";

    /**
     * 字典KEY：候选人
     */
    public final static String KEY_CANDIDATE_USERS = "candidateUsers";

    /**
     * 字典KEY：候选组
     */
    public final static String KEY_CANDIDATE_GROUPS = "candidateGroups";


    /**
     * 查询已办任务：包括流程进行中和流程已结束
     */
    public final static int DONE_TASK_ALL = 0;
    /**
     * 查询已办任务：流程进行中
     */
    public final static int DONE_TASK_UNFINISH = 1;
    /**
     * 查询已办任务：流程已结束
     */
    public final static int DONE_TASK_FINISH = 2;
}
