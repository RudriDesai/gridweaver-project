package com.gridweaver.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a single IoT node in the energy microgrid.
 * A node can be a solar panel, home battery, or grid relay point.
 *
 * States follow the Spring State Machine definition (Week 2):
 * CHARGING → IDLE → DISCHARGING → FAULT
 */
public class GridNode {

    @JsonProperty("nodeId")
    private String nodeId;

    @JsonProperty("latitude")
    private double latitude;

    @JsonProperty("longitude")
    private double longitude;
    /**
     * Current operational state.
     * Valid values: CHARGING, DISCHARGING, IDLE, FAULT
     * State transitions are managed by Spring State Machine (Week 2)
     */
    @JsonProperty("status")
    private String status;

    @JsonProperty("powerOutput")
    private double powerOutput; // kilowatts

    @JsonProperty("gridLoad")
    private double gridLoad; // percentage 0.0 to 100.0

    @JsonProperty("timestamp")
    private long timestamp; // epoch millis — when last updated

    // ── Constructor ───────────────────────────────────

    public GridNode(String nodeId,
                    double latitude,
                    double longitude,
                    String status,
                    double powerOutput,
                    double gridLoad) {
        this.nodeId      = nodeId;
        this.latitude    = latitude;
        this.longitude   = longitude;
        this.status      = status;
        this.powerOutput = powerOutput;
        this.gridLoad    = gridLoad;
        this.timestamp   = System.currentTimeMillis();
    }

    // ── Getters ───────────────────────────────────────

    public String getNodeId()      { return nodeId;      }
    public double getLatitude()    { return latitude;    }
    public double getLongitude()   { return longitude;   }
    public String getStatus()      { return status;      }
    public double getPowerOutput() { return powerOutput; }
    public double getGridLoad()    { return gridLoad;    }
    public long   getTimestamp()   { return timestamp;   }

    // ── Setters (only mutable fields) ─────────────────
    // nodeId, lat, lng are fixed after creation

    public void setStatus(String status) {
        this.status = status;
    }

    public void setPowerOutput(double powerOutput) {
        this.powerOutput = powerOutput;
    }

    public void setGridLoad(double gridLoad) {
        this.gridLoad = gridLoad;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "GridNode{" +
                "nodeId='" + nodeId + '\'' +
                ", status='" + status + '\'' +
                ", powerOutput=" + powerOutput +
                ", gridLoad=" + gridLoad +
                '}';
    }
}