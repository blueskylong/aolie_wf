package com.ranranx.aolie.wf.model;

import com.ranranx.aolie.core.common.CommonUtils;
import com.ranranx.aolie.core.common.Constants;
import com.ranranx.aolie.core.common.SessionUtils;
import com.ranranx.aolie.core.datameta.datamodel.SchemaHolder;
import com.ranranx.aolie.core.datameta.datamodel.TableColumnRelation;
import com.ranranx.aolie.core.datameta.datamodel.TableInfo;
import com.ranranx.aolie.core.datameta.dto.TableColumnRelationDto;
import com.ranranx.aolie.core.ds.definition.FieldOrder;
import com.ranranx.aolie.core.exceptions.IllegalOperatorException;
import com.ranranx.aolie.core.handler.HandleResult;
import com.ranranx.aolie.core.handler.HandlerFactory;
import com.ranranx.aolie.core.handler.param.DeleteParam;
import com.ranranx.aolie.core.handler.param.InsertParam;
import com.ranranx.aolie.core.handler.param.QueryParam;
import com.ranranx.aolie.core.handler.param.condition.Criteria;
import com.ranranx.aolie.core.runtime.LoginUser;
import com.ranranx.aolie.wf.WfConstants;
import com.ranranx.aolie.wf.dto.WfAuditDataDto;
import com.ranranx.aolie.wf.dto.WfAuditDataHisDto;
import com.ranranx.aolie.wf.dto.WfTableAndFlow;
import com.ranranx.aolie.wf.dto.WfWorkflowDto;
import com.ranranx.aolie.wf.exceptions.IllegalFlowOperationException;
import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FormProperty;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.form.TaskFormData;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ChangeActivityStateBuilder;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

/**
 * 部门流程的执行器抽像类，每一个流程，可以有自己不同的实现，可以自定义自己的执行器
 *
 * @author XXL
 */

public class BaseFlowOperator implements IFlowOperator {
    protected Logger logger = LoggerFactory.getLogger(IFlowOperator.class);


    /**
     * 缓存流程的状态信息 flowCode:lstStatus
     */
    protected final List<WfNode> lstNodes = new ArrayList<>();

    protected String processDefinitionId;
    protected Long wfId;

    @Autowired
    private HandlerFactory factory;
    @Autowired
    protected ProcessEngine processEngine;
    @Autowired
    protected RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    protected WfTableAndFlow wfTableAndFlow;

    private TableColumnRelation auditToBizRelation = null;

    public BaseFlowOperator(WfTableAndFlow wfTableAndFlow) {
        this.wfTableAndFlow = wfTableAndFlow;
        this.wfId = wfTableAndFlow.getWorkflowDto().getWfId();
        this.processDefinitionId = wfTableAndFlow.getWorkflowDto().getWfKey();
    }

    @Override
    public void init() {
        this.initWfNode();
        this.initAuditTableToBisTableRelation();
    }

    @Override
    public WfNode startFlow(Long businessId, String userId) {
        return startFlow(businessId, userId, null);
    }

    /**
     * 取得流程的定义 ID
     *
     * @return
     */
    private String getFlowDefinitionId() {
        return this.processDefinitionId;
    }

    @Override
    public WfNode startFlow(Long businessId, String userId, Map<String, Object> var) {
        if (businessId == null) {
            return null;
        }
        removeNullValue(var);
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(getFlowDefinitionId(), businessId.toString(), var);
        List<WfNode> status = getTaskStatus(businessId);
        this.updateTableStatus(businessId, instance.getProcessInstanceId(), getActivityDefineKey(status),
                null, WfConstants.WfOperType.TYPE_CREATE);
        return status.get(0);
    }

    /**
     * 更新表字段状态
     *
     * @param businessId     业务ID
     * @param fromNodeId     　流程定义ID
     * @param processId      流程实例ID
     * @param currentNodesId 当前节点ID
     */
    protected void updateTableStatus(Long businessId, String processId,
                                     String currentNodesId, String fromNodeId, Short operType) {
        //先将审核数据添加到历史表中
        //如果有from
        if (CommonUtils.isNotEmpty(fromNodeId)) {
            //复制历史 ,更新当前
            copyAuditToHisAndUpdate(businessId, currentNodesId, fromNodeId, operType);
        } else {
            //插入审核信息
            createAuditInfo(businessId, processId, currentNodesId);

        }
    }

    private void createAuditInfo(Long businessId, String processInstId, String currentNodesId) {
        WfAuditDataDto dto = new WfAuditDataDto();
        dto.setAuditId(-1L);
        dto.setXh(1);
        dto.setActId(currentNodesId);
        dto.setOperUser(SessionUtils.getLoginUser().getUserId());
        dto.setBusinessId(businessId);
        dto.setOperType(WfConstants.WfOperType.TYPE_CREATE);
        dto.setProcInstId(processInstId);
        dto.setTableId(wfTableAndFlow.getTableInfo().getTableDto().getTableId());
        dto.setWfId(this.wfTableAndFlow.getWorkflowDto().getWfId());
        InsertParam param = new InsertParam();
        param.setObjects(Arrays.asList(dto), WfConstants.DEFAULT_WF_SCHEMA);
        factory.handleInsert(param);
    }

    private void copyAuditToHisAndUpdate(Long businessId, String currentActId, String fromActId, Short operType) {
        if (CommonUtils.isEmpty(businessId) || CommonUtils.isEmpty(currentActId)) {
            logger.error("更新审核信息失败,关键信息没有提供 ");
            return;
        }
        QueryParam queryParam = new QueryParam();
        WfAuditDataDto auditDataDto = new WfAuditDataDto();
        auditDataDto.setBusinessId(businessId);
        auditDataDto.setTableId(this.wfTableAndFlow.getTableInfo().getTableDto().getTableId());
        queryParam.setFilterObjectAndTableAndResultType(WfConstants.DEFAULT_WF_SCHEMA, SessionUtils.getLoginVersion(), auditDataDto);
        HandleResult result = factory.handleQuery(queryParam);
        if (result.isSuccess() && result.getData() != null) {
            List<WfAuditDataDto> lstDto = (List<WfAuditDataDto>) result.getData();
            if (!lstDto.isEmpty()) {
                InsertParam param = new InsertParam();
                param.setTableDto(WfConstants.DEFAULT_WF_SCHEMA, WfAuditDataHisDto.class);
                param.setLstRows(toHis(lstDto));
                factory.handleInsert(param);
            }

            WfAuditDataDto dto = lstDto.get(0);
            //删除插入,算是新的审核信息,这里只可能有一条数据
            DeleteParam deleteParam = new DeleteParam();
            deleteParam.setIds(Arrays.asList(dto.getAuditId()));
            deleteParam.setTableDto(WfConstants.DEFAULT_WF_SCHEMA, WfAuditDataDto.class);
            factory.handleDelete(deleteParam);
            //插入
            dto.setAuditId(-1L);//由拦截器去生成
            dto.setFromActId(fromActId);
            dto.setXh(dto.getXh() + 1);
            dto.setOperType(operType);
            dto.setOperUser(SessionUtils.getLoginUser().getUserId());
            dto.setActId(currentActId);
            InsertParam param = new InsertParam();
            param.setObjects(lstDto, WfConstants.DEFAULT_WF_SCHEMA);
            factory.handleInsert(param);
        } else {
            logger.error("查询审核信息出错,没有查询到信息");
        }
    }

    private List<Map<String, Object>> toHis(List<WfAuditDataDto> lstDto) {
        List<Map<String, Object>> lstResult = new ArrayList<>(lstDto.size());
        lstDto.forEach(wfAuditDataDto -> {
            Map<String, Object> map = CommonUtils.toMap(wfAuditDataDto, true);
            map.put("audit_id_his", -1);
            lstResult.add(map);
        });
        return lstResult;
    }

    @Override
    public WfNode commit(Long businessId, String procInstId, LoginUser user, Map<String, Object> varibles) {
        String userId = user.getUserId().toString();
        String roleId = user.getRoleId().toString();

        Task task = getUserOneTask(procInstId, userId, roleId);
        // 先检查当前的用户是不是可以操作这个任务
        if (task == null) {
            throw new RuntimeException("指定的任务不存在,或不在权限内");
        }
        // 指定任务执行人
        processEngine.getTaskService().setAssignee(task.getId(), userId);
        // 提交任务
        removeNullValue(varibles);
        completeTask(userId, task.getId(), varibles);
        List<WfNode> result = getTaskStatus(businessId);
        updateTableStatus(businessId, null, getActivityDefineKey(result),
                task.getTaskDefinitionKey(), WfConstants.WfOperType.TYPE_COMMIT);

        return result.get(0);
    }


    private void completeTask(String userId, String taskId, Map<String, Object> vars) {
        taskService.setAssignee(taskId, userId);
        taskService.complete(taskId, vars);
    }

    /**
     * 取得当前用户指定流程实例ID,
     *
     * @param procInstId
     * @param userId
     * @return
     */
    private Task getUserOneTask(String procInstId, String userId, String roleId) {
        return processEngine.getTaskService()
                .createTaskQuery()
                .processInstanceId(procInstId)
                .or()
                .taskCandidateOrAssigned(userId)
                .taskCandidateGroup(roleId)
                .endOr()
                .singleResult();

    }

    /**
     * 取得当前用户指定流程实例ID,
     *
     * @param businessKey
     * @param user
     * @return
     */
    private Task getUserOneTaskByBusinessKey(String businessKey, LoginUser user) {
        return processEngine.getTaskService()
                .createTaskQuery()
                .processInstanceBusinessKey(businessKey)
                .or()
                .taskCandidateOrAssigned(user.getUserId().toString())
                .taskCandidateGroup(user.getRoleId().toString())
                .endOr()
                .singleResult();

    }

    /**
     * 取得所有有关任务的条件
     *
     * @param tableAlias
     * @param userId
     * @return
     */
    public String getAllTaskFilter(String tableAlias, String userId) {
        return null;
    }

    /**
     * Gen in-clause.
     *
     * @param lstData     the lst data
     * @param fieldName   the field name
     * @param isInOrNotIn the value is true if it is in or not in
     * @param isNum       the value is true if it is num
     * @return the string
     */
    public static String genInClause(List lstData, String fieldName, boolean isInOrNotIn, boolean isNum) {
        if (lstData == null || lstData.isEmpty()) {
            return "";
        }
        int iCount = lstData.size();
        String inExp = " not in ";
        if (isInOrNotIn) {
            inExp = " in ";
        }
        int pageCount = (iCount - 1) / 400 + 1;
        StringBuffer sqlAll = new StringBuffer("(");
        int ser = 0;
        for (int i = 0; i < pageCount; i++) {
            sqlAll.append(fieldName).append(inExp).append(" (");
            for (int j = 0; j < 400; j++) {
                ser = i * 400 + j;
                if (ser < iCount) {
                    if (isNum) {
                        sqlAll.append(lstData.get(ser)).append(",");
                    } else {
                        sqlAll.append("'").append(lstData.get(ser)).append("',");
                    }
                }
            }
            sqlAll.delete(sqlAll.length() - 1, sqlAll.length()).append(")").append(isInOrNotIn ? " or  " : " and ");
        }
        return sqlAll.substring(0, sqlAll.length() - 4) + ")";
    }

    @Override
    public boolean deleteFlowInstance(Long businessId, String userId, String reason) {
        if (businessId == null || "".equals(businessId)) {
            return false;
        }
        List<Task> lstTask = findTaskByBiz(businessId.toString());
        if (lstTask == null || lstTask.isEmpty()) {
            return false;
        }
        runtimeService.deleteProcessInstance(lstTask.get(0).getProcessInstanceId(), null);
        //删除审核信息
        return true;

    }

    /**
     * 最得一流程的最后版本
     *
     * @param flowCode
     * @return
     */
    private String getLastFlowDefinitionId(String flowCode) {
        List<ProcessDefinition> lstData = processEngine.getRepositoryService().createProcessDefinitionQuery().processDefinitionKey(flowCode)
                .orderByProcessDefinitionVersion().desc()// 使用流程定义的版本升序排列
                .list();
        if (lstData == null || lstData.isEmpty()) {
            return null;
        }
        return lstData.get(0).getId();
    }

    @Override
    public List<WfNode> getTaskStatus(Long businessId) {
        List<WfNode> result = new ArrayList<WfNode>();
        List<Task> lstTask = findTaskByBiz(businessId.toString());
        if (lstTask == null || lstTask.isEmpty()) {
            result.add(WfNode.newEndStatus());
            return result;
        }
        TaskFormData taskFormData;
        WfNode status;
        Set set = new HashSet();
        for (Task Task : lstTask) {
            taskFormData = processEngine.getFormService().getTaskFormData(Task.getId());
            status = getStatusFromFormData(taskFormData);
            if (status == null || StringUtils.isEmpty(status.getCode())) {
                // 如果没有定义表单参数，则要定义的状态
                status = getStatusFromDefinity(Task);
                if (set.add(status)) {
                    result.add(status);
                }
            } else {
                if (set.add(status)) {
                    result.add(status);
                }
            }
        }
        return result;
    }


    private String getUserTaskStatus(Long businessId, LoginUser user) {
        Task task = getUserOneTaskByBusinessKey(businessId.toString(), user);
        //如果在TODO中没有找到,则认为是完成状态
        if (task == null) {
            return "1";
        }
        WfNode status = getStatusByTask(task);
        if (status == null) {
            return null;
        }
        return status.getCode();
    }

    private WfNode getStatusByTask(Task task) {
        TaskFormData taskFormData = processEngine.getFormService().getTaskFormData(task.getId());
        WfNode status = getStatusFromFormData(taskFormData);
        if (status == null || StringUtils.isEmpty(status.getCode())) {
            // 如果没有定义表单参数，则要定义的状态
            status = getStatusFromDefinity(task);
        }
        return status;
    }

    private WfNode getStatusFromDefinity(Task task) {
        List<WfNode> lstStatus = getWfNode();
        String definitionKey = task.getTaskDefinitionKey();
        for (WfNode wfNode : lstStatus) {
            if (wfNode.getActId().equals(definitionKey)) {
                wfNode = wfNode.genCopy();
                wfNode.setProcessInstanceId(task.getProcessInstanceId());
                return wfNode;
            }
        }
        return null;
    }

    /**
     * 从表单生成状态信息
     *
     * @param taskFormData
     * @return
     */
    private WfNode getStatusFromFormData(TaskFormData taskFormData) {
        if (taskFormData == null) {
            return null;
        }
        Map<String, Object> param = createParam(taskFormData);
        String ignore = getAStringField(param, WfConstants.WfNodeConstAttr.ignore);
        String auditType = getAStringField(param, WfConstants.WfNodeConstAttr.auditType);
        WfNode node = new WfNode();
        node.setAuditType(auditType);
        node.setIgnore(ignore != null && "1".equals(ignore));
        node.setActId(taskFormData.getTask().getTaskDefinitionId());
        node.setProcessInstanceId(taskFormData.getTask().getProcessInstanceId());
        return node;
    }


    /**
     * 取得一Map中的一字段的字符值 XXL
     *
     * @param aData
     * @param sField
     * @return
     */
    public static String getAStringField(Map<? extends Object, Object> aData, String sField) {
        if (aData == null) {
            return null;
        }

        if (aData.get(sField) == null) {
            return null;
        }
        return aData.get(sField).toString();
    }

    protected List<WfNode> getStatusByProcessId(String processId) {
        ProcessInstance instance = getProcessById(processId);
        if (instance == null) {
            return null;
        }
        return getTaskStatus(Long.parseLong(instance.getBusinessKey()));
    }

    /**
     * 初始化并缓存节点
     *
     * @param
     * @return
     */
    private List<WfNode> initWfNode() {
        String id = getLastFlowDefinitionId(processDefinitionId);
        if (id == null) {
            return null;
        }
        BpmnModel model = processEngine.getRepositoryService().getBpmnModel(id);
        if (model == null) {
            return null;
        }
        Collection<FlowElement> flowElements = model.getMainProcess().getFlowElements();
        UserTask userTask;
        List<WfNode> lstState = new ArrayList<WfNode>();
        // 没有给顺序的，则按此开始排序
        int index = -1000;
        // 如果 没有定义表单参数，则会直接使用结点的ID 和 名称做为节点为信息
        String properName;
        boolean isIgnore = false;
        // 这里只处理人工节点
        for (FlowElement e : flowElements) {
            if (e instanceof UserTask) {
                userTask = (UserTask) e;
                WfNode status = new WfNode(userTask.getName(), userTask.getId(), index++);
                // 取得表单定义项
                List<FormProperty> lstProperty = userTask.getFormProperties();
                isIgnore = false;
                for (FormProperty formProperty : lstProperty) {
                    properName = formProperty.getName();
                    if (isNotEmpty(formProperty.getExpression())) {
                        if (WfConstants.WfNodeConstAttr.code.equals(properName)) {
                            status.setCode(formProperty.getExpression());
                        } else if (WfConstants.WfNodeConstAttr.name.equals(properName)) {
                            status.setName(formProperty.getExpression());
                        } else if (WfConstants.WfNodeConstAttr.order.equals(properName)) {
                            try {
                                status.setOrderNo(Integer.parseInt(formProperty.getExpression()));
                            } catch (NumberFormatException e1) {
                            }
                        } else if (WfConstants.WfNodeConstAttr.ignore.equals(properName)
                                && formProperty.getExpression() != null
                                && (formProperty.getExpression().equals("1") ||
                                formProperty.getExpression().equals("true"))) {
                            isIgnore = true;
                            break;
                        }

                    }
                }
                status.setActId(userTask.getId());
                status.setAssignee(userTask.getAssignee());
                status.setCandidateGroups(userTask.getCandidateGroups());
                status.setCandidateUsers(userTask.getCandidateUsers());
                if (!isIgnore) {
                    lstState.add(status);
                }
            }
        }
        lstState = sortFlowState(lstState);
        // 增加最后一个审核 结束节点 ，因为这是固定的，在流程图上也没有体现，所以这里直接加上
        WfNode lastStatus = new WfNode("审核结束", "1", 1000);
        lastStatus.setActId("1");
        lstState.add(lastStatus);
        lstNodes.addAll(lstState);
        return lstState;

    }

    /**
     * 取得表单数据
     *
     * @param taskFormData
     * @return
     */
    private Map<String, Object> createParam(TaskFormData taskFormData) {
        Map<String, Object> mapValue = new HashMap<String, Object>();
        Iterator<org.flowable.engine.form.FormProperty> it = taskFormData.getFormProperties().iterator();
        org.flowable.engine.form.FormProperty prop;
        while (it.hasNext()) {
            prop = it.next();
            mapValue.put(prop.getName(), prop.getValue());
        }
        return mapValue;
    }

    /**
     * 取得一流程所有有效节点的状态设置信息 这里是一个约定，在定义流程节点时，需要给流程增加表单信息，会取表单的中四个属性，如果 没有则使用默认值
     * AUDIT_STATUS状态值，这个值一般会记录在业务表里，以方便在查询时使用。默认会用节点ID
     * STATUS_NAME状态名，当前状态的状态名，默认使用节点的名称 STATUS_ORDER 状态排序号 默认以读取顺序 IGNORE_NODE
     * 是否忽略，如果为true,则这个节点，将不会取出来。
     *
     * @param
     * @return
     */
    @Override
    public List<WfNode> getWfNode() {
        return this.lstNodes;
    }

    /**
     * 给节点排序
     *
     * @param lstStates
     * @return
     */
    private static List<WfNode> sortFlowState(List<WfNode> lstStates) {
        if (lstStates.isEmpty()) {
            return lstStates;
        }
        Collections.sort(lstStates, new Comparator<WfNode>() {

            @Override
            public int compare(WfNode o1, WfNode o2) {
                return o1.getOrderNo() - o2.getOrderNo();
            }
        });
        return lstStates;
    }

    /**
     * 当前任务是否有正在当前用户节点下
     *
     * @param businessId
     * @param user
     * @return
     */
    @Override
    public boolean isTaskOnPoint(Long businessId, LoginUser user) {
        Task task = getUserOneTaskByBusinessKey(businessId.toString(), user);
        return task != null;
    }

    public static boolean isNotEmpty(Object obj) {
        return obj != null && !obj.equals("");
    }

    protected ProcessInstance getProcessById(String processId) {
        List<ProcessInstance> lstProcess = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processId).list();
        if (lstProcess == null || lstProcess.isEmpty()) {
            return null;
        }
        return lstProcess.get(0);
    }

    protected WfTableAndFlow getFlowInfo() {
        return this.wfTableAndFlow;
    }

    /**
     * 是否权限严格模式
     *
     * @return
     */
    public boolean isRightStrict() {
        return wfTableAndFlow.getWorkflowDto().getIsStrict() != null
                && wfTableAndFlow.getWorkflowDto().getIsStrict() == 1;
    }

    /**
     * 根据系统设置，取得流程过滤条件
     *
     * @param tableAlias
     * @param userId
     * @return
     */
    public String getFlowFilterAuto(String tableAlias, String userId) {
        if (isRightStrict()) {
            return getAllTaskFilter(tableAlias, userId);
        } else {
            return "1=1";//如果不是严格意义，则不做流程过滤
        }
    }

    public static void removeNullKey(Map map) {
        Set set = map.keySet();
        for (Iterator iterator = set.iterator(); iterator.hasNext(); ) {
            Object obj = (Object) iterator.next();
            remove(obj, iterator);
        }
    }

    private String getActivityDefineKey(List<WfNode> status) {
        int len = status.size();
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < len; i++) {
            str.append(status.get(i).getActId());
            if (i < len - 1) {
                str.append(",");
            }
        }
        return str.toString();
    }

    /**
     * 移除map中的value空值
     *
     * @param map
     * @return
     */
    public static void removeNullValue(Map map) {
        if (map != null) {
            Set set = map.keySet();
            for (Iterator iterator = set.iterator(); iterator.hasNext(); ) {
                Object obj = (Object) iterator.next();
                Object value = (Object) map.get(obj);
                remove(value, iterator);
            }

        }
    }

    /**
     * 移除map中的空值
     * <p>
     * Iterator 是工作在一个独立的线程中，并且拥有一个 mutex 锁。
     * Iterator 被创建之后会建立一个指向原来对象的单链索引表，当原来的对象数量发生变化时，这个索引表的内容不会同步改变，
     * 所以当索引指针往后移动的时候就找不到要迭代的对象，所以按照 fail-fast 原则 Iterator 会马上抛出 java.util.ConcurrentModificationException 异常。
     * 所以 Iterator 在工作的时候是不允许被迭代的对象被改变的。
     * 但你可以使用 Iterator 本身的方法 remove() 来删除对象， Iterator.remove() 方法会在删除当前迭代对象的同时维护索引的一致性。
     *
     * @param obj
     * @param iterator
     */
    private static void remove(Object obj, Iterator iterator) {
        if (obj instanceof String) {
            String str = (String) obj;
            if (StringUtils.isEmpty(str)) {
                iterator.remove();
            }

        } else if (obj instanceof Collection) {
            Collection col = (Collection) obj;
            if (col == null || col.isEmpty()) {
                iterator.remove();
            }

        } else if (obj instanceof Map) {
            Map temp = (Map) obj;
            if (temp == null || temp.isEmpty()) {
                iterator.remove();
            }

        } else if (obj instanceof Object[]) {
            Object[] array = (Object[]) obj;
            if (array == null || array.length <= 0) {
                iterator.remove();
            }
        } else {
            if (obj == null) {
                iterator.remove();
            }
        }
    }

    /**
     * 撤回上次提交
     *
     * @param processInstId
     * @param tableId
     * @return
     */
    @Override
    public boolean rollBack(String processInstId, long tableId) {
        //先检查审核表中,最后的操作人,是不是当前的人,如果是,则可以退回
        WfAuditDataDto auditData = findLastCommitData(processInstId, tableId);
        Long curUserId = SessionUtils.getLoginUser().getUserId();
        String version = SessionUtils.getLoginVersion();
        String fromActId;
        String toActId;
        if (auditData == null) {
            throw new IllegalFlowOperationException("当前流程不存在");
        }
        //0:当前节点是开始节点,则不可以退回
        else if (auditData.getOperType().equals(WfConstants.WfOperType.TYPE_CREATE) || CommonUtils.isEmpty(auditData.getFromActId())) {
            throw new IllegalFlowOperationException("当前流程新创建,不可以撤回");
        }
        //1.如果当前数据是提交,但不是此人操作,则错误
        else if (auditData.getOperType().equals(WfConstants.WfOperType.TYPE_COMMIT)
                && !auditData.getOperUser().equals(curUserId)) {
            throw new IllegalFlowOperationException("最后提交人不是本人,不可以操作");
        }
        //2.如果当前数据不是提交,则查询历史库,根据当前状态和提交类型,判断最后提交操作是不是此人操作,如果不是,则无权操作
        else if (!auditData.getOperType().equals(WfConstants.WfOperType.TYPE_COMMIT)) {
            //查询历史最后的一次提交
            WfAuditDataHisDto auditHis = findLastCommitAuditHis(processInstId, auditData.getActId(), tableId, version);
            if (auditHis == null) {
                throw new IllegalFlowOperationException("未查询到提交的历史信息");
            }
            if (!auditHis.getOperUser().equals(curUserId)) {
                throw new IllegalFlowOperationException("不是由本人提交的节点,不可撤回");
            }
            //执行退回
            fromActId = auditData.getActId();
            toActId = auditHis.getFromActId();
        } else {
            fromActId = auditData.getActId();
            toActId = auditData.getFromActId();
        }
        runtimeService.createChangeActivityStateBuilder().processInstanceId(processInstId)
                .moveActivityIdTo(fromActId, toActId).changeState();

        //保存审核轨迹
        copyAuditToHisAndUpdate(auditData.getBusinessId(), toActId, fromActId, WfConstants.WfOperType.ROLL_BACK);
        return true;
    }

    /**
     * 查询终点是指定节点的提交,并且是最后提交的
     *
     * @param processInstId
     * @param actTo
     * @param tableId
     * @return
     */
    private WfAuditDataHisDto findLastCommitAuditHis(String processInstId, String actTo, Long tableId, String version) {
        WfAuditDataHisDto dto = new WfAuditDataHisDto();
        dto.setActId(actTo);
        dto.setProcInstId(processInstId);
        dto.setOperType(WfConstants.WfOperType.TYPE_COMMIT);
        dto.setTableId(tableId);
        QueryParam param = new QueryParam();
        param.setFilterObjectAndTableAndResultType(WfConstants.DEFAULT_WF_SCHEMA, version, dto);
        param.addOrder(new FieldOrder((String) null, "xh", false, 1));
        HandleResult result = factory.handleQuery(param);
        return (WfAuditDataHisDto) result.singleValue();
    }

    /**
     * 查询当前的状态
     *
     * @param processInstId
     * @param tableId
     * @return
     */
    private WfAuditDataDto findLastCommitData(String processInstId, long tableId) {
        WfAuditDataDto dto = new WfAuditDataDto();
        dto.setProcInstId(processInstId);
        dto.setTableId(tableId);
        QueryParam param = new QueryParam();
        param.setFilterObjectAndTableAndResultType(WfConstants.DEFAULT_WF_SCHEMA, SessionUtils.getLoginVersion(), dto);
        HandleResult result = factory.handleQuery(param);
        return (WfAuditDataDto) result.singleValue();
    }

    /**
     * 取得该用户,可以操作的节点
     * 注意,此功能只可以用在不动态指定候选人和执行人的情况下
     *
     * @param user
     * @return
     */
    private List<WfNode> findUserCanOperNodes(LoginUser user) {
        List<WfNode> lstResult = new ArrayList<>();
        String userId = user.getUserId().toString();
        String roleId = user.getRoleId().toString();
        this.lstNodes.forEach(el -> {
            if (user.getUserId().toString().equals(el.getAssignee())
                    || el.getCandidateUsers().contains(userId)
                    || el.getCandidateGroups().contains(roleId)) {
                lstResult.add(el);
            }
        });
        return lstResult;
    }

    private List<Task> findTaskByBiz(String businessKey) {
        return taskService.createTaskQuery()
                .processInstanceBusinessKey(businessKey)
                .processDefinitionKey(this.processDefinitionId).list();
    }

    /**
     * 因为流程在确定后,已指定了节点的待办条件,所以这里只需要增加一个条件,就是当前审核表里存在这个信息,且是指定的几个节点
     *
     * @param user
     * @return
     */
    @Override
    public Criteria getCurrentUserTodoTaskFilter(LoginUser user) {

        Criteria criteria = new Criteria();
        //如果没有权限节点,则提交无效条件
        List<String> lstNodes = userCanOperNodeIds(user);
        if (lstNodes == null || lstNodes.isEmpty()) {
            return Criteria.getFalseExpression();
        }
        //增加可编辑节点,并且在审核主表里
        QueryParam queryParam = new QueryParam();
        String auditTable = CommonUtils.getTableName(WfAuditDataDto.class);
        queryParam.setTableDtos(WfConstants.DEFAULT_WF_SCHEMA, SessionUtils.getLoginVersion(), WfAuditDataDto.class);
        //增加节点条件
        //TODO 这里可以修改成支持多分支的情况
        queryParam.appendCriteria().andIn(auditTable, "act_id", lstNodes)
                //增加表条件
                .andEqualTo(auditTable, "table_id", this.wfTableAndFlow.getTableInfo().getTableDto().getTableId());
        //增加与主表的关联
        queryParam.setLstRelation(Arrays.asList(auditToBizRelation));
        criteria.andExists(queryParam);
        return criteria;
    }

    /**
     * 初始化业务表和审核的关联关系
     * 业务表的主键与审核表中的业务ID字段之间的关联
     *
     * @return
     */
    private TableColumnRelation initAuditTableToBisTableRelation() {
        TableColumnRelationDto dto = new TableColumnRelationDto();
        dto.setFieldTo(wfTableAndFlow.getTableInfo().getKeyColumn().get(0).getColumnDto().getColumnId());
        TableInfo tableAudit = SchemaHolder.findTableByTableName(CommonUtils.getTableName(WfAuditDataDto.class),
                WfConstants.DEFAULT_WF_SCHEMA, SessionUtils.getDefaultVersion());
        dto.setFieldFrom(tableAudit.findColumnByName(WfConstants.WfAuditField.bizField).getColumnDto().getColumnId());
        dto.setRelationType(Constants.TableRelationType.TYPE_ONE_ONE);
        dto.setVersionCode(tableAudit.getTableDto().getVersionCode());
        auditToBizRelation = new TableColumnRelation();
        auditToBizRelation.setDto(dto);
        auditToBizRelation.setTableTo(wfTableAndFlow.getTableInfo());
        auditToBizRelation.setTableFrom(tableAudit);
        return auditToBizRelation;
    }

    /**
     * 取得用户所有可操作的节点ID
     *
     * @param user
     * @return
     */
    private List<String> userCanOperNodeIds(LoginUser user) {
        List<WfNode> userCanOperNodes = findUserCanOperNodes(user);
        if (userCanOperNodes == null || userCanOperNodes.isEmpty()) {
            return null;
        }
        List<String> lstResult = new ArrayList<>();
        userCanOperNodes.forEach(el -> lstResult.add(el.getActId()));
        return lstResult;
    }

    /**
     * 取得当前用户
     *
     * @param user 登录用户
     * @return
     */
    private List<String> findUserAfterNodeIds(LoginUser user) {
        //取得当前用户节点,一般情况下,一个用户只可以操作一个节点,所以这里取第一个节点
        List<WfNode> lstCanOperNode = findUserCanOperNodes(user);
        if (lstCanOperNode == null || lstCanOperNode.isEmpty()) {
            return null;
        }
        WfNode userNode = lstCanOperNode.get(0);
        int index = this.lstNodes.indexOf(userNode);
        if (index == -1) {
            return null;
        }
        index++;
        List<String> lstResult = new ArrayList<>();
        int total = lstNodes.size();
        for (int i = index; i < this.lstNodes.size(); i++) {
            lstResult.add(lstNodes.get(i).getActId());
        }
        return lstResult;

    }

    /**
     * 取得已提交的任务的过滤条件,不包含已审核结束的
     * 方法，从审核表里查询提交的信息，并且不是待办的
     *
     * @param user 登录用户
     * @return
     */
    @Override
    public Criteria getCommittedTaskFilter(LoginUser user) {
        //未完成的任务条件
        //如果没有权限节点,则提交无效条件

        //增加可编辑节点,并且在审核主表里
        QueryParam queryParam = new QueryParam();
        String auditTable = CommonUtils.getTableName(WfAuditDataDto.class);
        queryParam.setTableDtos(WfConstants.DEFAULT_WF_SCHEMA, SessionUtils.getLoginVersion(), WfAuditDataDto.class);
        //并且节点在本节点后的
        List<String> userAfterNodeIds = findUserAfterNodeIds(user);
        if (userAfterNodeIds == null || userAfterNodeIds.isEmpty()) {
            return Criteria.getFalseExpression();
        }
        //增加表条件
        Criteria criteria = queryParam.appendCriteria().andIn(auditTable, "act_id", userAfterNodeIds)
                .andEqualTo(auditTable, "table_id", this.wfTableAndFlow.getTableInfo().getTableDto().getTableId());
        //增加与主表的关联
        queryParam.setLstRelation(Arrays.asList(auditToBizRelation));
        criteria.andExists(queryParam);
        return criteria;
    }

    /**
     * 取得指定节点的查询条件
     *
     * @param user 登录用户
     * @return
     */
    @Override
    public Criteria getNodeTaskFilter(LoginUser user, List<String> lstNodes) {
        if (lstNodes == null || lstNodes.isEmpty()) {
            //返回空条件
            return new Criteria();
        }

        //如果要查询已提交,则需要单独处理
        Criteria criFinished = null;
        Criteria criOther = null;
        if (lstNodes.indexOf(WfConstants.FixNodeIds.finished) != -1) {
            lstNodes.remove(WfConstants.FixNodeIds.finished);
            criFinished = getFinishedTaskFilter(user);
        }
        if (!lstNodes.isEmpty()) {
            criOther = getNormalNodesFilter(user, lstNodes);
        }
        if (criFinished != null && criOther != null) {
            Criteria criteria = new Criteria();
            criteria.addSubOrCriteria(criFinished);
            criteria.addSubOrCriteria(criOther);
        } else if (criFinished != null) {
            return criFinished;
        } else {
            return criOther;
        }
        return null;
    }

    /**
     * 取得普通节点的过滤条件
     *
     * @param user     登录用户
     * @param lstNodes
     * @return
     */
    private Criteria getNormalNodesFilter(LoginUser user, List<String> lstNodes) {
        Criteria criteria = new Criteria();
        String auditTable = CommonUtils.getTableName(WfAuditDataDto.class);
        criteria.andIn(auditTable, "act_id", lstNodes)
                .andEqualTo(auditTable, "table_id",
                        this.wfTableAndFlow.getTableInfo().getTableDto().getTableId());
        return criteria;
    }

    /**
     * 取得已结束的任务信息
     * 就是在审核表里不存在的信息
     * 条件一,审核的列ID,
     *
     * @param user
     * @return
     */
    public Criteria getFinishedTaskFilter(LoginUser user) {
        QueryParam queryParam = new QueryParam();
        String auditTable = CommonUtils.getTableName(WfAuditDataDto.class);
        queryParam.setTableDtos(WfConstants.DEFAULT_WF_SCHEMA, SessionUtils.getLoginVersion(), WfAuditDataDto.class);
        //增加表条件
        queryParam.appendCriteria()
                .andEqualTo(auditTable, "table_id", this.wfTableAndFlow.getTableInfo().getTableDto().getTableId());
        //增加与主表的关联
        queryParam.setLstRelation(Arrays.asList(auditToBizRelation));
        Criteria criteria = new Criteria();
        criteria.andNotExists(queryParam);
        return criteria;
    }

    @Override
    public WfWorkflowDto getWfInfo() {
        return wfTableAndFlow.getWorkflowDto();
    }
}
