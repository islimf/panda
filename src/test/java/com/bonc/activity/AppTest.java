package com.bonc.activity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.activiti.editor.constants.ModelDataJsonConstants;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.TaskService;
import org.activiti.engine.impl.RepositoryServiceImpl;
import org.activiti.engine.impl.TaskServiceImpl;
import org.activiti.engine.repository.Model;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * Unit test for simple App.
 */
public class AppTest {


    /**
     * Rigorous Test :-)
     */
    @Test
    public void shouldAnswerWithTrue() {
        assertTrue(true);
    }

    // 创建模型
    @Test
    public void createModel(){

        RepositoryService repositoryService = new RepositoryServiceImpl();
        ObjectMapper objectMapper = new ObjectMapper();
        Model modelData = repositoryService.getModel("leave");
        String name = "请假流程";
        String description = "请假申请流程";
        String id = null;

        try{
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

        }catch (Exception e){

        }

    }

    @Test
    public void test2(){
        TaskService taskService = new TaskServiceImpl();
        List list = taskService.getSubTasks("");
        System.out.println(list);
    }
}
