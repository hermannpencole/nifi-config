package com.github.hermannpencole.nifi.config.model;

import com.google.gson.annotations.SerializedName;

public class ConnectionPort {

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

    public void setSource(String source) {
        this.source = source;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

}
