package com.ranranx.aolie.wf.dto;

import javax.persistence.Table;
import com.ranranx.aolie.core.common.BaseDto;
/**
 * @author xxl 
 * @date 2021-03-06 20:35:14
 * @version 1.0
 */
@Table(name = "aolie_wf_auditmodel")
public class WfAuditmodelDto extends BaseDto implements java.io.Serializable{

	private static final long serialVersionUID = 1L;
	private Long modelId;
	private String modelName;
	private String uiClass;
	public void setModelId(Long modelId){
		this.modelId = modelId;
	}
	public Long getModelId(){
		return this.modelId;
	}
	public void setModelName(String modelName){
		this.modelName = modelName;
	}
	public String getModelName(){
		return this.modelName;
	}
	public void setUiClass(String uiClass){
		this.uiClass = uiClass;
	}
	public String getUiClass(){
		return this.uiClass;
	}

}