package com.ranranx.aolie.wf.interceptor;

import com.ranranx.aolie.core.annotation.DbOperInterceptor;
import com.ranranx.aolie.core.common.CommonUtils;
import com.ranranx.aolie.core.common.Constants;
import com.ranranx.aolie.core.common.Ordered;
import com.ranranx.aolie.core.common.SessionUtils;
import com.ranranx.aolie.core.datameta.datamodel.TableInfo;
import com.ranranx.aolie.core.exceptions.InvalidException;
import com.ranranx.aolie.core.handler.HandleResult;
import com.ranranx.aolie.core.handler.HandlerFactory;
import com.ranranx.aolie.core.handler.param.*;
import com.ranranx.aolie.core.interceptor.IOperInterceptor;
import com.ranranx.aolie.wf.WfConstants;
import com.ranranx.aolie.wf.dto.WfWorkflowDto;
import com.ranranx.aolie.wf.model.IFlowOperator;
import com.ranranx.aolie.wf.service.DeployService;
import com.ranranx.aolie.wf.service.impl.WfServiceImpl;
import org.flowable.ui.modeler.domain.Model;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 删除修改主表时的联动
 *
 * @author xxl
 * @version V0.0.1
 * @date 2021/5/31 0031 17:17
 **/
@DbOperInterceptor
public class WfUpdateOrDeleteInterceptor implements IOperInterceptor {
    public static String GLOBAL_PARAM_FOR_DELETE = "GLOBAL_PARAM_FOR_DELETE";


    @Autowired
    private WfServiceImpl wfService;

    @Autowired
    private DeployService deployService;

    @Autowired
    private HandlerFactory factory;


    @Override
    public boolean isCanHandle(String type, Object objExtinfo) {
        return Constants.HandleType.TYPE_DELETE.equals(type)
                || Constants.HandleType.TYPE_UPDATE.equals(type)
                || Constants.HandleType.TYPE_INSERT.equals(type);
    }

    @Override
    public HandleResult beforeOper(OperParam param, String handleType, Map<String, Object> globalParamData) throws InvalidException {
        if (Constants.HandleType.TYPE_DELETE.equals(handleType)) {
            collectDeleteDto((DeleteParam) param, globalParamData);
            return null;
        } else if (Constants.HandleType.TYPE_UPDATE.equals(handleType)) {
            filterFields(((UpdateParam) param).getLstRows());
        } else if (Constants.HandleType.TYPE_INSERT.equals(handleType)) {
            filterFields(((InsertParam) param).getLstRows());
        }
        return null;
    }

    /**
     * 删除前收集需要删除的流程明细信息
     *
     * @param param
     */
    private void collectDeleteDto(DeleteParam param, Map<String, Object> globalParam) {
        List<Object> ids = param.getIds();
        if (param.getTable() == null) {
            return;
        }
        String tableName = param.getTable().getTableDto().getTableName();
        if (CommonUtils.getTableName(WfWorkflowDto.class).equals(tableName)) {
            QueryParam queryParam = new QueryParam();
            queryParam.setTableDtos(WfConstants.DEFAULT_WF_SCHEMA, SessionUtils.getLoginVersion(), WfWorkflowDto.class);
            queryParam.appendCriteria().andIn(tableName, "wf_id", ids);
            queryParam.setResultClass(WfWorkflowDto.class);
            HandleResult result = factory.handleQuery(queryParam);
            List<WfWorkflowDto> lstDto = (List<WfWorkflowDto>) result.getData();
            if (lstDto == null || lstDto.isEmpty()) {
                return;
            }
            //放入全局参数中
            globalParam.put(GLOBAL_PARAM_FOR_DELETE, lstDto);
        }
    }


    /**
     * 这里处理流程相关的操作
     *
     * @param param
     * @param handleType
     * @param globalParamData
     * @param handleResult
     * @return
     */
    @Override
    public HandleResult beforeReturn(OperParam param, String handleType,
                                     Map<String, Object> globalParamData, HandleResult handleResult) {
        if (Constants.HandleType.TYPE_INSERT.equals(handleType)) {
            checkAndStartWf((InsertParam) param, globalParamData);
            //如果是要插入,则要生成模板
            checkAndAddModel((InsertParam) param);
        } else if (Constants.HandleType.TYPE_DELETE.equals(handleType)) {
            checkAndDeleteWf((DeleteParam) param, globalParamData);
            checkAndDeleteWorkflowDefine(globalParamData);
        } else if (Constants.HandleType.TYPE_UPDATE.equals(handleType)) {
            updateModelInfo((UpdateParam) param);
        }
        return null;
    }

    private void checkAndAddModel(InsertParam param) {
        String tableName = param.getTable().getTableDto().getTableName();
        if (CommonUtils.getTableName(WfWorkflowDto.class).equals(tableName)) {
            List<Map<String, Object>> lstRows = param.getLstRows();
            if (lstRows == null || lstRows.isEmpty()) {
                return;
            }
            lstRows.forEach(row -> {
                try {
                    WfWorkflowDto wfWorkflowDto = CommonUtils.populateBean(WfWorkflowDto.class, CommonUtils.convertToCamel(row));
                    Model model = deployService.createModel(
                            wfWorkflowDto.getWfKey(), wfWorkflowDto.getWfName(), "");
                    wfWorkflowDto.setModelId(model.getId());
                    UpdateParam updateParam = UpdateParam.genUpdateByObject(WfConstants.DEFAULT_WF_SCHEMA, SessionUtils.getLoginVersion(),
                            wfWorkflowDto, true);
                    factory.handleUpdate(updateParam);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            });

        }
    }

    private HandleResult updateModelInfo(UpdateParam param) {
        TableInfo tableInfo = param.getTable();
        if (tableInfo == null) {
            return null;
        }
        String tableName = param.getTable().getTableDto().getTableName();
        if (!CommonUtils.getTableName(WfWorkflowDto.class).equals(tableName)) {
            return null;
        }
        List<Map<String, Object>> lstRows = param.getLstRows();
        if (lstRows == null || lstRows.isEmpty()) {
            return null;
        }
        lstRows.forEach(row -> {
            try {
                WfWorkflowDto wfWorkflowDto = CommonUtils.populateBean(WfWorkflowDto.class, CommonUtils.convertToCamel(row));
                deployService.updateModel(wfWorkflowDto);
            } catch (Exception e) {

            }
        });
        return HandleResult.success(1);
    }

    private void checkAndDeleteWorkflowDefine(Map<String, Object> globalParam) {
        Object lst = globalParam.remove(GLOBAL_PARAM_FOR_DELETE);
        if (lst == null) {
            return;
        }
        List<WfWorkflowDto> lstDto = (List<WfWorkflowDto>) lst;
        if (lstDto.isEmpty()) {
            return;
        }
        lstDto.forEach(wfWorkflowDto -> {
            deployService.deleteWf(wfWorkflowDto);
        });
    }

    private void checkAndDeleteWf(DeleteParam param, Map<String, Object> globalParamData) {
        TableInfo tableInfo = param.getTable();
        if (tableInfo == null) {
            return;
        }
        //取得流程操作服务
        IFlowOperator iOper = wfService.getWfOperByTable(tableInfo.getTableDto().getTableId(),
                tableInfo.getTableDto().getVersionCode());
        if (iOper != null && param.getIds() != null && !param.getIds().isEmpty()) {
            //TODO 注意这里,只处理了指定ID的方式
            String userId = String.valueOf(SessionUtils.getLoginUser().getUserId());
            param.getIds().forEach(id -> {
                iOper.deleteFlowInstance(Long.parseLong(id.toString()), userId, null);
            });
        }
    }

    /**
     * 删除数据行中的流程虚拟列
     *
     * @param lstRows
     */
    private void filterFields(List<Map<String, Object>> lstRows) {
        if (lstRows == null || lstRows.isEmpty()) {
            return;
        }
        lstRows.forEach(map -> {
            map.remove(WfConstants.CLIENT_DISPLAY_FIELD_NAME);
        });
    }

    /**
     * 检查是不是需要流程,如果有则开始流程
     *
     * @param insertParam
     * @param globalParamData
     */
    private void checkAndStartWf(InsertParam insertParam, Map<String, Object> globalParamData) {
        TableInfo tableInfo = insertParam.getTable();
        if (tableInfo == null) {
            return;
        }
        //取得流程操作服务
        IFlowOperator wfOperByTable = wfService.getWfOperByTable(tableInfo.getTableDto().getTableId(),
                tableInfo.getTableDto().getVersionCode());
        if (wfOperByTable == null) {
            return;
        }
        List<Map<String, Object>> lstRows = insertParam.getLstRows();
        if (lstRows == null || lstRows.isEmpty()) {
            return;
        }

        String keyField = tableInfo.getKeyField();
        lstRows.forEach(map -> {
            //检查此表是不是流程相关表
            Map values = new HashMap();
            values.putAll(map);
            values.putAll(globalParamData);
            wfOperByTable.startFlow(CommonUtils.getLongField(map, keyField), String.valueOf(SessionUtils.getLoginUser().getUserId()), values);
        });
    }

    @Override
    public int getOrder() {
        return Ordered.BASE_ORDER - 19;
    }
}
