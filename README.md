# P2P Network Simulator

![P2P Simulator Screenshot](https://github.com/Yash-Shindey/p2psimulator/assets/96872207/ea7e648a-a4a4-486d-a3ca-6d62fe58e21c)

## Overview

P2P Network Simulator is a Java-based application that provides a visual simulation of peer-to-peer network behavior. The simulator demonstrates how nodes in a P2P network interact, communicate, and respond to various network conditions such as latency, bandwidth limitations, and packet loss.

## Features

- **Dynamic Node Management**: Add and remove nodes in real-time
- **Network Visualization**: See connections between nodes and packet transmission
- **Customizable Parameters**: Adjust simulation speed and node appearance
- **Realistic Network Conditions**:
  - Variable latency based on node distance
  - Bandwidth limitations
  - Random packet loss
  - Clock desynchronization between nodes

## Requirements

- Java Development Kit (JDK) 8 or higher
- A graphical display environment

## Installation and Running

1. Clone the repository:
   ```
   git clone https://github.com/yourusername/p2psimulator.git
   cd p2psimulator
   ```

2. Compile the Java file:
   ```
   javac P2PNetworkSimulator.java
   ```

3. Run the application:
   ```
   java P2PNetworkSimulator
   ```

## Usage

### User Interface

The simulator has a main visualization panel and a control panel on the right side with the following controls:

- **Add Node**: Adds a new node to the network
- **Subtract Node**: Removes the most recently added node
- **Speed Slider**: Adjusts the simulation speed (1-200%)
- **Change Node Colour**: Randomly changes the color of all nodes

### Network Behavior

- Nodes automatically establish connections with other nodes in the network
- Messages are visualized as small circles traveling between nodes
- Connection lines are color-coded to indicate bandwidth utilization:
  - Green: High available bandwidth
  - Yellow/Orange: Medium available bandwidth
  - Red: Low available bandwidth
- Red circles indicate dropped packets

## Technical Details

### Key Components

- **P2PNetworkSimulator**: Main class with GUI and simulation setup
- **Network**: Manages the overall network simulation including:
  - Node placement
  - Link establishment
  - Message transmission
  - Time synchronization
- **Node**: Abstract class representing network participants
- **TestNode**: Implementation of a basic peer node
- **Link**: Represents connections between nodes
- **Transmission**: Represents messages being sent between nodes

### Network Parameters

The simulator models several realistic network conditions:

- **Default Link Rate**: Base bandwidth for connections
- **Latency Per Distance**: How distance affects transmission delay
- **Max Random Latency**: Additional random delay variability
- **Packet Drop Chance**: Probability of message loss
- **Clock Desynchronization**: Time difference between nodes

## Customization

You can modify the following parameters in the code to customize the simulation:

- Initial number of nodes (`maxnodes`)
- Network parameters in the Network constructor:
  ```java
  net = new Network(
      30,       // default_link_rate
      0.01f,    // latency_per_distance
      .1f,      // max_random_latency
      0.2f,     // packet_drop_chance
      100f,     // clock_desynchronization
      12345     // random_seed
  );
  ```

## Educational Use

This simulator is useful for teaching and understanding several network concepts:

- Peer-to-peer network topology
- Bandwidth limitations and congestion
- Effects of latency on network performance
- Clock synchronization issues in distributed systems
- Packet loss and reliability

## Credits

This project is forked from [Alrecenk's P2P-Simulator](https://github.com/Alrecenk/P2P-Simulator) with modifications by [Yash-Shindey](https://github.com/Yash-Shindey).

## License

Feel free to use, modify, and distribute this code for educational purposes.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.
