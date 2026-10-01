#!/bin/bash
set -e

chmod +x gradlew
git add gradlew gradlew.bat gradle/wrapper .github/workflows/build.yaml
git commit -m "Configure CI and Gradle wrapper" || true
git push
