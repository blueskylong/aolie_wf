package com.ranranx.aolie.wf.dto;

import com.ranranx.aolie.core.datameta.datamodel.TableInfo;

/**
 * 记录表对流程的全信息
 *
 * @author xxl
 * @version V0.0.1
 * @date 2021/3/9 0009 16:19
 **/
public class WfTableAndFlow {
    private WfTable2flowDto table2flowDto;
    private TableInfo tableInfo;
    private WfWorkflowDto workflowDto;

    public WfTable2flowDto getTable2flowDto() {
        return table2flowDto;
    }

    public void setTable2flowDto(WfTable2flowDto table2flowDto) {
        this.table2flowDto = table2flowDto;
    }

    public TableInfo getTableInfo() {
        return tableInfo;
    }

    public void setTableInfo(TableInfo tableInfo) {
        this.tableInfo = tableInfo;
    }

    public WfWorkflowDto getWorkflowDto() {
        return workflowDto;
    }

    public void setWorkflowDto(WfWorkflowDto workflowDto) {
        this.workflowDto = workflowDto;
    }
}
