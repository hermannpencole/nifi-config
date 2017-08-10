package com.github.hermannpencole.nifi.config.service;

import com.github.hermannpencole.nifi.config.utils.FunctionUtils;
import com.github.hermannpencole.nifi.swagger.ApiException;
import com.github.hermannpencole.nifi.swagger.client.ControllerServicesApi;
import com.github.hermannpencole.nifi.swagger.client.ProcessorsApi;
import com.github.hermannpencole.nifi.swagger.client.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Class that offer service for nifi processor
 * <p>
 * Created by SFRJ on 01/04/2017.
 */
@Singleton
public class ControllerServicesService {

    /**
     * The logger.
     */
    private final static Logger LOG = LoggerFactory.getLogger(ControllerServicesService.class);

    @Named("timeout")
    @Inject
    public Integer timeout;

    @Named("interval")
    @Inject
    public Integer interval;

    @Inject
    private ControllerServicesApi controllerServicesApi;

    /**
     * disable, update and re enable the controller
     *
     * @param controllerServiceDTO
     * @param controllerServiceEntity
     * @return
     * @throws ApiException
     * @throws InterruptedException
     */
    public ControllerServiceEntity updateControllerService(ControllerServiceDTO controllerServiceDTO, ControllerServiceEntity controllerServiceEntity) throws ApiException, InterruptedException {
        //TODO timeout
        //Disabling this controller service
        ControllerServiceEntity controllerServiceEntityEmpty = new ControllerServiceEntity();
        controllerServiceEntityEmpty.setRevision(controllerServiceEntity.getRevision());
        controllerServiceEntityEmpty.setComponent(new ControllerServiceDTO());
        controllerServiceEntityEmpty.getComponent().setId(controllerServiceEntity.getId());
        controllerServiceEntityEmpty.getComponent().setState(ControllerServiceDTO.StateEnum.DISABLED);
        controllerServiceEntityEmpty.getComponent().setProperties(null);
        controllerServiceEntityEmpty.getComponent().setDescriptors(null);
        controllerServiceEntityEmpty.getComponent().setReferencingComponents(null);
        controllerServiceEntityEmpty.getComponent().setValidationErrors(null);
        controllerServiceEntityEmpty.getComponent().setPersistsState(null);
        controllerServiceEntityEmpty.getComponent().setRestricted(null);
        ControllerServiceEntity controllerServiceEntityUpdate = controllerServicesApi.updateControllerService(controllerServiceEntity.getId(), controllerServiceEntityEmpty);

        //Wait disabled
        FunctionUtils.runWhile(()-> {
            ControllerServiceEntity controllerService = controllerServicesApi.getControllerService(controllerServiceEntity.getId());
            LOG.info(controllerService.getId() + " is " + controllerService.getComponent().getState());
            return !controllerService.getComponent().getState().equals(ControllerServiceDTO.StateEnum.DISABLED);
        }, interval, timeout);

        //update processor
        ControllerServiceEntity controllerServiceEntityConf = new ControllerServiceEntity();
        controllerServiceEntityConf.setRevision(controllerServiceEntityUpdate.getRevision());
        controllerServiceEntityConf.setComponent(controllerServiceDTO);
        controllerServiceEntityConf.getComponent().setId(controllerServiceEntity.getId());
        controllerServiceEntityUpdate = controllerServicesApi.updateControllerService(controllerServiceEntityUpdate.getId(), controllerServiceEntityConf);
        LOG.info(controllerServiceEntityUpdate.getId() + " is UPDATED");

        //Disabling this controller service
        controllerServiceEntityEmpty.setRevision(controllerServiceEntityUpdate.getRevision());
        controllerServiceEntityEmpty.getComponent().setState(ControllerServiceDTO.StateEnum.ENABLED);
        controllerServiceEntityUpdate = controllerServicesApi.updateControllerService(controllerServiceEntity.getId(), controllerServiceEntityEmpty);

        //Wait enabled
        FunctionUtils.runWhile(()-> {
            ControllerServiceEntity controllerService = controllerServicesApi.getControllerService(controllerServiceEntity.getId());
            LOG.info(controllerService.getId() + " is " + controllerService.getComponent().getState());
            return !controllerService.getComponent().getState().equals(ControllerServiceDTO.StateEnum.ENABLED);
        }, interval, timeout);

        return controllerServiceEntityUpdate;
    }

    public void setStateReferencingControllerServices(String id, UpdateControllerServiceReferenceRequestEntity.StateEnum state) throws ApiException {
        UpdateControllerServiceReferenceRequestEntity updateControllerServiceReferenceRequestEntity;
        ControllerServiceReferencingComponentsEntity controllerServiceReferencingComponentsEntity;
        ControllerServiceEntity controllerServiceEntity;
        updateControllerServiceReferenceRequestEntity = new UpdateControllerServiceReferenceRequestEntity();
        updateControllerServiceReferenceRequestEntity.setId(id);
        updateControllerServiceReferenceRequestEntity.setState(state);
        controllerServiceReferencingComponentsEntity = controllerServicesApi.updateControllerServiceReferences(id, updateControllerServiceReferenceRequestEntity);
        controllerServiceEntity = controllerServicesApi.getControllerService(id);
    }

    public void setStateReferenceProcessors(ControllerServiceEntity controllerServiceEntityFind, UpdateControllerServiceReferenceRequestEntity.StateEnum state) throws ApiException {
        //how obtain state of controllerServiceReference and don't have this bullshit trick
        //TODO timeout
        FunctionUtils.runWhile(()-> {
            ControllerServiceEntity controllerServiceEntity = null;
            try {
                UpdateControllerServiceReferenceRequestEntity updateControllerServiceReferenceRequestEntity = new UpdateControllerServiceReferenceRequestEntity();
                updateControllerServiceReferenceRequestEntity.setId(controllerServiceEntityFind.getId());
                for (ControllerServiceReferencingComponentEntity controllerServiceReferencingComponentEntity : controllerServiceEntityFind.getComponent().getReferencingComponents()) {
                    updateControllerServiceReferenceRequestEntity.getReferencingComponentRevisions().put(controllerServiceReferencingComponentEntity.getId(), controllerServiceReferencingComponentEntity.getRevision());
                }
                updateControllerServiceReferenceRequestEntity.setState(state);
                ControllerServiceReferencingComponentsEntity controllerServiceReferencingComponentsEntity = controllerServicesApi.updateControllerServiceReferences(controllerServiceEntityFind.getId(), updateControllerServiceReferenceRequestEntity);
                controllerServiceEntity = controllerServicesApi.getControllerService(controllerServiceEntityFind.getId());
            } catch (ApiException e) {
                LOG.info(e.getResponseBody());
                if (!e.getResponseBody().endsWith("Current state is STOPPING")){
                    throw e;
                }
            }
            return (controllerServiceEntity == null);
        }, interval, timeout);

    }

}
