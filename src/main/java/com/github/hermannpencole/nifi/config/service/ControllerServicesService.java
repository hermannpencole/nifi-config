package com.github.hermannpencole.nifi.config.service;

import com.github.hermannpencole.nifi.config.utils.FunctionUtils;
import com.github.hermannpencole.nifi.swagger.ApiException;
import com.github.hermannpencole.nifi.swagger.client.ControllerServicesApi;
import com.github.hermannpencole.nifi.swagger.client.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.github.hermannpencole.nifi.swagger.client.model.ControllerServiceReferencingComponentDTO.ReferenceTypeEnum.CONTROLLERSERVICE;
import static com.github.hermannpencole.nifi.swagger.client.model.ControllerServiceReferencingComponentDTO.ReferenceTypeEnum.PROCESSOR;

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

    @Inject
    private ProcessorService processorService;

    /**
     * disable, update and re enable the controller
     *
     * @param controllerServiceDTO component with properties to update
     * @param controllerServiceEntity controllerService with the last revision ()
     * @return
     * @throws ApiException
     * @throws InterruptedException
     */
    public ControllerServiceEntity updateControllerService(ControllerServiceDTO controllerServiceDTO, ControllerServiceEntity controllerServiceEntity, boolean forceByController) throws ApiException {
        //Disabling this controller service
       // ControllerServiceEntity controllerServiceEntityUpdate = setStateControllerService(controllerServiceEntity, ControllerServiceDTO.StateEnum.DISABLED);
        ControllerServiceEntity controllerServiceEntityUpdate = controllerServicesApi.getControllerService(controllerServiceEntity.getId());

        //update processor
        ControllerServiceEntity controllerServiceEntityConf = new ControllerServiceEntity();
        controllerServiceEntityConf.setRevision(controllerServiceEntityUpdate.getRevision());
        controllerServiceEntityConf.setComponent(controllerServiceDTO);
        controllerServiceEntityConf.getComponent().setId(controllerServiceEntity.getId());
        controllerServiceEntityConf.getComponent().setRestricted(null);
        if (! forceByController) {
            //remove controller link
            for (Map.Entry<String, PropertyDescriptorDTO> entry : controllerServiceEntityUpdate.getComponent().getDescriptors().entrySet()) {
                if (entry.getValue().getIdentifiesControllerService() != null) {
                    controllerServiceDTO.getProperties().remove(entry.getKey());
                }
            }
        }
        controllerServiceEntityUpdate = controllerServicesApi.updateControllerService(controllerServiceEntity.getId(), controllerServiceEntityConf);
        LOG.info( " {} ({}) is UPDATED", controllerServiceEntityUpdate.getComponent().getName(), controllerServiceEntityUpdate.getId());

        //Enabling this controller service
       // controllerServiceEntityUpdate = setStateControllerService(controllerServiceEntityUpdate, ControllerServiceDTO.StateEnum.ENABLED);

        return controllerServiceEntityUpdate;
    }

    /**
     * Set the state of controller service
     *
     * @param controllerServiceEntity
     * @param state
     * @return
     * @throws ApiException
     */
    public ControllerServiceEntity setStateControllerService(ControllerServiceEntity controllerServiceEntity, ControllerServiceDTO.StateEnum state) throws ApiException {

        if (state.equals(ControllerServiceDTO.StateEnum.ENABLED))
            setStateControllerServicesReference(controllerServiceEntity.getId(), ControllerServiceDTO.StateEnum.ENABLED);

        ControllerServiceEntity controllerServiceEntityUpdate = controllerServicesApi.getControllerService(controllerServiceEntity.getId());
        //Disabling this controller service
        ControllerServiceEntity controllerServiceEntityEmpty = new ControllerServiceEntity();
        controllerServiceEntityEmpty.setRevision(controllerServiceEntityUpdate.getRevision());
        controllerServiceEntityEmpty.setComponent(new ControllerServiceDTO());
        controllerServiceEntityEmpty.getComponent().setId(controllerServiceEntity.getId());
        controllerServiceEntityEmpty.getComponent().setState(state);
        controllerServiceEntityEmpty.getComponent().setProperties(null);
        controllerServiceEntityEmpty.getComponent().setDescriptors(null);
        controllerServiceEntityEmpty.getComponent().setReferencingComponents(null);
        controllerServiceEntityEmpty.getComponent().setValidationErrors(null);
        controllerServiceEntityEmpty.getComponent().setPersistsState(null);
        controllerServiceEntityEmpty.getComponent().setRestricted(null);
        LOG.info(" {} ({}) update for {}" , controllerServiceEntity.getComponent().getName(), controllerServiceEntity.getId(), state);
        controllerServiceEntityUpdate = controllerServicesApi.updateControllerService(controllerServiceEntity.getId(), controllerServiceEntityEmpty);
        //Wait disabled
        FunctionUtils.runWhile(()-> {
            LOG.info(" {} ({}) waiting for {}" , controllerServiceEntity.getComponent().getName(), controllerServiceEntity.getId(), state);
            ControllerServiceEntity controllerService = controllerServicesApi.getControllerService(controllerServiceEntity.getId());
            LOG.info(" {} ({}) is {}" , controllerService.getComponent().getName(), controllerService.getId(), controllerService.getComponent().getState());
            return !controllerService.getComponent().getState().equals(state);
        }, interval, timeout);
        return controllerServiceEntityUpdate;
    }

    public ControllerServiceEntity getControllerServices(String id) throws ApiException {
        return controllerServicesApi.getControllerService(id);
    }

    public void setStateReferencingControllerServices(String id, UpdateControllerServiceReferenceRequestEntity.StateEnum state) throws ApiException {
        //Get fresh references
        Map<String, RevisionDTO> referencingControllerServices = getReferencingServices(id, CONTROLLERSERVICE, state.toString());
        if (referencingControllerServices.isEmpty()) return;
        FunctionUtils.runWhile(()-> {
            ControllerServiceReferencingComponentsEntity controllerServiceReferencingComponentsEntity = null;
            try {
                UpdateControllerServiceReferenceRequestEntity updateControllerServiceReferenceRequestEntity = new UpdateControllerServiceReferenceRequestEntity();
                updateControllerServiceReferenceRequestEntity.setId(id);
                updateControllerServiceReferenceRequestEntity.setState(state);
                updateControllerServiceReferenceRequestEntity.setReferencingComponentRevisions(referencingControllerServices);
                controllerServiceReferencingComponentsEntity = controllerServicesApi.updateControllerServiceReferences(id, updateControllerServiceReferenceRequestEntity);
            } catch (ApiException e) {
                LOG.info(e.getResponseBody());
                if (e.getResponseBody() == null || !e.getResponseBody().endsWith("Current state is STOPPING")){
                    throw e;
                }
            }
            return controllerServiceReferencingComponentsEntity == null;
        }, interval, timeout);
    }


    public void setStateControllerServicesReference(String id, ControllerServiceDTO.StateEnum state) throws ApiException {
        //Get fresh references
        ControllerServiceEntity controllerServiceEntity = controllerServicesApi.getControllerService(id);

        //remove controller link
        for (Map.Entry<String, PropertyDescriptorDTO> entry : controllerServiceEntity.getComponent().getDescriptors().entrySet()) {
            if (entry.getValue().getIdentifiesControllerService() != null) {
                String idController = controllerServiceEntity.getComponent().getProperties().get(entry.getKey());
                if(idController != null)
                    setStateControllerService(controllerServicesApi.getControllerService(idController), state);
            }
        }
    }

    public void setStateReferenceProcessors(ControllerServiceEntity controllerServiceEntityFind, UpdateControllerServiceReferenceRequestEntity.StateEnum state) throws ApiException {
        //how obtain state of controllerServiceReference and don't have this bullshit trick

        //Get fresh references
        Map<String, RevisionDTO> referencingProcessorsServices = getReferencingServices(controllerServiceEntityFind.getId(), PROCESSOR, state.toString());
        if (referencingProcessorsServices.isEmpty()) return;
        FunctionUtils.runWhile(()-> {
            ControllerServiceEntity controllerServiceEntity = null;
            try {
                UpdateControllerServiceReferenceRequestEntity updateControllerServiceReferenceRequestEntity = new UpdateControllerServiceReferenceRequestEntity();
                updateControllerServiceReferenceRequestEntity.setId(controllerServiceEntityFind.getId());
                updateControllerServiceReferenceRequestEntity.setReferencingComponentRevisions(referencingProcessorsServices);
                updateControllerServiceReferenceRequestEntity.setState(state);
                controllerServicesApi.updateControllerServiceReferences(controllerServiceEntityFind.getId(), updateControllerServiceReferenceRequestEntity);
                controllerServiceEntity = controllerServicesApi.getControllerService(controllerServiceEntityFind.getId());
            } catch (ApiException e) {
                LOG.info(e.getResponseBody());
                if (e.getResponseBody() == null || !e.getResponseBody().endsWith("Current state is STOPPING")){
                    throw e;
                }
            }
            return (controllerServiceEntity == null);
        }, interval, timeout);

        //be sure stop/start ALL processor
        for (String idProcessor : getReferencingServices(controllerServiceEntityFind.getId(), PROCESSOR, "ALL").keySet() ) {
            ProcessorEntity processorEntity = processorService.getById(idProcessor);
            if (state.equals(UpdateControllerServiceReferenceRequestEntity.StateEnum.STOPPED))
                processorService.setState(processorEntity, ProcessorDTO.StateEnum.STOPPED);
            else
                processorService.setState(processorEntity, ProcessorDTO.StateEnum.RUNNING);
        }
    }

    public Map<String, RevisionDTO> getReferencingServices(String id, ControllerServiceReferencingComponentDTO.ReferenceTypeEnum type, String filteredState) throws ApiException {
        ControllerServiceEntity controllerServiceEntityFresh = getControllerServices(id);
        List<ControllerServiceReferencingComponentEntity> referencingComponentEntities = controllerServiceEntityFresh.getComponent().getReferencingComponents();
        Map<String, RevisionDTO> referencingControllerServices = referencingComponentEntities.stream()
                .filter(item -> item.getComponent().getReferenceType() == type)
                .filter(item -> !item.getComponent().getState().equals(filteredState))
                .collect(Collectors.toMap(item -> item.getId(), item -> item.getRevision()));
        return referencingControllerServices;
    }

    public void remove(ControllerServiceEntity controllerServiceToRemove) throws ApiException {
        //Disabling this controller service
        ControllerServiceEntity controllerServiceEntityUpdate = setStateControllerService(controllerServiceToRemove, ControllerServiceDTO.StateEnum.DISABLED);
        LOG.info(" {} ({}) trying removing" , controllerServiceEntityUpdate.getComponent().getName(), controllerServiceEntityUpdate.getId());
        //how obtain state of controllerServiceReference and don't have this bullshit trick
        FunctionUtils.runWhile(()-> {
            ControllerServiceEntity controllerServiceEntity = null;
            try {
                controllerServiceEntity = controllerServicesApi.removeControllerService(controllerServiceEntityUpdate.getId(), controllerServiceEntityUpdate.getRevision().getVersion().toString(), controllerServiceEntityUpdate.getRevision().getClientId());
                LOG.info(" {} ({}) is removed" , controllerServiceEntity.getComponent().getName(), controllerServiceEntity.getId());
            } catch (ApiException e) {
                LOG.info(e.getResponseBody());
                if (e.getResponseBody() == null || !e.getResponseBody().endsWith("Current state is STOPPING")){
                    throw e;
                }
            }
            return controllerServiceEntity == null;
        }, interval, timeout);    }

}
