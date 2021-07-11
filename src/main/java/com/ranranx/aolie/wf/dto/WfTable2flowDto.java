package com.ranranx.aolie.wf.dto;

import javax.persistence.Table;
import com.ranranx.aolie.core.common.BaseDto;
/**
 * @author xxl 
 * @date 2021-03-06 20:35:14
 * @version 1.0
 */
@Table(name = "aolie_wf_table2flow")
public class WfTable2flowDto extends BaseDto implements java.io.Serializable{

	private static final long serialVersionUID = 1L;
	private Long id;
	private Long tableId;
	private Long wfId;
	public void setId(Long id){
		this.id = id;
	}
	public Long getId(){
		return this.id;
	}
	public void setTableId(Long tableId){
		this.tableId = tableId;
	}
	public Long getTableId(){
		return this.tableId;
	}
	public void setWfId(Long wfId){
		this.wfId = wfId;
	}
	public Long getWfId(){
		return this.wfId;
	}

}