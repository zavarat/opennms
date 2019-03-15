#!/bin/bash

mvn clean -DupdatePolicy=never || exit
mvn clean -DupdatePolicy=never --file opennms-full-assembly/pom.xml || exit
