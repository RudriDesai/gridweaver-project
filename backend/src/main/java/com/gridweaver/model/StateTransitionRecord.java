package com.gridweaver.model;

public class StateTransitionRecord {

    private final String nodeId;
    private final String fromState;
    private final String toState;
    private final long timestamp;

    public StateTransitionRecord(String nodeId, String fromState, String toState) {
        this.nodeId = nodeId;
        this.fromState = fromState;
        this.toState = toState;
        this.timestamp = System.currentTimeMillis();
    }

    public String getNodeId()    { return nodeId; }
    public String getFromState() { return fromState; }
    public String getToState()   { return toState; }
    public long   getTimestamp() { return timestamp; }
}