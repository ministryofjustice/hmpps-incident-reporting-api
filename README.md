# HMPPS Incident Reporting API

[![CircleCI](https://circleci.com/gh/ministryofjustice/hmpps-incident-reporting-api/tree/main.svg?style=svg)](https://circleci.com/gh/ministryofjustice/hmpps-incident-reporting-api)
[![Docker Repository on Quay](https://quay.io/repository/hmpps/hmpps-incident-reporting-api/status "Docker Repository on Quay")](https://quay.io/repository/hmpps/hmpps-incident-reporting-api)
[![Runbook](https://img.shields.io/badge/runbook-view-172B4D.svg?logo=confluence)](https://dsdmoj.atlassian.net/wiki/spaces/NOM/pages/1739325587/DPS+Runbook)
[![API docs](https://img.shields.io/badge/API_docs_-view-85EA2D.svg?logo=swagger)](https://incident-reporting-api-dev.hmpps.service.justice.gov.uk/swagger-ui/index.html)

This application is api and database that owns incident report data for prisons.

## Running locally against dev/T3 services

This is straight-forward as authentication is delegated down to the calling services in `dev` environment.
Environment variables to be set are as follows:

```
API_BASE_URL_OAUTH=https://sign-in-dev.hmpps.service.justice.gov.uk/auth
API_BASE_URL_PRISON=https://prison-api-dev.prison.service.justice.gov.uk
INCIDENT_REPORTING_API_CLIENT_ID=[choose a suitable hmpps-auth client]
INCIDENT_REPORTING_API_CLIENT_SECRET=
```

Start the database and other required services via docker-compose with:

```shell
docker compose -f docker-compose-local.yml up
```

Then run the API.

## Architecture

Architecture decision records start [here](docs/0001-use-adr.md)
