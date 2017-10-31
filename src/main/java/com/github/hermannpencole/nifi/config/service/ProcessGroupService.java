package com.github.hermannpencole.nifi.config.service;

import com.github.hermannpencole.nifi.swagger.ApiException;
import com.github.hermannpencole.nifi.swagger.client.FlowApi;
import com.github.hermannpencole.nifi.swagger.client.ProcessGroupsApi;
import com.github.hermannpencole.nifi.swagger.client.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.hermannpencole.nifi.config.utils.FunctionUtils.findByComponentName;

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

	@Inject
	private ProcessorService processorService;

	@Inject
	private PortService portService;

	@Inject
	private ConnectionService connectionService;

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
			Optional<ProcessGroupEntity> flowEntityChild = findByComponentName(
					flowEntity.getProcessGroupFlow().getFlow().getProcessGroups(), processGroupName);
			if (!flowEntityChild.isPresent()) {
				return Optional.empty();
			}
			flowEntity = flowapi.getFlow(flowEntityChild.get().getId());
		}
		return Optional.of(flowEntity);
	}

	/**
	 * browse nifi on branch pass in parameter
	 *
	 * @param branch
	 * @return
	 * @throws ApiException
	 */
	public ProcessGroupFlowEntity createDirectory(List<String> branch) throws ApiException {
		// generate clientID
		String clientId = flowapi.generateClientId();
		// find root
		ProcessGroupFlowEntity flowEntity = flowapi.getFlow("root");
		for (String processGroupName : branch.subList(1, branch.size())) {
			Optional<ProcessGroupEntity> flowEntityChild = findByComponentName(
					flowEntity.getProcessGroupFlow().getFlow().getProcessGroups(), processGroupName);
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

	/**
	 * set state on entire process group (no report error if there is)
	 *
	 * @param id
	 * @param state
	 * @throws ApiException
	 */
	public void setState(String id, ScheduleComponentsEntity.StateEnum state) throws ApiException {
		ScheduleComponentsEntity body = new ScheduleComponentsEntity();
		body.setId(id);
		body.setState(state);
		body.setComponents(null);// for all
		flowapi.scheduleComponents(id, body);
	}

	/**
	 * start the processor group. Begin by processor that consumme flow and end with
	 * processor that consumme stream and create flow
	 *
	 * @param processGroupFlow
	 * @throws ApiException
	 */
	public void start(ProcessGroupFlowEntity processGroupFlow) throws ApiException {
		try {
			List<Set<?>> listing = reorder(processGroupFlow.getProcessGroupFlow());
			for (int i = (listing.size() - 1); i >= 0; i--) {
				Set<?> set = listing.get(i);
				for (Object object : set) {
					if (object instanceof ProcessorEntity) {
						processorService.setState((ProcessorEntity) object, ProcessorDTO.StateEnum.RUNNING);
					} else if (object instanceof PortEntity) {
						portService.setState((PortEntity) object, PortDTO.StateEnum.STOPPED);
					}
				}
			}
			for (ProcessGroupEntity procGroupInConf : processGroupFlow.getProcessGroupFlow().getFlow()
					.getProcessGroups()) {
				ProcessGroupFlowEntity processGroupFlowEntity = flowapi.getFlow(procGroupInConf.getId());
				start(processGroupFlowEntity);
			}
		} catch (Exception e) {
			setState(processGroupFlow.getProcessGroupFlow().getId(), ScheduleComponentsEntity.StateEnum.STOPPED);
			throw e;
		}
	}

	/**
	 * stop the processor group. Begin by processor that consumme stream and create
	 * flow and end with processor that consumme flow.
	 *
	 * @param processGroupFlow
	 * @throws ApiException
	 */
	public void stop(ProcessGroupFlowEntity processGroupFlow) throws ApiException {
		try {
			List<Set<?>> listing = reorder(processGroupFlow.getProcessGroupFlow());
			for (int i = 0; i < (listing.size()); i++) {
				Set<?> set = listing.get(i);

				if (set.size() > 0 && set.stream().findFirst().get() instanceof ConnectionEntity) {
					// make be sur that in one pass all queue are empty (for the case when there is
					// cycle)
					boolean emptyQueue = true;
					do {
						emptyQueue = true;
						for (Object o : set) {
							emptyQueue = emptyQueue && connectionService.isEmptyQueue((ConnectionEntity) o);
						}
						if (!emptyQueue) {
							for (Object o : set) {
								connectionService.waitEmptyQueue((ConnectionEntity) o);
							}
						}
					} while (!emptyQueue);
				}
				// TODO manage remoteProcessGroup
				for (Object object : set) {
					if (object instanceof ProcessorEntity) {
						processorService.setState((ProcessorEntity) object, ProcessorDTO.StateEnum.STOPPED);
					} else if (object instanceof PortEntity) {
						portService.setState((PortEntity) object, PortDTO.StateEnum.STOPPED);
					}
				}
			}
			for (ProcessGroupEntity procGroupInConf : processGroupFlow.getProcessGroupFlow().getFlow()
					.getProcessGroups()) {
				ProcessGroupFlowEntity processGroupFlowEntity = flowapi.getFlow(procGroupInConf.getId());
				start(processGroupFlowEntity);
			}
		} catch (Exception e) {
			setState(processGroupFlow.getProcessGroupFlow().getId(), ScheduleComponentsEntity.StateEnum.RUNNING);
			throw e;
		}
	}

	/**
	 * reorder for have the processor that consume stream -> connection -> processor
	 * connected etc ...in the good order.
	 *
	 * Just put the first at the first and the other after trick for bypass cycle
	 *
	 * @param processGroupFlow
	 * @return
	 */
	public List<Set<?>> reorder(ProcessGroupFlowDTO processGroupFlow) {
		List<Set<?>> level = new ArrayList<>();

		Set<ProcessGroupFlowDTO> allProcessGroupFlow = getAllProcessGroupFlow(processGroupFlow);
		Set<ConnectionEntity> allConnections = allProcessGroupFlow.stream()
				.flatMap(p -> p.getFlow().getConnections().stream()).collect(Collectors.toSet());

		// get the first
		Set<String> destination = new HashSet<>();
		Set<String> source = new HashSet<>();
		allConnections.forEach(connection -> {
			destination.add(connection.getDestinationId());
			source.add(connection.getSourceId());
		});

		// get the first (the first have no destination)
		Set<String> first = new HashSet<>(source);
		first.removeAll(destination);
		level.add(first.stream().map(id -> findById(allProcessGroupFlow, id)).filter(Optional::isPresent)
				.map(Optional::get).collect(Collectors.toSet()));

		// get the other (the other have destination)
		level.add(allConnections);
		level.add(destination.stream().map(id -> findById(allProcessGroupFlow, id)).filter(Optional::isPresent)
				.map(Optional::get).collect(Collectors.toSet()));

		return level;
	}

	private Set<ProcessGroupFlowDTO> getAllProcessGroupFlow(ProcessGroupFlowDTO processGroupFlow) {
		Set<ProcessGroupFlowDTO> result = new HashSet<>();
		result.add(processGroupFlow);
		for (ProcessGroupEntity processGroup : processGroupFlow.getFlow().getProcessGroups()) {
			result.add(flowapi.getFlow(processGroup.getId()).getProcessGroupFlow());
		}
		return result;
	}

	public Optional<?> findById(Set<ProcessGroupFlowDTO> allProcessGroupFlow, String id) {
		for (ProcessGroupFlowDTO processGroupFlowDTO : allProcessGroupFlow) {
			Optional<?> result = findById(processGroupFlowDTO.getFlow(), id);
			if (result.isPresent()) {
				return result;
			}
		}
		return Optional.empty();
	}

	public Optional<?> findById(FlowDTO flow, String id) {
		Optional<?> result = flow.getProcessors().stream().filter(processor -> id.equals(processor.getId()))
				.findFirst();

		if (!result.isPresent())
			result = flow.getInputPorts().stream().filter(port -> id.equals(port.getId())).findFirst();
		if (!result.isPresent())
			result = flow.getOutputPorts().stream().filter(port -> id.equals(port.getId())).findFirst();
		if (!result.isPresent())
			result = flow.getFunnels().stream().filter(funnel -> id.equals(funnel.getId())).findFirst();
		if (!result.isPresent())
			result = flow.getRemoteProcessGroups().stream()
					.filter(remoteProcessGroup -> id.equals(remoteProcessGroup.getId())).findFirst();

		return result;
	}

	/**
	 * get the next free position to place the processor(or group processor) on this
	 * group processor
	 *
	 * @param flowEntity
	 * @return
	 */
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
			if (nextPosition.getX() == 800d) {
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
