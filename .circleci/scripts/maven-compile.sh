#!/bin/bash

echo "Compiling..."
MAVEN_FLAGS="-DupdatePolicy=never -DskipTests=true -DskipITs=true -T 1C"
mvn install "${MAVEN_FLAGS} -Dopennms.home=/opt/opennms -Dinstall.version=${INSTALL_VERSION} --batch-mode -Prun-expensive-tasks" || exit
mvn install "${MAVEN_FLAGS} -Dopennms.home=/opt/opennms -Dinstall.version=${INSTALL_VERSION} --batch-mode -Prun-expensive-tasks -Pdefault --file opennms-full-assembly/pom.xml" || exit
mvn install "${MAVEN_FLAGS} -Dopennms.home=/opt/opennms -Dinstall.version=${INSTALL_VERSION} --batch-mode -Prun-expensive-tasks --non-recursive --file opennms-tools/pom.xml" || exit
mvn install "${MAVEN_FLAGS} -Dopennms.home=/opt/opennms -Dinstall.version=${INSTALL_VERSION} --batch-mode -Prun-expensive-tasks --file opennms-tools/centric-troubleticketer/pom.xml" || exit