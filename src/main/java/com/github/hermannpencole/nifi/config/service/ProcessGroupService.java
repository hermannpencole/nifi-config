package com.github.hermannpencole.nifi.config.service;

import com.github.hermannpencole.nifi.swagger.ApiException;
import com.github.hermannpencole.nifi.swagger.client.FlowApi;
import com.github.hermannpencole.nifi.swagger.client.ProcessGroupsApi;
import com.github.hermannpencole.nifi.swagger.client.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Class that offer service for process group
 * <p>
 * Created by SFRJ on 01/04/2017.
 */
@Singleton
public class ProcessGroupService {


    /**
     * The logger.
     */
    private final static Logger LOG = LoggerFactory.getLogger(ProcessGroupService.class);

    @Inject
    private FlowApi flowapi;

    @Inject
    private ProcessGroupsApi processGroupsApi;

    /**
     * browse nifi on branch pass in parameter
     *
     * @param branch
     * @return
     * @throws ApiException
     */
    public Optional<ProcessGroupFlowEntity> changeDirectory(List<String> branch) throws ApiException {
        ProcessGroupFlowEntity flowEntity = flowapi.getFlow("root");
        for (String processGroupName : branch.subList(1, branch.size())) {
            Optional<ProcessGroupEntity> flowEntityChild = findByComponentName(flowEntity.getProcessGroupFlow().getFlow().getProcessGroups(), processGroupName);
            if (!flowEntityChild.isPresent()) {
                return Optional.empty();
            }
            flowEntity = flowapi.getFlow(flowEntityChild.get().getId());
        }
        return Optional.of(flowEntity);
    }

    //can static => utils
    public static Optional<ProcessGroupEntity> findByComponentName(List<ProcessGroupEntity> listGroup, String name) {
        return listGroup.stream()
                .filter(item -> item.getComponent().getName().trim().equals(name.trim()))
                .findFirst();
    }


    public void setState(String id, ScheduleComponentsEntity.StateEnum state) throws ApiException {
        ScheduleComponentsEntity body = new ScheduleComponentsEntity();
        body.setId(id);
        body.setState(state);
        body.setComponents(null);//for all
        flowapi.scheduleComponents(id, body);
    }

    /**
     * browse nifi on branch pass in parameter
     *
     * @param branch
     * @return
     * @throws ApiException
     */
    public ProcessGroupFlowEntity createDirectory(List<String> branch) throws ApiException {
        //generate clientID
        String clientId = flowapi.generateClientId();
        //find root
        ProcessGroupFlowEntity flowEntity = flowapi.getFlow("root");
        for (String processGroupName : branch.subList(1, branch.size())) {
            Optional<ProcessGroupEntity> flowEntityChild = findByComponentName(flowEntity.getProcessGroupFlow().getFlow().getProcessGroups(), processGroupName);
            if (!flowEntityChild.isPresent()) {
                PositionDTO position = getNextPosition(flowEntity);
                ProcessGroupEntity created = new ProcessGroupEntity();
                created.setRevision(new RevisionDTO());
                created.setComponent(new ProcessGroupDTO());
                created.getRevision().setVersion(0L);
                created.getRevision().setClientId(clientId);
                created.getComponent().setName(processGroupName);
                created.getComponent().setPosition(position);
                created = processGroupsApi.createProcessGroup(flowEntity.getProcessGroupFlow().getId(), created);
                flowEntity = flowapi.getFlow(created.getId());
            } else {
                flowEntity = flowapi.getFlow(flowEntityChild.get().getId());
            }
        }
        return flowEntity;
    }

    public PositionDTO getNextPosition(ProcessGroupFlowEntity flowEntity) {
        PositionDTO nextPosition = new PositionDTO();
        List<PositionDTO> positions = new ArrayList<>();
        for (ProcessorEntity processor : flowEntity.getProcessGroupFlow().getFlow().getProcessors()) {
            positions.add(processor.getPosition());
        }
        for (ProcessGroupEntity processGroup : flowEntity.getProcessGroupFlow().getFlow().getProcessGroups()) {
            positions.add(processGroup.getPosition());
        }

        nextPosition.setX(0d);
        nextPosition.setY(0d);
        while (positions.indexOf(nextPosition) != -1) {
            if(nextPosition.getX() == 800d) {
                nextPosition.setX(0d);
                nextPosition.setY(nextPosition.getY() + 200);
            } else {
                nextPosition.setX(nextPosition.getX() + 400);
            }
        }
        LOG.debug("nest postion {},{}", nextPosition.getX(), nextPosition.getY());
        return nextPosition;
    }
}
