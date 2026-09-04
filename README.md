# GridWeaver – IoT Microgrid Monitoring System

GridWeaver is a real-time IoT microgrid monitoring and state management system designed to handle a large number of concurrent device connections and state changes efficiently.

The project simulates an IoT-enabled microgrid where multiple devices continuously send state updates. The backend processes these events in real time using Java 21 Virtual Threads and WebSocket communication, while the React-based frontend provides a live geographical view of the microgrid.

---

## 🚀 Features

- Real-time IoT device state monitoring
- Support for large numbers of concurrent connections
- Java 21 Virtual Threads for lightweight concurrency
- WebSocket-based real-time communication
- IoT device simulator for generating state changes
- REST APIs for device and simulator management
- Microgrid state management using Spring State Machine
- Real-time map visualization using React and Leaflet
- Simulated charge/discharge operations
- Backend–frontend real-time integration

---

## 🏗️ System Architecture

```text
                    ┌──────────────────────┐
                    │    IoT Simulator     │
                    │                      │
                    │ Device State Changes │
                    └──────────┬───────────┘
                               │
                               ▼
                    ┌──────────────────────┐
                    │     Spring Boot      │
                    │       Backend        │
                    └──────────┬───────────┘
                               │
                  ┌────────────┴────────────┐
                  │                         │
                  ▼                         ▼
        ┌──────────────────┐      ┌──────────────────┐
        │ WebSocket Layer  │      │   REST APIs      │
        │                  │      │                  │
        │ Real-time Events │      │ Device/Simulator │
        └────────┬─────────┘      └──────────────────┘
                 │
                 ▼
        ┌──────────────────────┐
        │ Grid State Management│
        │                      │
        │ Spring State Machine │
        └──────────┬───────────┘
                   │
                   ▼
        ┌──────────────────────┐
        │   React Frontend     │
        │                      │
        │ React + Leaflet Map  │
        └──────────────────────┘
````

---

## 🛠️ Technology Stack

### Backend

* Java 21
* Spring Boot
* Spring WebSocket
* Spring State Machine
* REST APIs
* Java Virtual Threads

### Frontend

* React
* JavaScript
* Leaflet
* HTML/CSS

### Development Tools

* Maven
* Git & GitHub
* WebSocket
* IoT Simulator

---

## ⚡ Why Java Virtual Threads?

GridWeaver is designed to handle a large number of concurrent IoT state changes.

Traditional platform threads can become expensive when thousands of devices need to communicate simultaneously. Java 21 Virtual Threads provide lightweight threads that allow the application to handle a much larger number of concurrent tasks without creating an equivalent number of heavyweight OS threads.

This makes Virtual Threads well suited for the highly concurrent nature of IoT monitoring systems.

---

## 🔄 Real-Time Workflow

```text
IoT Device
    │
    │ State Change
    ▼
IoT Simulator
    │
    ▼
Spring Boot Backend
    │
    ├──► State Machine
    │
    └──► WebSocket
             │
             ▼
        React Frontend
             │
             ▼
       Leaflet Map
```

The simulator generates device events such as state changes. These events are processed by the backend and the updated state is pushed to connected frontend clients through WebSocket communication.

---

## 🔋 Microgrid State Management

GridWeaver models the operational states of microgrid devices and processes state transitions based on incoming events.

For example:

```text
        ┌───────────┐
        │   IDLE    │
        └─────┬─────┘
              │
          DISCHARGE
              ▼
        ┌───────────┐
        │ DISCHARGE │
        └───────────┘
```

The Spring State Machine is used to manage these transitions and maintain consistent device states.

---

## 🗺️ Frontend Visualization

The frontend provides a geographical representation of the microgrid using React and Leaflet.

The interface can display:

* Microgrid/device locations
* Current device states
* Real-time state updates
* Device activity
* Grid monitoring information

---

## 🧪 IoT Simulator

The built-in IoT simulator generates simulated device activity so that the system can be tested without requiring physical IoT hardware.

The simulator can be used to:

1. Create simulated devices
2. Generate device state changes
3. Send events to the backend
4. Test concurrent device activity
5. Observe real-time updates on the monitoring dashboard

---

## 📁 Project Structure

```text
gridweaver/
│
├── backend/
│   ├── src/
│   ├── pom.xml
│   └── ...
│
├── frontend/
│   ├── src/
│   ├── package.json
│   └── ...
│
├── simulator/
│   └── ...
│
├── README.md
└── ...
```

> The exact directory structure may vary depending on the final project configuration.

---

## ▶️ Getting Started

### Prerequisites

Make sure the following are installed:

* Java 21+
* Maven
* Node.js and npm
* Git

### 1. Clone the Repository

```bash
git clone <https://github.com/RudriDesai/gridweaver-project>
cd gridweaver
```

### 2. Start the Backend

```bash
cd backend
./mvnw spring-boot:run
```

On Windows:

```bash
mvnw.cmd spring-boot:run
```

### 3. Start the Frontend

```bash
cd frontend
npm install
npm run dev
```

The frontend will be available at the URL displayed by Vite.

---

## 📡 Communication

GridWeaver uses two primary communication mechanisms:

### REST APIs

Used for operations such as:

* Device management
* Simulator management
* Configuration
* State-related operations

### WebSocket

Used for:

* Real-time device updates
* Live state changes
* Continuous communication between backend and frontend

---

## 🎯 Project Goals

The main goals of GridWeaver are:

* Demonstrate high-concurrency IoT processing
* Explore Java 21 Virtual Threads
* Implement real-time WebSocket communication
* Model microgrid device states
* Provide a simulated IoT environment
* Visualize microgrid activity in real time
* Build a scalable foundation for IoT monitoring systems

---

## 📌 Future Improvements

Possible future enhancements include:

* Persistent database storage
* Authentication and authorization
* Historical device data and analytics
* Alert and notification system
* Real IoT hardware integration
* Advanced energy consumption analytics
* Horizontal scaling for larger deployments
* Monitoring and observability dashboards

---

## 👨‍💻 Project Context

GridWeaver was developed as part of an internship project to gain practical experience in building a real-time, concurrent IoT application using modern Java and web technologies.

The project provided hands-on experience with backend development, frontend integration, WebSockets, state management, concurrency, REST APIs, and IoT simulation.

---

## 📄 License

This project is intended for educational and internship purposes.
