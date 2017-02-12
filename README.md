# Unhypem Backend

[![Build Status](https://img.shields.io/travis/feedm3/unhypem-backend.svg?style=flat-square)](https://travis-ci.org/feedm3/unhypem-backend)
[![Dependency Status](https://dependencyci.com/github/feedm3/unhypem-backend/badge?style=flat-square)](https://dependencyci.com/github/feedm3/unhypem-backend)

Backend for [unhypem.com](https://unhypem.com).

## Setup

You have to setup a postgres database and edit the _application,properties_ file.

## Run

To start the project simply hit
```
gradlew bootRun
```

## Test

To start all test use
```
gradlew test
```

#### Outdated dependencies

To check for outdated dependencies
```
gradlew dependencyUpdates -Drevision=release
```

#### Vulnerabilities

To check for vulnerabilities in the dependencies run
```
gradlew dependencyCheck --info
```
The reports will be generated automatically under `build/reports` folder
