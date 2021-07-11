package com.ranranx.aolie.wf.model;

import com.ranranx.aolie.wf.dto.WfWorkflowDto;

import java.io.Serializable;
import java.util.List;

/**
 * 流程综合信息
 *
 * @author xxl
 * @version V0.0.1
 * @date 2021/3/25 0025 20:43
 **/
public class FlowInfo implements Serializable {


    private WfWorkflowDto flowDto;
    private List<WfNode> lstNodes;

    public FlowInfo(WfWorkflowDto flowDto, List<WfNode> lstNodes) {
        this.flowDto = flowDto;
        this.lstNodes = lstNodes;
    }

    public WfWorkflowDto getFlowDto() {
        return flowDto;
    }

    public void setFlowDto(WfWorkflowDto flowDto) {
        this.flowDto = flowDto;
    }

    public List<WfNode> getLstNodes() {
        return lstNodes;
    }

    public void setLstNodes(List<WfNode> lstNodes) {
        this.lstNodes = lstNodes;
    }
}
