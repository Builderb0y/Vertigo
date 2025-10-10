#!/bin/bash

export JAVA_HOME=/home/builderb0y/java/jdk-21.0.4+7/;

./gradlew "Switch to 1.20.1" && ./gradlew build && \
./gradlew "Switch to 1.20.2" && ./gradlew build && \
./gradlew "Switch to 1.20.4" && ./gradlew build && \
./gradlew "Switch to 1.20.6" && ./gradlew build && \
./gradlew "Switch to 1.21.1" && ./gradlew build && \
./gradlew "Switch to 1.21.3" && ./gradlew build && \
./gradlew "Switch to 1.21.4" && ./gradlew build && \
./gradlew "Switch to 1.21.5" && ./gradlew build && \
./gradlew "Switch to 1.21.8" && ./gradlew build && \
./gradlew "Switch to 1.21.10" && ./gradlew build && \
./gradlew --stop;