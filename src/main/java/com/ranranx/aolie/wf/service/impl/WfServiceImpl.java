package com.ranranx.aolie.wf.service.impl;

import com.alibaba.druid.support.monitor.annotation.MField;
import com.ranranx.aolie.application.right.dto.Role;
import com.ranranx.aolie.application.right.service.RightService;
import com.ranranx.aolie.application.user.dto.UserDto;
import com.ranranx.aolie.application.user.service.UserService;
import com.ranranx.aolie.core.common.CommonUtils;
import com.ranranx.aolie.core.common.Constants;
import com.ranranx.aolie.core.common.SessionUtils;
import com.ranranx.aolie.core.datameta.datamodel.SchemaHolder;
import com.ranranx.aolie.core.handler.HandleResult;
import com.ranranx.aolie.core.handler.HandlerFactory;
import com.ranranx.aolie.core.handler.param.QueryParam;
import com.ranranx.aolie.core.handler.param.UpdateParam;
import com.ranranx.aolie.core.interfaces.ICacheRefTableChanged;
import com.ranranx.aolie.core.service.DmDataService;
import com.ranranx.aolie.wf.WfConstants;
import com.ranranx.aolie.wf.dto.WfAuditDataDto;
import com.ranranx.aolie.wf.dto.WfTable2flowDto;
import com.ranranx.aolie.wf.dto.WfTableAndFlow;
import com.ranranx.aolie.wf.dto.WfWorkflowDto;
import com.ranranx.aolie.wf.model.FlowInfo;
import com.ranranx.aolie.wf.model.FlowOperFactory;
import com.ranranx.aolie.wf.model.IFlowOperator;
import com.ranranx.aolie.wf.model.WfNode;
import com.ranranx.aolie.wf.service.WfService;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.User;
import org.flowable.idm.engine.impl.persistence.entity.UserEntity;
import org.flowable.ui.common.model.GroupRepresentation;
import org.flowable.ui.common.model.RemoteUser;
import org.flowable.ui.common.model.ResultListDataRepresentation;
import org.flowable.ui.common.model.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.*;

/**
 * @author xxl
 * @version V0.0.1
 * @date 2021/3/8 0008 8:29
 **/
@Service
@Order(7)
public class WfServiceImpl implements ICacheRefTableChanged, CommandLineRunner, WfService {

    @Autowired
    private AutowireCapableBeanFactory beanFactory;

    @Autowired
    private HandlerFactory handlerFactory;
    @Autowired
    private HandlerFactory factory;

    @Autowired
    private DmDataService dmDataService;

    private String tableWorkFlow = CommonUtils.getTableName(WfWorkflowDto.class);
    private String tableTable2Flow = CommonUtils.getTableName(WfTable2flowDto.class);
    /**
     * 表名对流程  key version_tableId
     */
    private static Map<String, IFlowOperator> mapTableIdToFlowOper;
    /**
     * flowId对流程DTO key:version_flowId
     */
    private static Map<String, WfWorkflowDto> mapFlowIdToFlowDto;
    /**
     * flowId对流程信息 key:version_flowId
     */
    private static Map<String, FlowInfo> mapFlowInfo;
    /**
     * key:version
     */
    private static Map<String, List<WfTable2flowDto>> mapTable2Flow;

    @Autowired
    private UserService userService;

    @Autowired
    private RightService rightService;

    @Override
    public List<String> getCareTables() {
        return Arrays.asList(tableWorkFlow,
                tableTable2Flow);
    }

    /**
     * 取得业务主表对应的流程
     *
     * @param tableId
     * @param version
     * @return
     */
    public IFlowOperator getWfOperByTable(Long tableId, String version) {
        if (tableId == null || version == null || mapTableIdToFlowOper == null) {
            return null;
        }
        return mapTableIdToFlowOper.get(CommonUtils.makeKey(tableId.toString(), version));
    }

    /**
     * 刷新或初始化
     *
     * @param tableName
     */
    @Override
    public void refresh(String tableName) {

        mapTableIdToFlowOper = new HashMap<>();
        mapFlowIdToFlowDto = new HashMap<>();
        mapFlowInfo = new HashMap<>();
        mapTable2Flow = new HashMap<>();
        List<WfWorkflowDto> lstWf = findAllFlow();
        if (lstWf == null || lstWf.isEmpty()) {
            return;
        }
        lstWf.forEach(el ->
                mapFlowIdToFlowDto.put(CommonUtils.makeKey(el.getWfId().toString(), el.getVersionCode()), el));
        List<WfTable2flowDto> lstTableFlowDto = findAllTableFlow();
        if (lstTableFlowDto == null || lstTableFlowDto.isEmpty()) {
            return;
        }
        lstTableFlowDto.forEach(el ->
        {
            String version = el.getVersionCode();
            WfTableAndFlow tableAndFlow = new WfTableAndFlow();
            tableAndFlow.setTable2flowDto(el);
            tableAndFlow.setTableInfo(SchemaHolder.getTable(el.getTableId(), version));
            tableAndFlow.setWorkflowDto(mapFlowIdToFlowDto.get(CommonUtils.makeKey(el.getWfId().toString(), version)));
            IFlowOperator flowOper = FlowOperFactory.createFlowOper(tableAndFlow);
            //注入服务
            beanFactory.autowireBean(flowOper);
            flowOper.init();
            mapTableIdToFlowOper.put(CommonUtils.makeKey(el.getTableId().toString(), version),
                    flowOper);
            String key = CommonUtils.makeKey(el.getWfId().toString(), version);
            //这里可能会是有重复的情况,多表对应一个流程,所以判断一下
            if (!mapFlowInfo.containsKey(key)) {
                FlowInfo flowInfo = new FlowInfo(tableAndFlow.getWorkflowDto(), flowOper.getWfNode());
                mapFlowInfo.put(key, flowInfo);
            }
            List<WfTable2flowDto> wfTable2flowDtos = mapTable2Flow.computeIfAbsent(el.getVersionCode(), ver -> new ArrayList<>());
            wfTable2flowDtos.add(el);
        });

    }

    public HandleResult updateFlowMainInfo(WfWorkflowDto dtoInfo) {
        //更新部署信息
        dtoInfo.setDeployTime(Constants.DATE_FORMAT.format(new Date()));
        dtoInfo.setDeployUser(SessionUtils.getLoginUser().getUserId());
        UpdateParam param = new UpdateParam();
        Map<String, Object> mapValue = CommonUtils.toMap(dtoInfo, true, true);
        param.setLstRows(Arrays.asList(mapValue));
        param.setTable(SchemaHolder.findTableByTableName(CommonUtils.getTableName(WfWorkflowDto.class),
                WfConstants.DEFAULT_WF_SCHEMA, SessionUtils.getLoginVersion()));
        return factory.handleUpdate(param);
    }


    /**
     * 取得审核的DTO
     *
     * @param tableId
     * @param bussKey
     * @param version
     * @return
     */
    public WfAuditDataDto findAuditDto(Long tableId, Long bussKey, String version) {
        QueryParam param = new QueryParam();
        param.setResultClass(WfAuditDataDto.class);
        WfAuditDataDto dto = new WfAuditDataDto();
        dto.setTableId(tableId);
        dto.setBusinessId(bussKey);
        param.setFilterObjectAndTableAndResultType(WfConstants.DEFAULT_WF_SCHEMA, version, dto);
        HandleResult result = factory.handleQuery(param);
        if (!result.isSuccess()) {
            return null;
        }
        return (WfAuditDataDto) result.singleValue();

    }

    /**
     * 查询审核表的行数据
     *
     * @param tableId
     * @param bussKey
     * @param version
     * @return
     */
    public Map<String, Object> findTableRow(Long tableId, Long bussKey, String version) {
        HandleResult result = dmDataService.findTableRow(tableId, bussKey, version);
        return (Map<String, Object>) result.singleValue();
    }

    private List<WfWorkflowDto> findAllFlow() {
        QueryParam queryParam = new QueryParam();
        queryParam.setResultClass(WfWorkflowDto.class);
        queryParam.setTableDtos(WfConstants.DEFAULT_WF_SCHEMA, SessionUtils.getDefaultVersion(), WfWorkflowDto.class);
        queryParam.setNoVersionFilter(true);
        HandleResult result = handlerFactory.handleQuery(queryParam);
        if (result.isSuccess()) {
            return (List<WfWorkflowDto>) result.getData();
        }
        return null;
    }

    /**
     * 取得表所有数据
     *
     * @return
     */
    private List<WfTable2flowDto> findAllTableFlow() {
        QueryParam queryParam = new QueryParam();
        queryParam.setResultClass(WfTable2flowDto.class);
        queryParam.setTableDtos(WfConstants.DEFAULT_WF_SCHEMA, SessionUtils.getDefaultVersion(), WfTable2flowDto.class);
        queryParam.setNoVersionFilter(true);
        HandleResult result = handlerFactory.handleQuery(queryParam);
        if (result.isSuccess()) {
            return (List<WfTable2flowDto>) result.getData();
        }
        return null;
    }

    @Override
    public void run(String... args) throws Exception {
        refresh(null);
    }

    /**
     * 查询一版本下的流程定义
     *
     * @param version
     * @return
     */
    public static List<FlowInfo> findFlowInfoByVersion(String version) {
        if (mapFlowInfo.isEmpty()) {
            return null;
        }
        List<FlowInfo> lstFlow = new ArrayList<>();
        mapFlowInfo.forEach((key, value) -> {
            if (value.getFlowDto().getVersionCode().equalsIgnoreCase(version)) {
                lstFlow.add(value);
            }
        });
        return lstFlow;
    }


    /**
     * 查询一版本下,表和流程的关系信息
     *
     * @param version
     * @return
     */
    public static List<WfTable2flowDto> findTable2FlowByVersion(String version) {
        return mapTable2Flow.get(version);
    }

    /**
     * 审核提交或者退回,根据条件由引擎判断
     *
     * @param dsId
     * @param bussKey
     * @return
     */
    @Override
    public HandleResult commit(long dsId, long bussKey, Map<String, Object> mapValues) {
        IFlowOperator wfOperByTable = getWfOperByTable(dsId, SessionUtils.getLoginVersion());
        if (wfOperByTable == null) {
            return HandleResult.failure("没有查询到相关流程");
        }
        WfAuditDataDto auditRow = findAuditDto(dsId, bussKey, SessionUtils.getLoginVersion());
        if (auditRow == null) {
            return HandleResult.failure("没有查询到审核信息");
        }
        Map<String, Object> row = findTableRow(dsId, bussKey, SessionUtils.getLoginVersion());
        if (row != null) {
            row.putAll(mapValues);
        }
        //根据表和bussKey,查询流程相关信息
        WfNode node = wfOperByTable.commit(bussKey, auditRow.getProcInstId(), SessionUtils.getLoginUser(), row);
        if (node == null) {
            return HandleResult.failure("提交失败");
        } else {
            return HandleResult.success(1);
        }

    }

    /**
     * 撤回上次的提交
     *
     * @param dsId
     * @param bussKey
     * @return
     */
    @Override
    public HandleResult rollBack(long dsId, Long bussKey) {

        String version = SessionUtils.getLoginVersion();
        IFlowOperator wfOperator = getWfOperByTable(dsId, SessionUtils.getLoginVersion());
        if (wfOperator == null) {
            return HandleResult.failure("没有查询到相关流程");
        }
        WfAuditDataDto auditDto = findAuditDto(dsId, bussKey, version);
        if (auditDto == null) {
            return HandleResult.failure("没有查询到审核信息");
        }
        if (wfOperator.rollBack(auditDto.getProcInstId(), dsId)) {
            return HandleResult.success(1);
        } else {
            return HandleResult.failure("流程撤消失败");
        }
    }

    /**
     * 取得角色信息
     *
     * @param filter
     * @return
     */
    @Override
    public ResultListDataRepresentation getGroups(String filter) {
        List<GroupRepresentation> result = new ArrayList<>();

        List<Role> lstRole = rightService.findRoleByName(filter);
        if (lstRole == null || lstRole.isEmpty()) {
            return null;
        }
        for (Role role : lstRole) {
            result.add(toGroupRep(role));
        }
        return new ResultListDataRepresentation(result);
    }

    private GroupRepresentation toGroupRep(Role role) {
        GroupRepresentation groupRepresentation = new GroupRepresentation();
        groupRepresentation.setId(role.getRoleId().toString());
        groupRepresentation.setName(role.getRoleName());
        groupRepresentation.setType(role.getRoleType() == null ? "" : role.getRoleType().toString());
        return groupRepresentation;
    }

    /**
     * 取得用户信息
     *
     * @param filter
     * @return
     */
    @Override
    public ResultListDataRepresentation getUsers(String filter) {
        List<UserDto> lstUser = userService.findUserByCodeOrName(filter);

        if (lstUser == null || lstUser.isEmpty()) {
            return new ResultListDataRepresentation();
        }
        List<UserRepresentation> userRepresentations = new ArrayList<>(lstUser.size());
        for (UserDto user : lstUser) {
            userRepresentations.add(toUser(user));
        }
        return new ResultListDataRepresentation(userRepresentations);
    }

    private UserRepresentation toUser(UserDto userDto) {
        UserRepresentation user = new UserRepresentation();
        user.setId(userDto.getUserId().toString());
        user.setEmail(userDto.getEMail());
        user.setFirstName(userDto.getAccountCode());
        user.setLastName(userDto.getUserName());
        return user;
    }
}
