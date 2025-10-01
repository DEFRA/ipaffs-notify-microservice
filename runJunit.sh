#!/bin/sh

sdk env
mvn clean test jacoco:report -f service/pom.xml --settings service/vs-maven.xml -Dmaven.repo.local=${WORKSPACE}/.m2
