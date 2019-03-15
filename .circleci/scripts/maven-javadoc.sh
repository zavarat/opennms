#!/bin/bash

mvn javadoc:aggregate -DupdatePolicy=never --batch-mode -Prun-expensive-tasks || exit