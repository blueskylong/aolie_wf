package com.ranranx.aolie.wf.dto;

import javax.persistence.Table;
import com.ranranx.aolie.core.common.BaseDto;
/**
 * @author xxl 
 * @date 2021-03-06 20:35:14
 * @version 1.0
 */
@Table(name = "aolie_wf_node_auditmodel")
public class WfNodeAuditmodelDto extends BaseDto implements java.io.Serializable{

	private static final long serialVersionUID = 1L;
	private Long wfId;
	private Long nodeAuditId;
	private Long modelId;
	private String params;
	private String actNodeId;
	public void setWfId(Long wfId){
		this.wfId = wfId;
	}
	public Long getWfId(){
		return this.wfId;
	}
	public void setNodeAuditId(Long nodeAuditId){
		this.nodeAuditId = nodeAuditId;
	}
	public Long getNodeAuditId(){
		return this.nodeAuditId;
	}
	public void setModelId(Long modelId){
		this.modelId = modelId;
	}
	public Long getModelId(){
		return this.modelId;
	}
	public void setParams(String params){
		this.params = params;
	}
	public String getParams(){
		return this.params;
	}
	public void setActNodeId(String actNodeId){
		this.actNodeId = actNodeId;
	}
	public String getActNodeId(){
		return this.actNodeId;
	}

}