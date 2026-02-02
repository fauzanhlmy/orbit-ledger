#!/bin/sh
source ../../../java-17-env.sh
mvn clean compile
mvn clean test
#mvn clean compile -DskipTests