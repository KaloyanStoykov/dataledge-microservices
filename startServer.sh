#!/bin/bash

echo "Running eureka server"
docker start eureka

echo "Running jenkins"
docker start jenkins
