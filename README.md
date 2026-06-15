# Kafka Tutorial

This repository contains my hands-on Apache Kafka tutorial project. I created it to learn and demonstrate the fundamental concepts of Kafka through simple Java examples rather than focusing only on theory.

The project covers the basics of setting up a Kafka environment, creating producers and consumers, and understanding how messages flow through Kafka topics.

## Goals

The purpose of this repository is to:

* Learn the core concepts of Apache Kafka
* Experiment with Kafka producers and consumers
* Provide simple examples that can be run locally
* Serve as a reference for future Kafka-related projects

## Technologies

* Java
* Apache Kafka
* Docker Compose
* Gradle

## Prerequisites

Before running the examples, make sure you have:

* Java installed
* Docker and Docker Compose installed

## Getting Started

Start the Kafka environment:

```bash
docker compose up -d
```

After Kafka is running, execute the desired Java main class from your IDE or using Gradle.

## Project Structure

```text
.
├── kafka-basics/
├── docker-compose.yml
├── build.gradle
├── settings.gradle
└── README.md
```

The project contains example implementations demonstrating Kafka fundamentals such as producing and consuming messages.

## What I Learned

While building this project, I explored:

* Kafka architecture
* Topics and partitions
* Producers and consumers
* Message publishing and consumption
* Local Kafka development using Docker

## Future Improvements

Some ideas for future extensions:

* Consumer groups
* Message serialization with JSON/Avro
* Spring Kafka examples
* Error handling and retries
* Kafka Streams examples

## License

This project is intended for educational and learning purposes.