package com.github.hermannpencole.nifi.config.model;

import com.github.hermannpencole.nifi.swagger.client.model.ControllerServiceDTO;
import com.github.hermannpencole.nifi.swagger.client.model.ProcessorDTO;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by SFRJ2737 on 2017-05-26.
 */
public class GroupProcessorsEntity {

    @SerializedName("processors")
    private List<ProcessorDTO> processors = new ArrayList<>();

    @SerializedName("groupProcessorsEntity")
    private List<GroupProcessorsEntity> groupProcessorsEntity = new ArrayList<>();

    @SerializedName("controllerServices")
    private List<ControllerServiceDTO> controllerServicesDTO = new ArrayList<>();

    @SerializedName("name")
    private String name;


    public List<ProcessorDTO> getProcessors() {
        return processors;
    }

    public void setProcessors(List<ProcessorDTO> processors) {
        this.processors = processors;
    }

    public List<GroupProcessorsEntity> getGroupProcessorsEntity() {
        return groupProcessorsEntity;
    }

    /**
     *
     * @param groupProcessorsEntity
     */
    public void setGroupProcessorsEntity(List<GroupProcessorsEntity> groupProcessorsEntity) {
        this.groupProcessorsEntity = groupProcessorsEntity;
    }

    public List<ControllerServiceDTO> getControllerServicesDTO() {
        return controllerServicesDTO;
    }

    public void setControllerServicesDTO(List<ControllerServiceDTO> controllerServicesDTO) {
        this.controllerServicesDTO = controllerServicesDTO;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


}
