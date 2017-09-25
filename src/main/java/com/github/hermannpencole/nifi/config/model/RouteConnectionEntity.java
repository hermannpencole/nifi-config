package com.github.hermannpencole.nifi.config.model;

import com.google.gson.annotations.SerializedName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RouteConnectionEntity {

    /**
     * The logger.
     */
    private final static Logger LOG = LoggerFactory.getLogger(RouteConnectionEntity.class);

    @SerializedName("name")
    private String name;

    @SerializedName("source")
    private String source;

    @SerializedName("destination")
    private String destination;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSource() {
        return source;
    }

    public List<String> getSourceList() {
        return validateBranch(splitBranch(source));
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDestination() {
        return destination;
    }

    public List<String> getDestinationList() {
        return validateBranch(splitBranch(destination));
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    private List<String> splitBranch(String branch) {
        LOG.info("Parsing branch '{}' for connection named '{}'", branch, name);
        return Arrays.stream(branch.split(">")).map(String::trim).collect(Collectors.toList());
    }

    private List<String> validateBranch(List<String> branch) {
        if (!branch.get(0).equals("root")) {
            throw new ConfigException("The branch address must begin with the element 'root' ( sample : root > branch > sub-branch)");
        }
        return branch;
    }
}
