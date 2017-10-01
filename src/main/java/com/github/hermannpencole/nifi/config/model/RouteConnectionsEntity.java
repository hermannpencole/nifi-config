package com.github.hermannpencole.nifi.config.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class RouteConnectionsEntity {

    @SerializedName("connections")
    private List<RouteConnectionEntity> connections = new ArrayList<>();

    public List<RouteConnectionEntity> getConnections() {
        return connections;
    }

    public void setConnections(List<RouteConnectionEntity> connections) {
        this.connections = connections;
    }
}
