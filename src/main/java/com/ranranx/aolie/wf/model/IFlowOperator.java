package com.ranranx.aolie.wf.model;

import com.ranranx.aolie.core.handler.param.condition.Criteria;
import com.ranranx.aolie.core.runtime.LoginUser;
import com.ranranx.aolie.wf.dto.WfWorkflowDto;

import java.util.List;
import java.util.Map;

/**
 * 用于执行流程相关操作时，不同的流程有不同的行为
 *
 * @author xxl
 * @version V0.0.1
 * @date 2021/3/8 0008 22:32
 **/
public interface IFlowOperator {


    /**
     * 开始一个流程
     *
     * @param businessId
     * @param userId
     * @return
     */
    WfNode startFlow(Long businessId, String userId);

    /**
     * 开始一个流程 增加流程流转条件
     *
     * @param businessId
     * @param userId
     * @param var
     * @return
     */
    WfNode startFlow(Long businessId, String userId, Map<String, Object> var);

    /**
     * 提交或者退回
     * 返回提交后的状态
     *
     * @param
     * @param user     用户登录信息
     * @param varibles 返回提交后的状态
     */
    WfNode commit(Long businessId, String procInstId, LoginUser user, Map<String, Object> varibles);


    /**
     * 取得一用户待办任务的查询条件，用于查询条件
     * 目前还没有想到好的办法，因为待办不会有太多，这里就先用引擎来查询，然后生成IN语句来作为过滤条件
     * 如果没有任务则返回1=2，如果有则返回in语句，逻辑词自己加
     *
     * @param
     * @param user
     * @param
     * @return
     */
    Criteria getCurrentUserTodoTaskFilter(LoginUser user);

    /**
     * 取得已提交的任务的过滤条件
     * 方法，从审核表里查询提交的信息，并且不是待办的
     *
     * @param user
     * @return
     */
    Criteria getCommittedTaskFilter(LoginUser user);


    /**
     * 取得所有有关任务的条件
     *
     * @param user
     * @return
     */
    Criteria getNodeTaskFilter(LoginUser user, List<String> lstNodes);


    /**
     * 删除项目的流程实例相关信息
     *
     * @param businessId
     * @param userId
     */
    boolean deleteFlowInstance(Long businessId, String userId, String reason);

    /**
     * 取得当前任务的状态
     */
    List<WfNode> getTaskStatus(Long businessId);

    /**
     * 当前任务是否有正在当前用户节点下
     *
     * @param businessId
     * @param user
     * @return
     */
    boolean isTaskOnPoint(Long businessId, LoginUser user);

    /**
     * 取得一流程所有有效节点的状态设置信息
     * 这里是一个约定，在定义流程节点时，需要给流程增加表单信息，会取表单的中四个属性，如果 没有则使用默认值
     * AUDIT_STATUS状态值，这个值一般会记录在业务表里，以方便在查询时使用。默认会用节点ID
     * STATUS_NAME状态名，当前状态的状态名，默认使用节点的名称
     * STATUS_ORDER 状态排序号 默认以读取顺序
     * IGNORE_NODE 是否忽略，如果为true,则这个节点，将不会取出来。
     *
     * @param
     * @return
     */
    List<WfNode> getWfNode();

//    /**
//     * 撤回终审
//     *
//     * @param processId
//     * @param userId
//     * @return
//     */
//    boolean revert(String processId, String userId);
//

    /**
     * 撤回上次提交
     *
     * @param processInstId
     * @param tableId
     * @return
     */
    boolean rollBack(String processInstId, long tableId);

    /**
     * 取得流程定义信息
     *
     * @return
     */
    WfWorkflowDto getWfInfo();

    void init();

}
