package com.ranranx.aolie.wf.model;

import org.flowable.bpmn.model.Activity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 这是节点的动态信息,从流程节点的定义中取得,不存储到数据库
 *
 * @author xxl
 * @version V0.0.1
 * @date 2021/3/8 0008 22:11
 **/
public class WfNode implements Serializable {
    /**
     * 开始节点定义
     */
    public static final String START_NODE_ID = "0";
    /**
     * 结束节点定义
     */
    public static final String END_NODE_ID = "1";

    /**
     *
     */
    private static final long serialVersionUID = -2222;

    /**
     * 状态名
     */
    private String name;
    /**
     * 状态编码
     */
    private String code;
    /**
     * 排序号
     */
    private int orderNo;

    /**
     * 定义节点ID
     */
    private String actId;

    /**
     * 审核类型,和审核模块挂接
     */
    private String auditType;

    /**
     * 流程实例ID（返回值时使用）
     */
    private String processInstanceId;

    private boolean isIgnore;

    /**
     * 直接指定的人
     */
    protected String assignee;
    /**
     * 任务候选人
     */
    protected List<String> candidateUsers = new ArrayList<>();
    /**
     * 任务候选角色
     */
    protected List<String> candidateGroups = new ArrayList<>();


    public String getActId() {
        return actId;
    }

    public void setActId(String actId) {
        this.actId = actId;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }


    public WfNode() {
        super();
    }


    public WfNode(String name, String actId, int orderNo) {
        super();
        this.name = name;
        this.actId = actId;
        this.orderNo = orderNo;
    }

    /**
     * 生成虚拟的开始节点
     *
     * @return
     */
    public static WfNode newStartStatus() {
        WfNode status = new WfNode();
        status.setName("开始");
        status.setCode(START_NODE_ID);
        status.setActId(START_NODE_ID);
        return status;
    }

    /**
     * 生成虚拟的结束节点
     *
     * @return
     */
    public static WfNode newEndStatus() {
        WfNode status = new WfNode();
        status.setName("完成");
        status.setCode(END_NODE_ID);
        status.setActId(END_NODE_ID);
        return status;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public int getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(int orderNo) {
        this.orderNo = orderNo;
    }

    public boolean isIgnore() {
        return isIgnore;
    }

    public void setIgnore(boolean ignore) {
        isIgnore = ignore;
    }

    public boolean isEmpty() {
        return code == null && name == null;
    }

    @Override
    public String toString() {
        return "[" + code + "]" + name + "--->" + orderNo + ":" + actId;

    }

    public String getAuditType() {
        return auditType;
    }

    public void setAuditType(String auditType) {
        this.auditType = auditType;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public List<String> getCandidateUsers() {
        return candidateUsers;
    }

    public void setCandidateUsers(List<String> candidateUsers) {
        this.candidateUsers = candidateUsers;
    }

    public List<String> getCandidateGroups() {
        return candidateGroups;
    }

    public void setCandidateGroups(List<String> candidateGroups) {
        this.candidateGroups = candidateGroups;
    }

    public WfNode genCopy() {
        WfNode node = new WfNode();
        node.setName(this.getName());
        node.setCode(this.getCode());
        node.setOrderNo(this.getOrderNo());
        node.setActId(this.getActId());
        node.setAuditType(auditType);
        node.setProcessInstanceId(processInstanceId);
        node.isIgnore = this.isIgnore;
        node.assignee = assignee;
        if (this.candidateGroups != null) {
            node.candidateGroups = Collections.unmodifiableList(this.candidateGroups);
        }
        if (this.candidateUsers != null) {
            node.candidateUsers = Collections.unmodifiableList(this.candidateUsers);
        }
        return node;
    }
}
