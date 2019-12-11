package com.bonc.activity.controller;

import com.alibaba.fastjson.JSONObject;
import com.bonc.activity.entity.KeyValue;
import com.bonc.activity.entity.MyForm;
import com.bonc.activity.utils.JsonUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.activiti.bpmn.converter.BpmnXMLConverter;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.editor.constants.ModelDataJsonConstants;
import org.activiti.editor.language.json.converter.BpmnJsonConverter;
import org.activiti.engine.*;
import org.activiti.engine.form.FormProperty;
import org.activiti.engine.form.StartFormData;
import org.activiti.engine.form.TaskFormData;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.history.HistoricDetail;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.impl.persistence.entity.HistoricDetailVariableInstanceUpdateEntity;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.impl.pvm.PvmTransition;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.Model;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.activiti.image.ProcessDiagramGenerator;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import sun.misc.BASE64Encoder;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * @Author Winston
 * @Date 2019/12/5 14:38
 * @Descriptions 工作流引擎控制层
 **/
@Controller
@RequestMapping
public class ActivityController {
    private static final Logger logger = LoggerFactory.getLogger(ActivityController.class);
    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private FormService formService;
    @Autowired
    private RuntimeService runtimeService;
    /**
     * 任务服务类
     */
    @Autowired
    private TaskService taskService;
    /**
     * 查询历史信息类
     */
    @Autowired
    private HistoryService historyService;
    @Autowired
    private ProcessEngineConfiguration processEngineConfiguration;

    @RequestMapping("index")
    public String toIndex(org.springframework.ui.Model model) {

        List<Model> list = repositoryService.createModelQuery().list();

        model.addAttribute("list", list);
        return "index";
    }

    /**
     * @return java.lang.String
     * @Author MRC
     * @Description //TODO 创建模型
     * @Date 11:10 2019/8/3
     * @Param []
     **/
    @RequestMapping("createModel")
    public String createModel(HttpServletRequest request, HttpServletResponse response) {

        String name = "报销流程";
        String description = "报销申请流程";

        String id = null;
        try {
            Model model = repositoryService.newModel();
            String key = name;
            //版本号
            String revision = "v1";
            ObjectNode modelNode = objectMapper.createObjectNode();
            modelNode.put(ModelDataJsonConstants.MODEL_NAME, name);
            modelNode.put(ModelDataJsonConstants.MODEL_DESCRIPTION, description);
            modelNode.put(ModelDataJsonConstants.MODEL_REVISION, revision);

            model.setName(name);
            model.setKey(key);
            //模型分类 结合自己的业务逻辑
            //model.setCategory(category);

            model.setMetaInfo(modelNode.toString());

            repositoryService.saveModel(model);
            id = model.getId();

            //完善ModelEditorSource
            ObjectNode editorNode = objectMapper.createObjectNode();
            editorNode.put("id", "canvas");
            editorNode.put("resourceId", "canvas");
            ObjectNode stencilSetNode = objectMapper.createObjectNode();
            stencilSetNode.put("namespace", "http://b3mn.org/stencilset/bpmn2.0#");
            editorNode.put("stencilset", stencilSetNode);
            repositoryService.addModelEditorSource(id, editorNode.toString().getBytes("utf-8"));

            response.sendRedirect(request.getContextPath() + "/static/modeler.html?modelId=" + id);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "index";
    }

    /**
     * 删除模型（model）
     *
     * @param modelId modelId
     * @return 删除成功后提示信息
     */
    @RequestMapping("deleteModel")
    @ResponseBody
    public String deleteModel(String modelId) {
        repositoryService.deleteModel(modelId);
        logger.info("删除流程实例完成！");
        return "Delete model is completed!";
    }

    /**
     * @return java.lang.String
     * @Author MRC
     * @Description //TODO 部署一个模型
     * @Date 15:32 2019/8/3
     * @Param [id]
     **/
    @RequestMapping("deploymentModel")
    @ResponseBody
    public JSONObject deploymentModel(String id) throws Exception {

        //获取模型
        Model modelData = repositoryService.getModel(id);
        byte[] bytes = repositoryService.getModelEditorSource(modelData.getId());

        if (bytes == null) {
            return JsonUtil.getFailJson("模型数据为空，请先设计流程并成功保存，再进行发布。");
        }
        JsonNode modelNode = new ObjectMapper().readTree(bytes);

        BpmnModel model = new BpmnJsonConverter().convertToBpmnModel(modelNode);
        if (model.getProcesses().size() == 0) {
            return JsonUtil.getFailJson("数据模型不符要求，请至少设计一条主线流程。");
        }
        byte[] bpmnBytes = new BpmnXMLConverter().convertToXML(model);

        //发布流程
        String processName = modelData.getName() + ".bpmn20.xml";
        Deployment deployment = repositoryService.createDeployment().name(modelData.getName()).addString(processName, new String(bpmnBytes, "UTF-8")).deploy();
        modelData.setDeploymentId(deployment.getId());
        repositoryService.saveModel(modelData);
        return JsonUtil.getSuccessJson("流程发布成功");
    }

    /**
     * @return java.lang.String
     * @Author MRC
     * @Description //TODO 开启流程页面
     * @Date 15:45 2019/8/3
     * @Param []
     **/
    @RequestMapping("startPage")
    public String startPage(org.springframework.ui.Model model) {
        //加载流程定义
        List<ProcessDefinition> list = repositoryService.createProcessDefinitionQuery().list();
        model.addAttribute("list", list);
        return "startPage";
    }

    /**
     * 删除部署好的流程（model）--> startPage页中的内容
     *
     * @param deploymentId deploymentId
     * @return 删除成功后提示信息
     */
    @RequestMapping("deleteDeployment")
    @ResponseBody
    public String deleteDeployment(String deploymentId) {
        //级联删除部署的流程
        repositoryService.deleteDeployment(deploymentId,true);
        logger.info("部署的流程已删除！");
        return "Delete process is completed!";
    }

    /**
     * @return java.lang.String
     * @Author MRC
     * @Description //TODO 开启流程填写表单页面
     * @Date 16:47 2019/8/3
     * @Param []
     **/
    @RequestMapping("startProcess/{id}")
    public String startProcess(@PathVariable("id") String id, org.springframework.ui.Model model) {

        //按照流程定义ID加载流程开启时候需要的表单信息
        StartFormData startFormData = formService.getStartFormData(id);
        List<FormProperty> formProperties = startFormData.getFormProperties();

        //流程定义ID
        model.addAttribute("processesId", id);
        model.addAttribute("form", formProperties);

        return "startProcess";
    }


    /**
     * @Description: 启动一个新的流程
     * @Param: [id]
     * @return: com.alibaba.fastjson.JSONObject
     * @Author: Mr.MRC
     * @Date: 2019/7/25  11:26
     */
    @RequestMapping("startProcesses")
    @ResponseBody
    public JSONObject startProcesses(@RequestParam Map<String, Object> param) {

        String processesId = (String) param.get("processesId");
        //流程提交人 这里模拟
        String userId = (String) param.get("userId");

        if (StringUtils.isEmpty(processesId)) {
            return JsonUtil.getFailJson("参数错误");
        }
        param.remove("processesId");

//        Execution last = runtimeService.createExecutionQuery().processInstanceBusinessKey(userId).processDefinitionId(processesId).singleResult();
//        if (null != last) {
//            return JsonUtil.getFailJson("请勿重复提交！");
//        }

        ProcessInstance pi = runtimeService.startProcessInstanceById(processesId, userId, param);

        if (null == pi) {
            return JsonUtil.getFailJson("流程启动失败！");
        }
        return JsonUtil.getSuccessJson("启动流程成功！");

    }


    /**
     * @return java.lang.String
     * @Author MRC
     * @Description //TODO 跳转至审批页面
     * @Date 14:13 2019/9/7
     * @Param [id] 用户id
     **/
    @RequestMapping("taskApproval/{id}")
    public String toTaskList(@PathVariable("id") String id, org.springframework.ui.Model model) {

        if (StringUtils.isNotEmpty(id)) {

            List<Task> list = taskService.createTaskQuery().taskAssignee(id).list();
            model.addAttribute("list", list);
        }

        return "taskApproval";
    }

    /**
     * @return java.lang.String
     * @Author MRC
     * @Description //TODO 任务详情
     * @Date 14:45 2019/9/7
     * @Param [id, model]
     **/
    @RequestMapping("taskDetails/{taskId}")
    public String toTaskDetails(@PathVariable("taskId") String id, org.springframework.ui.Model model) {


        Map<String, Object> map = new HashMap<>();
        //当前任务
        Task task = this.taskService.createTaskQuery().taskId(id).singleResult();
        String processInstanceId = task.getProcessInstanceId();

        TaskFormData taskFormData = this.formService.getTaskFormData(id);
        List<FormProperty> list = taskFormData.getFormProperties();
        map.put("task", task);
        map.put("list", list);
        map.put("history", assembleProcessForm(processInstanceId));

        model.addAllAttributes(map);

        return "taskDetails";
    }


    /**
     * @return com.alibaba.fastjson.JSONObject
     * @Author MRC
     * @Description //TODO 完成任务
     * @Date 15:50 2019/9/7
     * @Param []
     **/
    @RequestMapping("completeTasks")
    @ResponseBody
    public JSONObject completeTasks(@RequestParam Map<String, Object> param) {


        String taskId = (String) param.get("taskId");

        if (StringUtils.isEmpty(taskId)) {
            return JsonUtil.getFailJson("参数错误");
        }
        param.remove("taskId");

        Task task = this.taskService.createTaskQuery().taskId(taskId).singleResult();

        taskService.complete(task.getId(), param);

        return JsonUtil.getSuccessJson("流程已确认");

    }


    /**
     * @return java.util.List<com.yckj.entity.MyForm>
     * @Author MRC
     * @Description 组装表单过程的表单信息
     * @Date 10:59 2019/8/5
     * @Param [processInstanceId]
     **/
    public List<MyForm> assembleProcessForm(String processInstanceId) {

        List<HistoricActivityInstance> historys = historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstanceId).list();

        List<MyForm> myform = new ArrayList<>();

        for (HistoricActivityInstance activity : historys) {

            String actInstanceId = activity.getId();
            MyForm form = new MyForm();
            form.setActName(activity.getActivityName());
            form.setAssignee(activity.getAssignee());
            form.setProcInstId(activity.getProcessInstanceId());
            form.setTaskId(activity.getTaskId());
            //查询表单信息

            List<KeyValue> maps = new LinkedList<>();

            List<HistoricDetail> processes = historyService.createHistoricDetailQuery().activityInstanceId(actInstanceId).list();
            for (HistoricDetail process : processes) {
                HistoricDetailVariableInstanceUpdateEntity pro = (HistoricDetailVariableInstanceUpdateEntity) process;

                KeyValue keyValue = new KeyValue();

                keyValue.setKey(pro.getName());
                keyValue.setValue(pro.getTextValue());

                maps.add(keyValue);
            }
            form.setProcess(maps);

            myform.add(form);
        }

        return myform;
    }

    /**
     * @return com.alibaba.fastjson.JSONObject
     * @Author MRC
     * @Description //TODO 生成流程图
     * @Date 15:39 2019/9/7
     * @Param [processInstanceId]
     **/
    @RequestMapping("generateProcessImg")
    @ResponseBody
    public JSONObject generateProcessImg(String processInstanceId) throws IOException {

        //获取历史流程实例
        HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
        //获取流程图
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processInstance.getProcessDefinitionId());

        ProcessDiagramGenerator diagramGenerator = processEngineConfiguration.getProcessDiagramGenerator();
        ProcessDefinitionEntity definitionEntity = (ProcessDefinitionEntity) repositoryService.getProcessDefinition(processInstance.getProcessDefinitionId());

        List<HistoricActivityInstance> highLightedActivitList = historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstanceId).list();
        //高亮环节id集合
        List<String> highLightedActivitis = new ArrayList<String>();

        //高亮线路id集合
        List<String> highLightedFlows = getHighLightedFlows(definitionEntity, highLightedActivitList);

        for (HistoricActivityInstance tempActivity : highLightedActivitList) {
            String activityId = tempActivity.getActivityId();
            highLightedActivitis.add(activityId);
        }
        //配置字体
        InputStream imageStream = diagramGenerator.generateDiagram(bpmnModel, "png", highLightedActivitis, highLightedFlows, "宋体", "微软雅黑", "黑体", null, 2.0);
        BufferedImage bi = ImageIO.read(imageStream);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(bi, "png", bos);
        //转换成字节
        byte[] bytes = bos.toByteArray();
        BASE64Encoder encoder = new BASE64Encoder();
        //转换成base64串
        String png_base64 = encoder.encodeBuffer(bytes);
        //删除 \r\n
        png_base64 = png_base64.replaceAll("\n", "").replaceAll("\r", "");

        bos.close();
        imageStream.close();
        return JsonUtil.getSuccessJson("success", png_base64);
    }


    public List<String> getHighLightedFlows(ProcessDefinitionEntity processDefinitionEntity, List<HistoricActivityInstance> historicActivityInstances) {

        // 用以保存高亮的线flowId
        List<String> highFlows = new ArrayList<String>();
        // 对历史流程节点进行遍历
        for (int i = 0; i < historicActivityInstances.size() - 1; i++) {
            // 得到节点定义的详细信息
            ActivityImpl activityImpl = processDefinitionEntity.findActivity(historicActivityInstances.get(i).getActivityId());
            // 用以保存后需开始时间相同的节点
            List<ActivityImpl> sameStartTimeNodes = new ArrayList<ActivityImpl>();
            ActivityImpl sameActivityImpl1 = processDefinitionEntity.findActivity(historicActivityInstances.get(i + 1).getActivityId());
            // 将后面第一个节点放在时间相同节点的集合里
            sameStartTimeNodes.add(sameActivityImpl1);
            for (int j = i + 1; j < historicActivityInstances.size() - 1; j++) {
                // 后续第一个节点
                HistoricActivityInstance activityImpl1 = historicActivityInstances.get(j);
                // 后续第二个节点
                HistoricActivityInstance activityImpl2 = historicActivityInstances.get(j + 1);
                // 如果第一个节点和第二个节点开始时间相同保存
                if (activityImpl1.getStartTime().equals(activityImpl2.getStartTime())) {
                    ActivityImpl sameActivityImpl2 = processDefinitionEntity.findActivity(activityImpl2.getActivityId());
                    sameStartTimeNodes.add(sameActivityImpl2);
                } else {
                    // 有不相同跳出循环
                    break;
                }
            }
            // 取出节点的所有出去的线
            List<PvmTransition> pvmTransitions = activityImpl.getOutgoingTransitions();
            // 对所有的线进行遍历
            for (PvmTransition pvmTransition : pvmTransitions) {
                ActivityImpl pvmActivityImpl = (ActivityImpl) pvmTransition.getDestination();
                // 如果取出的线的目标节点存在时间相同的节点里，保存该线的id，进行高亮显示
                if (sameStartTimeNodes.contains(pvmActivityImpl)) {
                    highFlows.add(pvmTransition.getId());
                }
            }
        }
        return highFlows;
    }


}
