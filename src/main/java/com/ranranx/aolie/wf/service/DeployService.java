package com.ranranx.aolie.wf.service;

import com.ranranx.aolie.core.handler.HandleResult;
import com.ranranx.aolie.wf.dto.WfWorkflowDto;
import org.flowable.ui.modeler.domain.Model;

/**
 * 部署服务
 *
 * @author xxl
 * @version V0.0.1
 * @date 2021/3/16 0016 20:41
 **/
public interface DeployService {

    /**
     * 布署流程,并检查
     *
     * @param flowId
     * @return
     */
    HandleResult validateModelAndDeploy(Long flowId);

    /**
     * 删除模板
     *
     * @param modelId
     */
    void deleteModel(String modelId);

    /**
     * 创建一个默认的模板
     *
     * @param key
     * @param name
     * @param description
     * @return
     */
    Model createModel(String key, String name, String description);


    /**
     * 删除流程
     *
     * @param dto
     */
    void deleteWf(WfWorkflowDto dto);


    /**
     * 更新模板的信息
     *
     * @param dto
     * @return
     */
    HandleResult updateModel(WfWorkflowDto dto);

    /**
     * 取得流程图片
     *
     * @param processInstanceId
     * @return
     */
    byte[] genFlowImage(String processInstanceId);

    /**
     * 生成业务流程图片
     *
     * @param bussId
     * @param tableId
     * @param version
     * @return
     */
    byte[] getFlowImage(Long bussId, Long tableId, String version);

}
