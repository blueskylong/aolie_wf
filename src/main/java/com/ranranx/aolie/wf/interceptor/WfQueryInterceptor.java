package com.ranranx.aolie.wf.interceptor;

import com.ranranx.aolie.core.annotation.DbOperInterceptor;
import com.ranranx.aolie.core.common.CommonUtils;
import com.ranranx.aolie.core.common.Constants;
import com.ranranx.aolie.core.common.Ordered;
import com.ranranx.aolie.core.common.SessionUtils;
import com.ranranx.aolie.core.datameta.datamodel.SchemaHolder;
import com.ranranx.aolie.core.datameta.datamodel.TableColumnRelation;
import com.ranranx.aolie.core.datameta.datamodel.TableInfo;
import com.ranranx.aolie.core.datameta.dto.TableColumnRelationDto;
import com.ranranx.aolie.core.ds.definition.Field;
import com.ranranx.aolie.core.exceptions.InvalidException;
import com.ranranx.aolie.core.handler.HandleResult;
import com.ranranx.aolie.core.handler.HandlerFactory;
import com.ranranx.aolie.core.handler.param.OperParam;
import com.ranranx.aolie.core.handler.param.QueryParam;
import com.ranranx.aolie.core.handler.param.condition.Criteria;
import com.ranranx.aolie.core.interceptor.IOperInterceptor;
import com.ranranx.aolie.wf.WfConstants;
import com.ranranx.aolie.wf.dto.WfAuditDataDto;
import com.ranranx.aolie.wf.model.IFlowOperator;
import com.ranranx.aolie.wf.service.impl.WfServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

import static com.ranranx.aolie.wf.WfConstants.CLIENT_DISPLAY_FIELD_NAME;
import static com.ranranx.aolie.wf.WfConstants.WfAuditField;

/**
 * 查询增加流程条件的拦截器
 * <p>
 * 约定:1.前端在生成列表时,增加一列流程节点的信息,这个虚拟的列名组成
 * 为:JQParameter.PLUG_FILTER_PREFIX+WfConstants.FLOW_FILTER_PREFIX+tableId
 * 2. 一个用户在一个角色(用户ID)下,只能定位到一个流程中的一个节点上.这样就可以在用户登录时,确定他的审核位置,利于判断已提交的数据.
 * 如里对多个节点有权限,则会停到第一个有权限的节点上
 *
 * @author xxl
 * @version V0.0.1
 * @date 2021/3/7 0007 19:49
 **/
@DbOperInterceptor
public class WfQueryInterceptor implements IOperInterceptor {

    private Logger logger = LoggerFactory.getLogger(WfQueryInterceptor.class);
    /**
     * 状态虚拟字段
     */
    private final String WF_STATUS_FIELD = "WF_STATE";

    @Autowired
    private WfServiceImpl wfService;

    @Autowired
    private HandlerFactory factory;

    private static String auditTable = CommonUtils.getTableName(WfAuditDataDto.class);

    /**
     * 查询,删除,修改数据,都需要检查一下流程节点情况,其中修改和删除,是判断是否有权限,而查询是增加流程节点条件
     *
     * @param type
     * @param objExtinfo
     * @return
     */
    @Override
    public boolean isCanHandle(String type, Object objExtinfo) {
        return Constants.HandleType.TYPE_QUERY.equals(type);
    }

    /**
     * 这里增加流程条件
     *
     * @param param
     * @param handleType
     * @param globalParamData
     * @return
     * @throws InvalidException
     */
    @Override
    public HandleResult beforeOper(OperParam param, String handleType, Map<String, Object> globalParamData)
            throws InvalidException {
        QueryParam queryParam = (QueryParam) param;
        if (queryParam.getTable() == null) {
            return null;
        }
        Map<String, Object> plugFilter = queryParam.getPlugFilter();
        IFlowOperator flowOperator = addBizCol(queryParam, globalParamData);

        if (flowOperator == null || plugFilter == null || plugFilter.isEmpty()) {
            return null;
        }

        TableInfo[] tables = queryParam.getTables();
        //判断有没有表需要增加条件,目前只支持单表流程过滤,如果这里是多个表,可以去查询一次,看哪个有流程
        Long tableId = 0L;
        if (tables.length > 1) {
            for (TableInfo table : tables) {
                if (wfService.getWfOperByTable(table.getTableDto().getTableId(), SessionUtils.getLoginVersion()) != null) {
                    tableId = table.getTableDto().getTableId();
                }
            }
        } else {
            tableId = tables[0].getTableDto().getTableId();
        }
        Map<Long, List<String>> flowCondition = findFlowCondition(plugFilter, tableId);
        for (int i = 0; i < tables.length; i++) {
            makeOneTableFilter(tables[i], queryParam, flowCondition);
        }
        return null;
    }

    /**
     * 增加业务键查询,以便后面做流程状态查询
     *
     * @param queryParam
     * @return
     */
    private IFlowOperator addBizCol(QueryParam queryParam, Map<String, Object> globalParam) {
        TableInfo[] tables = queryParam.getTables();

        List<Field> fields = queryParam.getFields();
        boolean isAddAllField = fields.isEmpty();
        IFlowOperator wfOper = null;
        //循环,这里只处理第一个查找到的流程信息
        for (TableInfo table : tables) {
            wfOper = wfService.getWfOperByTable(table.getTableDto().getTableId(), table.getTableDto().getVersionCode());
            if (wfOper != null) {
                Field field = new Field();
                field.setTableName(auditTable);
                field.setFieldName(WfAuditField.actField);
                field.setAliasName(CLIENT_DISPLAY_FIELD_NAME);
                field.setDefaultValue("无");

                if (isAddAllField) {
                    fields.add(Field.genFieldAll(table.getTableDto().getTableName()));
                }
                fields.add(field);
                TableColumnRelation tableRelation = genRelation(table, queryParam, wfOper.getWfInfo().getWfId());
                List<TableColumnRelation> lstRelation = queryParam.getLstRelation();
                if (lstRelation == null) {
                    lstRelation = new ArrayList<>();
                    queryParam.setLstRelation(lstRelation);
                }
                lstRelation.add(tableRelation);

            }
        }
        return wfOper;
    }

    private TableColumnRelation genRelation(TableInfo tableInfo, QueryParam param, Long wfId) {
        TableColumnRelationDto dto = new TableColumnRelationDto();
        TableInfo tableAudit = SchemaHolder.findTableByTableName(auditTable,
                WfConstants.DEFAULT_WF_SCHEMA, tableInfo.getTableDto().getVersionCode());
        dto.setFieldTo(tableAudit.findColumnByName(WfAuditField.bizField).getColumnDto().getColumnId());
        dto.setFieldFrom(tableInfo.getKeyColumn().get(0).getColumnDto().getColumnId());
        dto.setRelationType(Constants.TableRelationType.TYPE_ONE_ONE0);
        dto.setVersionCode(tableAudit.getTableDto().getVersionCode());
        TableColumnRelation auditToBizRelation = new TableColumnRelation();
        auditToBizRelation.setDto(dto);
        auditToBizRelation.setTableTo(tableAudit);
        auditToBizRelation.setTableFrom(tableInfo);

        auditToBizRelation.appendCriteria().andEqualTo(auditTable,
                WfAuditField.tableId, tableInfo.getTableDto().getTableId())
                .andEqualTo(auditTable, WfAuditField.wfIdField, wfId);
        param.appendTable(tableAudit);

        return auditToBizRelation;
    }


    private void makeOneTableFilter(TableInfo tableInfo, QueryParam param, Map<Long, List<String>> flowCondition) {
        //先判断此表有没有关联流程
        IFlowOperator wfOper = wfService.getWfOperByTable(tableInfo.getTableDto().getTableId(),
                tableInfo.getTableDto().getVersionCode());
        if (wfOper == null) {
            return;
        }
        List<String> lstNodes = flowCondition.get(tableInfo.getTableDto().getTableId());
        //如果前端没有指定节点条件,则显示全部
        if (lstNodes == null || lstNodes.isEmpty()) {
            return;
        }
        //根据第一个节点来判断是哪种过滤类型
        String firstNode = lstNodes.get(0);
        if (firstNode.equals(WfConstants.WfTypeNode.NODE_ALL)) {
            genAllFilter(tableInfo, param, wfOper);
        } else if (firstNode.equals(WfConstants.WfTypeNode.NODE_PASS)) {
            genPassFilter(param, wfOper);
        } else if (firstNode.equals(WfConstants.WfTypeNode.NODE_TODO)) {
            genTodoFilter(tableInfo, param, wfOper);
        } else {
            genNodesFilter(param, wfOper, lstNodes);
        }


    }

    /**
     * 生成本人已审核条件
     * 思路: 审核过的都在审核表里存在记录,
     *
     * @param param
     * @param wfOper
     */
    private void genPassFilter(QueryParam param,
                               IFlowOperator wfOper) {
        Criteria criteria = wfOper.getCommittedTaskFilter(SessionUtils.getLoginUser());
        param.addCriteria(criteria);

    }

    /**
     * 生成待办条件
     *
     * @param tableInfo
     * @param param
     * @param wfOper
     */
    private void genTodoFilter(TableInfo tableInfo, QueryParam param, IFlowOperator wfOper) {
        Criteria criteria = wfOper.getCurrentUserTodoTaskFilter(SessionUtils.getLoginUser());
        param.addCriteria(criteria);
    }

    /**
     * 生成所有节点的条件
     * 如果是所有,则不增加条件
     *
     * @param tableInfo
     * @param param
     * @param wfOper
     */
    private void genAllFilter(TableInfo tableInfo, QueryParam param,
                              IFlowOperator wfOper) {
        return;
    }

    /**
     * 生成指定节点条件
     *
     * @param param
     * @param wfOper
     */
    private void genNodesFilter(QueryParam param,
                                IFlowOperator wfOper, List<String> lstNodes) {
        Criteria nodeTaskFilter = wfOper.getNodeTaskFilter(SessionUtils.getLoginUser(), lstNodes);
        param.addCriteria(nodeTaskFilter);
    }


    @Override
    public int getOrder() {
        return Ordered.BASE_ORDER - 20;
    }

    /**
     * 根据前端条件,生成流程节点的条件
     *
     * @param filter key:WfConstants.FLOW_FILTER_PREFIX
     * @return key: tableId,value:NodeID.或者约定的虚拟类型
     */
    private Map<Long, List<String>> findFlowCondition(Map<String, Object> filter, Long tableId) {
        Iterator<Map.Entry<String, Object>> iterator = filter.entrySet().iterator();
        Map<Long, List<String>> mapResult = new HashMap<>();
        while (iterator.hasNext()) {
            Map.Entry<String, Object> next = iterator.next();
            String key = next.getKey();
            //首先插件条件需要是流程条件
            if (key.equals(WfConstants.FLOW_FILTER_PREFIX)) {
                //再判断此表是不是存在流程,这一步只是防止客户端异常,一般情况下,不会出现
                IFlowOperator wfInfoByTable = wfService.getWfOperByTable(tableId, SessionUtils.getLoginVersion());
                if (wfInfoByTable == null) {
                    logger.error("客户端发生错误,提交没有流程配置的查询条件");
                    continue;
                }
                Object value = next.getValue();
                if (CommonUtils.isEmpty(value)) {
                    mapResult.put(tableId, Arrays.asList(WfConstants.WfTypeNode.NODE_ALL));
                } else {
                    mapResult.put(tableId, Arrays.asList(value.toString().split(",")));
                }
            }
        }
        return mapResult;
    }
}
