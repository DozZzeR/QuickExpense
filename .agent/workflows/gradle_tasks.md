---
description: common gradle and android dev tasks
---

# Gradle & Android Workflows

## Build and Run
1. To build the project:
```bash
./gradlew assembleDebug
```

## Running Tests
1. To run unit tests:
```bash
./gradlew test
```
2. To run instrumentation tests (requires emulator/device):
```bash
./gradlew connectedAndroidTest
```

## Static Analysis
1. To run lint:
```bash
./gradlew lint
```

## Cleaning the project
1. To clean build artifacts:
```bash
./gradlew clean
```
