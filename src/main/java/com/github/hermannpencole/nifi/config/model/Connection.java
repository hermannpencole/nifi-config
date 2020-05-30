package com.github.hermannpencole.nifi.config.model;

import java.util.List;
import java.util.Objects;

public class Connection {

    private String id;

    private String name;

    private String source;

    private String destination;

    private String backPressureDataSizeThreshold;

    private String flowFileExpiration;

    private List<String> relationShips;

    private Long backPressureObjectThreshold;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getBackPressureDataSizeThreshold() {
        return backPressureDataSizeThreshold;
    }

    public void setBackPressureDataSizeThreshold(String backPressureDataSizeThreshold) {
        this.backPressureDataSizeThreshold = backPressureDataSizeThreshold;
    }

    public Long getBackPressureObjectThreshold() {
        return backPressureObjectThreshold;
    }

    public void setBackPressureObjectThreshold(Long backPressureObjectThreshold) {
        this.backPressureObjectThreshold = backPressureObjectThreshold;
    }

    public String getFlowFileExpiration() {
        return flowFileExpiration;
    }

    public void setFlowFileExpiration(String flowFileExpiration) {
        this.flowFileExpiration = flowFileExpiration;
    }

    public List<String> getRelationShips() {
        return relationShips;
    }

    public void setRelationShips(List<String> relationShips) {
        this.relationShips = relationShips;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Connection that = (Connection) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(source, that.source) &&
                Objects.equals(destination, that.destination) &&
                Objects.equals(backPressureDataSizeThreshold, that.backPressureDataSizeThreshold) &&
                Objects.equals(backPressureObjectThreshold, that.backPressureObjectThreshold);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, source, destination, backPressureDataSizeThreshold, backPressureObjectThreshold);
    }
}
