package com.ranranx.aolie.wf.dto;

import javax.persistence.Table;
import javax.xml.crypto.Data;

import com.ranranx.aolie.core.common.BaseDto;

import java.util.Date;

/**
 * @author xxl
 * @version 1.0
 * @date 2021-03-06 20:35:14
 */
@Table(name = "aolie_wf_workflow")
public class WfWorkflowDto extends BaseDto implements java.io.Serializable {

    private static final long serialVersionUID = 1L;
    private Long wfId;
    private String wfName;
    /**
     * 流程KEY,可能是在业务KEY后加一个版本
     */
    private String wfKey;
    /**
     * 业务KEY ,
     */
    private String bussKey;
    private Integer isStrict;
    private String modelId;
    private String deployTime;
    private Long deployUser;
    private String memo;

    public void setWfId(Long wfId) {
        this.wfId = wfId;
    }

    public Long getWfId() {
        return this.wfId;
    }

    public void setWfName(String wfName) {
        this.wfName = wfName;
    }

    public String getWfName() {
        return this.wfName;
    }

    public void setWfKey(String wfKey) {
        this.wfKey = wfKey;
    }

    public String getWfKey() {
        return this.wfKey;
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public Integer getIsStrict() {
        return isStrict;
    }

    public void setIsStrict(Integer isStrict) {
        this.isStrict = isStrict;
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public String getDeployTime() {
        return deployTime;
    }

    public void setDeployTime(String deployTime) {
        this.deployTime = deployTime;
    }

    public Long getDeployUser() {
        return deployUser;
    }

    public void setDeployUser(Long deployUser) {
        this.deployUser = deployUser;
    }

    public String getBussKey() {
        return bussKey;
    }

    public void setBussKey(String bussKey) {
        this.bussKey = bussKey;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }
}