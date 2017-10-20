package com.github.hermannpencole.nifi.config.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class RouteConnectionsEntity {

    @SerializedName("connections")
    private List<ConnectionPort> connections = new ArrayList<>();

    public List<ConnectionPort> getConnections() {
        return connections;
    }

    public void setConnections(List<ConnectionPort> connections) {
        this.connections = connections;
    }
}
