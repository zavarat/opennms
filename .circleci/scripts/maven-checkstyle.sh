#!/bin/bash

echo "Compile checkstyle module"
cd checkstyle || exit 1
mvn install -DupdatePolicy=never -DskipTests=true -DskipITs=true -T 1C -Dopennms.home=/opt/opennms -Dinstall.version="${INSTALL_VERSION}" --batch-mode --errors --fail-at-end --show-version || exit
