#!/bin/bash

export JAVA_HOME=/home/builderb0y/java/jdk-25+36/;

./gradlew "Switch to 26.1" && ./gradlew build && \
./gradlew --stop;