package com.bonc.activity.entity;

import java.util.List;

/**
 * Copyright (C), 2015-2019
 * FileName: MyForm
 * Author:   MRC
 * Date:     2019/9/7 14:57
 * Description:
 * History:
 */
public class MyForm {


    //任务名称
    private String actName;

    //派遣人
    private String assignee;


    //流程实例ID
    private String procInstId;


    //任务ID
    private String taskId;

    //表单属性
    private List<KeyValue> process;


    public String getActName() {
        return actName;
    }

    public void setActName(String actName) {
        this.actName = actName;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public String getProcInstId() {
        return procInstId;
    }

    public void setProcInstId(String procInstId) {
        this.procInstId = procInstId;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public List<KeyValue> getProcess() {
        return process;
    }

    public void setProcess(List<KeyValue> process) {
        this.process = process;
    }
}