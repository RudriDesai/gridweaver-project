package com.gridweaver.simulator;

public class SimulatorRunner {

    private static final String WS_URL = "ws://localhost:8080/ws/iot";

    // Change this value for different scalability tests
    // Examples: 100, 500, 1000, 5000, 10000
    private static final int NODE_COUNT = 1200;

    // Number of telemetry messages each simulated node sends
    private static final int MESSAGES_PER_NODE = 3;

    public static void main(String[] args) throws InterruptedException {

        System.out.println("======================================");
        System.out.println("      GridWeaver IoT Simulator");
        System.out.println("======================================");
        System.out.println("Server            : " + WS_URL);
        System.out.println("Simulated Nodes   : " + NODE_COUNT);
        System.out.println("Messages per Node : " + MESSAGES_PER_NODE);
        System.out.println();

        IoTSimulatorClient simulator = new IoTSimulatorClient(WS_URL);

        simulator.runSimulation(NODE_COUNT, MESSAGES_PER_NODE);

        System.out.println();
        System.out.println("========== Simulation Finished ==========");
    }
}