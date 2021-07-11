package com.ranranx.aolie.wf.service.impl;

import com.ranranx.aolie.core.common.SessionUtils;
import com.ranranx.aolie.core.handler.HandleResult;
import com.ranranx.aolie.core.handler.HandlerFactory;
import com.ranranx.aolie.core.handler.param.QueryParam;
import com.ranranx.aolie.wf.WfConstants;
import com.ranranx.aolie.wf.dto.WfWorkflowDto;
import com.ranranx.aolie.wf.model.IFlowOperator;
import com.ranranx.aolie.wf.model.WfNode;
import com.ranranx.aolie.wf.service.DeployService;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.*;
import org.flowable.common.engine.impl.util.IoUtil;
import org.flowable.engine.*;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.image.ProcessDiagramGenerator;
import org.flowable.ui.common.security.SecurityUtils;
import org.flowable.ui.common.service.exception.BadRequestException;
import org.flowable.ui.common.service.exception.ConflictingRequestException;
import org.flowable.ui.common.service.exception.NotFoundException;
import org.flowable.ui.modeler.domain.Model;
import org.flowable.ui.modeler.model.ModelKeyRepresentation;
import org.flowable.ui.modeler.model.ModelRepresentation;
import org.flowable.ui.modeler.repository.ModelRepository;
import org.flowable.ui.modeler.serviceapi.ModelService;
import org.flowable.validation.ProcessValidator;
import org.flowable.validation.ProcessValidatorFactory;
import org.flowable.validation.ValidationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 部署服务实现
 *
 * @author xxl
 * @version V0.0.1
 * @date 2021/3/16 0016 20:42
 **/
@Service
@Transactional(readOnly = true)
public class DeployServiceImpl implements DeployService {

    private final String ZIP_FILE_EXTENSION = "zip";
    private final String BAR_FILE_EXTENSION = "bar";
    private final String BPMN_FILE_EXTENSION = "bpmn";
    private final String XML_FILE_EXTENSION = "xml";
    private final String IMG_FILE_EXTENSION = "png";

    private Logger logger = LoggerFactory.getLogger(DeployServiceImpl.class);
    @Autowired
    private HandlerFactory factory;

    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    protected ProcessEngine processEngine;
    @Autowired
    protected ModelRepository modelRepository;

    @Autowired
    private ModelService modelService;
    @Autowired
    private WfServiceImpl wfService;
    @Autowired
    private HistoryService historyService;
    @Autowired
    private RuntimeService runtimeService;


    private void startProcess() {

        RuntimeService runtimeService = processEngine.getRuntimeService();

        Map<String, Object> variables;
        variables = new HashMap<String, Object>();
        variables.put("employee", "employee");
        variables.put("nrOfHolidays", 3);
        variables.put("description", "这是说明");
        ProcessInstance processInstance =
                runtimeService.startProcessInstanceByKey("oneTaskProcess", variables);

    }

    private WfWorkflowDto findDtoInfo(Long wfId, String version) {
        if (wfId == null) {
            return null;
        }
        WfWorkflowDto dto = new WfWorkflowDto();
        dto.setWfId(wfId);
        dto.setVersionCode(version);
        QueryParam param = new QueryParam();
        param.setFilterObjectAndTableAndResultType(WfConstants.DEFAULT_WF_SCHEMA,
                version, dto);
        HandleResult result = factory.handleQuery(param);
        return (WfWorkflowDto) result.singleValue();
    }

    /**
     * 布署流程,并检查
     *
     * @param flowId
     * @return
     */
    @Override
    @Transactional(readOnly = false)
    public HandleResult validateModelAndDeploy(Long flowId) {
        WfWorkflowDto dtoInfo = findDtoInfo(flowId, SessionUtils.getLoginVersion());
        if (dtoInfo == null) {
            return HandleResult.failure("没有查询到流程信息");
        }
        Model model;
        try {
            model = modelService.getModel(dtoInfo.getModelId());
        } catch (NotFoundException e) {
            e.printStackTrace();
            return HandleResult.failure("没有查询到模板信息");

        }
        //这里强转,使其KEY值一致
        if (!dtoInfo.getWfKey().equals(model.getKey())) {
            return HandleResult.failure("模板流程Key与流程定义名不致");
        }
        BpmnModel bpmnModel = modelService.getBpmnModel(model);
        modelService.getModel(dtoInfo.getModelId());
        if (bpmnModel == null) {
            return HandleResult.failure("没有查询到指定的流程模板");
        }
        List<ValidationError> validationErrors = validateModel(bpmnModel);
        //查看有没有错误,警告就不提示了
        final ValidationError[] error = new ValidationError[1];
        if (validationErrors != null && !validationErrors.isEmpty()) {
            validationErrors.forEach(validationError -> {
                if (!validationError.isWarning()) {
                    error[0] = validationError;
                }
            });
            if (error[0] != null) {
                return HandleResult.failure("模板存在问题:" + error[0].getProblem());
            }
        }
        byte[] bpmnBytes = new BpmnXMLConverter().convertToXML(bpmnModel);

        try {
            //发布流程
            String processName = model.getName() + ".bpmn20.xml";
            repositoryService.createDeployment()
                    .name(model.getName())
                    .addString(processName, new String(bpmnBytes, "UTF-8"))
                    .deploy();

            //部署
            repositoryService.createDeployment().addBpmnModel(dtoInfo.getWfKey(), bpmnModel).deploy();
            wfService.updateFlowMainInfo(dtoInfo);
            return HandleResult.success(1);
        } catch (Exception e) {
            e.printStackTrace();
            return HandleResult.failure("部署出错 :" + e.getMessage());
        }
    }


    private List<ValidationError> validateModel(BpmnModel bpmnModel) {
        ProcessValidator validator = new ProcessValidatorFactory().createDefaultProcessValidator();
        return validator.validate(bpmnModel);
    }

    /**
     * 创建一个默认的模板
     *
     * @param key
     * @param name
     * @param description
     * @return
     */
    @Override
    @Transactional(readOnly = false)
    public Model createModel(String key, String name, String description) {
        ModelRepresentation representation = new ModelRepresentation();
        representation.setKey(key);
        representation.setName(name);
        representation.setModelType(0);
        representation.setDescription(description);

        representation.setKey(representation.getKey().replaceAll(" ", ""));
        checkForDuplicateKey(representation);

        String json = modelService.createModelJson(representation);

        return modelService.createModel(representation, json, SecurityUtils.getCurrentUserObject());
    }

    /**
     * 删除模板
     *
     * @param modelId
     */
    @Override
    public void deleteModel(String modelId) {
        try {
            Model model = modelRepository.get(modelId);
            if (model == null) {
                return;
            }
            modelService.deleteModel(modelId);
        } catch (Exception e) {
            logger.error("删除模板失败" + e.getMessage());
        }

    }

    protected void checkForDuplicateKey(ModelRepresentation modelRepresentation) {
        ModelKeyRepresentation modelKeyInfo = modelService.validateModelKey(null, modelRepresentation.getModelType(), modelRepresentation.getKey());
        if (modelKeyInfo.isKeyAlreadyExists()) {
            throw new ConflictingRequestException("Provided model key already exists: " + modelRepresentation.getKey());
        }
    }

    /**
     * 删除流程
     *
     * @param dto
     */
    @Override
    @Transactional(readOnly = false)
    public void deleteWf(WfWorkflowDto dto) {
        String wfKey = dto.getWfKey();
        List<Deployment> list = repositoryService.createDeploymentQuery().deploymentKey(wfKey).list();
        //删除所有版本
        if (list != null && !list.isEmpty()) {
            list.forEach(deployment -> {
                repositoryService.deleteDeployment(deployment.getId());
            });
        }
        //删除模板
        try {
            deleteModel(dto.getModelId());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    /**
     * 更新模板的信息
     *
     * @param dto
     * @return
     */
    @Override
    public HandleResult updateModel(WfWorkflowDto dto) {
        String name = dto.getWfName();
        String key = dto.getWfKey();
        String description = dto.getMemo();

        Model model;
        try {
            model = modelService.getModel(dto.getModelId());
        } catch (NotFoundException e) {
            e.printStackTrace();
            return HandleResult.failure("没有查询到模板信息");

        }

        ModelKeyRepresentation modelKeyInfo = modelService.validateModelKey(model, model.getModelType(), key);
        if (modelKeyInfo.isKeyAlreadyExists()) {
            throw new BadRequestException("Model with provided key already exists " + key);
        }

        model.setKey(key);
        model.setDescription(description);
        modelService.saveModel(model);
        return HandleResult.success(1);

    }

    /**
     * 取得流程图片
     *
     * @param processInstanceId
     * @return
     */
    @Override
    public byte[] genFlowImage(String processInstanceId) {
        //1.获取当前的流程实例
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
        String processDefinitionId = null;
        List<String> activeActivityIds = null;
        //2.获取所有的历史轨迹对象
        List<HistoricActivityInstance> list = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId).list();
        Map<String, HistoricActivityInstance> hisActivityMap = new HashMap<>();
        list.forEach(historicActivityInstance -> {
            if (!hisActivityMap.containsKey(historicActivityInstance.getActivityId())) {
                hisActivityMap.put(historicActivityInstance.getActivityId(), historicActivityInstance);
            }
        });
        if (processInstance != null) {
            processDefinitionId = processInstance.getProcessDefinitionId();
        } else {
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
            processDefinitionId = historicProcessInstance.getProcessDefinitionId();
        }
        //先取得节点信息
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
        List<EndEvent> endEvents = new ArrayList<>();
        List<FlowNode> flowNodes = new ArrayList<>();
        findFlowElement(bpmnModel, endEvents, flowNodes);

        if (processInstance != null) {
            //3.1. 正在运行的流程实例
            activeActivityIds = runtimeService.getActiveActivityIds(processInstanceId);
        } else {
            //3.2. 已经结束的流程实例
            activeActivityIds = new ArrayList<>();
            List<String> finalActiveActivityIds = activeActivityIds;
            endEvents.forEach(endEvent -> {
                if (hisActivityMap.containsKey(endEvent.getId())) {
                    finalActiveActivityIds.add(endEvent.getId());
                }
            });
        }

        Map<String, FlowNode> activityMap = flowNodes.stream().collect(Collectors.toMap(FlowNode::getId, flowNode -> flowNode));
        List<String> highLightedFlows = new ArrayList<>();
        //5. 递归得到高亮线
        activeActivityIds.forEach(activeActivityId -> this.getHighLightedFlows(activityMap, hisActivityMap, activeActivityId, highLightedFlows, activeActivityId));

        //7. 生成图片流
        ProcessEngineConfiguration engconf = processEngine.getProcessEngineConfiguration();
        ProcessDiagramGenerator diagramGenerator = engconf.getProcessDiagramGenerator();
        InputStream inputStream = diagramGenerator.generateDiagram(bpmnModel, "png",
                activeActivityIds, highLightedFlows, "宋体",
                "宋体", "宋体", null, 1.2,
                true);
        //8. 转化成byte便于网络传输
        return IoUtil.readInputStream(inputStream, "image inputStream name");
    }

    private void findFlowElement(BpmnModel bpmnModel, List<EndEvent> lstEndEvent, List<FlowNode> lstNode) {
        Collection<FlowElement> flowElements = bpmnModel.getMainProcess().getFlowElements();
        flowElements.forEach(flowElement -> {
            if (flowElement instanceof FlowNode) {
                lstNode.add((FlowNode) flowElement);
            }
            if (flowElement instanceof EndEvent) {
                lstEndEvent.add((EndEvent) flowElement);
            }
        });
    }

    private void getHighLightedFlows(
            Map<String, FlowNode> flowNodeMap,
            Map<String, HistoricActivityInstance> hisActivityMap,
            String activeActivityId,
            List<String> highLightedFlows,
            String oldActivityId) {
        FlowNode flowNode = flowNodeMap.get(activeActivityId);
        List<SequenceFlow> incomingFlows = flowNode.getIncomingFlows();
        for (SequenceFlow sequenceFlow : incomingFlows) {
            String sourceRefId = sequenceFlow.getSourceRef();
            if (hisActivityMap.containsKey(sourceRefId) && !oldActivityId.equals(sourceRefId)) {
                highLightedFlows.add(sequenceFlow.getId());
                this.getHighLightedFlows(flowNodeMap, hisActivityMap, sourceRefId, highLightedFlows, oldActivityId);
            } else {
                if (hisActivityMap.containsKey(sourceRefId)) {
                    highLightedFlows.add(sequenceFlow.getId());
                }
                break;
            }
        }
    }

    /**
     * 生成业务流程图片
     *
     * @param bussId
     * @param tableId
     * @param version
     * @return
     */
    @Override
    public byte[] getFlowImage(Long bussId, Long tableId, String version) {
        IFlowOperator operator = wfService.getWfOperByTable(tableId, version);
        List<WfNode> lstNode = operator.getTaskStatus(bussId);
        if (lstNode == null || lstNode.isEmpty()) {
            return null;
        }
        return genFlowImage(lstNode.get(0).getProcessInstanceId());
    }
}