#!/bin/sh
./mvnw clean install
./mvnw release:clean
./mvnw release:prepare
./mvnw release:perform


