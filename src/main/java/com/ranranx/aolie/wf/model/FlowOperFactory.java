package com.ranranx.aolie.wf.model;

import com.ranranx.aolie.wf.dto.WfTableAndFlow;

/**
 * 根据传入的流程信息创建流程操作服务
 *
 * @author xxl
 * @version V0.0.1
 * @date 2021/3/14 0014 15:48
 **/
public class FlowOperFactory {

    public static IFlowOperator createFlowOper(WfTableAndFlow flowInfo) {
        //目前只有一个服务,所以直接创建了
        GeneralFlowOper generalFlowOper = new GeneralFlowOper(flowInfo);
        return generalFlowOper;
    }

}
