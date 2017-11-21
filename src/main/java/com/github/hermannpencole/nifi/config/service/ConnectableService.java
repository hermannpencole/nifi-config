package com.github.hermannpencole.nifi.config.service;

import com.github.hermannpencole.nifi.config.model.ConfigException;
import com.github.hermannpencole.nifi.swagger.client.model.ConnectableDTO;
import com.github.hermannpencole.nifi.swagger.client.model.PortDTO;
import com.github.hermannpencole.nifi.swagger.client.model.PortEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;

/**
 * Class that offer service for process group
 * <p>
 * Created by SFRJ on 01/04/2017.
 */
@Singleton
public class ConnectableService {

    /**
     * The logger.
     */
    private final static Logger LOG = LoggerFactory.getLogger(ConnectableService.class);

    public ConnectableDTO createConnectableDTOFromPort(final PortEntity port) {
        ConnectableDTO connectableDTO = new ConnectableDTO();
        connectableDTO.setGroupId(port.getComponent().getParentGroupId());
        connectableDTO.setName(port.getComponent().getName());
        connectableDTO.setId(port.getComponent().getId());
        connectableDTO.setType(matchPortTypeToConnectableType(port.getComponent().getType()));

        return connectableDTO;
    }

    private ConnectableDTO.TypeEnum matchPortTypeToConnectableType(final PortDTO.TypeEnum portType) {
        switch (portType) {
            case INPUT_PORT:
                return ConnectableDTO.TypeEnum.INPUT_PORT;
            case OUTPUT_PORT:
                return ConnectableDTO.TypeEnum.OUTPUT_PORT;
            default:
                throw new ConfigException("Cannot convert " + portType + " to ConnectableDTO");
        }
    }

}
