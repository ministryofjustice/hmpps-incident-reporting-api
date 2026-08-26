# HMPPS Incident Reporting API

[![Ministry of Justice Repository Compliance Badge](https://github-community.service.justice.gov.uk/repository-standards/api/hmpps-incident-reporting-api/badge?style=flat)](https://github-community.service.justice.gov.uk/repository-standards/hmpps-incident-reporting-api)
[![Docker Repository on ghcr](https://img.shields.io/badge/ghcr.io-repository-2496ED.svg?logo=docker)](https://ghcr.io/ministryofjustice/hmpps-incident-reporting-api)
[![Runbook](https://img.shields.io/badge/runbook-view-172B4D.svg?logo=confluence)](https://dsdmoj.atlassian.net/wiki/spaces/NOM/pages/1739325587/DPS+Runbook)
[![API docs](https://img.shields.io/badge/API_docs_-view-85EA2D.svg?logo=swagger)](https://incident-reporting-api-dev.hmpps.service.justice.gov.uk/swagger-ui/index.html)
[![Event docs](https://img.shields.io/badge/Event_docs-view-85EA2D.svg)](https://studio.asyncapi.com/?url=https://raw.githubusercontent.com/ministryofjustice/hmpps-incident-reporting-api/main/async-api.yml&readOnly)

This application is the REST api and database that owns incident report data for prisons.

[View the guidelines here](guidelines.md)

[View the key features of the service here](key-features.md)

## Running locally against dev/T3 services

This is straight-forward as authentication is delegated down to the calling services in `dev` environment.

Use all environment variables starting with `API_BASE_URL_` from [helm chart values](./helm_deploy/values-dev.yaml).
Choose a suitable hmpps-auth oauth client, for instance from kubernetes `hmpps-incentives-api` secret and add
`INCIDENT_REPORTING_API_CLIENT_ID` and `INCIDENT_REPORTING_API_CLIENT_SECRET`.

Start the database and other required services via docker-compose with:

```shell
docker compose -f docker-compose-local.yml up
```

Then run the API; for example using IntelliJ.

## Testing and linting

Run unit and integration tests with:

```shell
./gradlew test
```

Run automatic lint fixes:

```shell
./gradlew ktlintformat
```

## Connecting to AWS resources from a local port

There are custom gradle tasks that make it easier to connect to AWS resources (RDS and ElastiCache Redis)
in Cloud Platform from a local port:

```shell
./gradlew portForwardRDS
# and
./gradlew portForwardRedis
```

These could be useful to, for instance, clear out a development database or edit data live.

They require `kubectl` to already be set up to access the kubernetes cluster;
essentially these tasks are just convenience wrappers.

Both accept the `--environment` argument to select between `dev`, `preprod` and `prod` namespaces
or prompt for user input when run.

Both also accept the `--port` argument to choose a different local port, other than the resource’s default.

## Database schema

A browsable schema report is published from `main` to
[ministryofjustice.github.io/hmpps-incident-reporting-api/schema-spy-report](https://ministryofjustice.github.io/hmpps-incident-reporting-api/schema-spy-report/).

The report shows every table and column, with types, nullability, primary and foreign keys, and ER
diagrams. Share it rather than a hand-written description when explaining the schema — to the Data Hub
transition team, or when working out what a subject access request covers.

It is generated from a database built by Flyway, so it cannot drift from the migrations. To regenerate
it locally:

```shell
docker compose -f docker-compose-schema-spy.yml up -d --wait
./gradlew -Pinit-db=true test --tests '*InitialiseDatabase'
docker run --rm --network host -v /tmp/schemaspy:/output schemaspy/schemaspy:6.2.4 \
  -t pgsql -host localhost -port 5432 -db incident_reporting -s public \
  -u incident_reporting -p incident_reporting -vizjs
```

If you change `V1_48__schema_comments.sql` while the compose database is still up, Flyway will refuse to
start with a checksum mismatch — the container persists between runs. Recreate it with
`docker compose -f docker-compose-schema-spy.yml down -v` before re-running.

### Table and column descriptions

Descriptions live in the database as `COMMENT ON` statements, applied by
`db/migration/V1_48__schema_comments.sql`, so SchemaSpy and any Glue crawl read the same source of
truth. Each column description ends with a sensitivity classification:

| Tag | Meaning |
| --- | --- |
| `[Sensitivity: NONE]` | Not personal data in itself |
| `[Sensitivity: PERSONAL]` | Personal data about a prisoner — identifies or locates them |
| `[Sensitivity: STAFF]` | Personal data about a member of staff, typically the username that acted |
| `[Sensitivity: SPECIAL-CATEGORY]` | UK GDPR Article 9 data, or offence data under Article 10 |
| `[Sensitivity: OFFICIAL-SENSITIVE]` | Not personal data, but damaging if disclosed |

`STAFF` is still personal data and still in scope for a staff member's own subject access request. It
is separated from `PERSONAL` so an extract about prisoners can be reasoned about without staff columns
inflating the count.

The tags describe **the column's own content, not the row's**. Almost every report names prisoners
through `prisoner_involvement`, so the record as a whole is personal data about them whatever an
individual column is marked.

**This is the most sensitive schema in the Manage Safety set.** Reports cover self-harm, deaths in
custody, assaults, sexual assaults and drug finds, so `report.type` alone is Article 9 or Article 10
data about everyone named on the report, whichever type it is. Every free-text column should be assumed
to contain more than its question asks. Of 129 columns: 22 are special category, 15 staff, 4 personal.

Two things worth knowing when reading the tags:

- Question *wording* (`question.question`, `question.label`) is form text, identical for every report of
  a type and derivable from `report.type`, so it is `NONE`. What was *answered* — `response.code`,
  `response.label`, `response.response` and any additional information — is `SPECIAL-CATEGORY`.
- Anything analysing answers over time must read `historical_question` and `historical_response` as well
  as `question` and `response`, or it silently misses every report whose type has been changed.

**Any new table or column needs a `COMMENT ON`** in a migration — `SchemaCommentsTest` fails the build
otherwise. A later migration can add to or replace any comment at any time.

Note that the compose database binds host port 5432 deliberately: `TestContainer.isRunning()` defers to
an already-running database, so `InitialiseDatabase` migrates that container and SchemaSpy can read the
same schema afterwards. Left to Testcontainers the schema would die with the JVM.

## Architecture

Architecture decision records start [here](docs/0001-use-adr.md)
