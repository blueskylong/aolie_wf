package com.ranranx.aolie.wf.dto;

import javax.persistence.Table;

import com.ranranx.aolie.core.common.BaseDto;

/**
 * @author xxl
 * @version 1.0
 * @date 2021-03-06 20:35:14
 */
@Table(name = "aolie_wf_audit_data")
public class WfAuditDataDto extends BaseDto implements java.io.Serializable {

    private static final long serialVersionUID = 1L;
    private Long auditId;
    private Long modelId;
    private Integer xh;
    private Short operType;
    private String fromActId;
    private String actId;
    private Long businessId;
    private String conclusion;
    private Long wfId;
    private Long tableId;
    private Long operUser;

    public Long getWfId() {
        return wfId;
    }

    public void setWfId(Long wfId) {
        this.wfId = wfId;
    }

    private String procInstId;

    public String getProcInstId() {
        return procInstId;
    }

    public void setProcInstId(String procInstId) {
        this.procInstId = procInstId;
    }

    public void setAuditId(Long auditId) {
        this.auditId = auditId;
    }

    public Long getAuditId() {
        return this.auditId;
    }

    public void setModelId(Long modelId) {
        this.modelId = modelId;
    }

    public Long getModelId() {
        return this.modelId;
    }

    public void setXh(Integer xh) {
        this.xh = xh;
    }

    public Integer getXh() {
        return this.xh;
    }

    public void setOperType(Short operType) {
        this.operType = operType;
    }

    public Short getOperType() {
        return this.operType;
    }

    public void setFromActId(String fromActId) {
        this.fromActId = fromActId;
    }

    public String getFromActId() {
        return this.fromActId;
    }

    public void setActId(String actId) {
        this.actId = actId;
    }

    public String getActId() {
        return this.actId;
    }


    public void setConclusion(String conclusion) {
        this.conclusion = conclusion;
    }

    public String getConclusion() {
        return this.conclusion;
    }

    public Long getBusinessId() {
        return businessId;
    }

    public void setBusinessId(Long businessId) {
        this.businessId = businessId;
    }

    public Long getTableId() {
        return tableId;
    }

    public void setTableId(Long tableId) {
        this.tableId = tableId;
    }

    public Long getOperUser() {
        return operUser;
    }

    public void setOperUser(Long operUser) {
        this.operUser = operUser;
    }
}