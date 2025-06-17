# hmpps-remand-and-sentencing-api
[![repo standards badge](https://img.shields.io/badge/dynamic/json?color=blue&style=flat&logo=github&label=MoJ%20Compliant&query=%24.result&url=https%3A%2F%2Foperations-engineering-reports.cloud-platform.service.justice.gov.uk%2Fapi%2Fv1%2Fcompliant_public_repositories%2Fhmpps-remand-and-sentencing-api)](https://operations-engineering-reports.cloud-platform.service.justice.gov.uk/public-github-repositories.html#hmpps-remand-and-sentencing-api "Link to report")
[![Pipeline [test -> build -> deploy]](https://github.com/ministryofjustice/hmpps-remand-and-sentencing-api/actions/workflows/pipeline.yml/badge.svg?branch=main)](https://github.com/ministryofjustice/hmpps-remand-and-sentencing-api/actions/workflows/pipeline.yml)
[![Docker Repository on ghcr](https://img.shields.io/badge/ghcr.io-repository-2496ED.svg?logo=docker)](https://ghcr.io/ministryofjustice/hmpps-remand-and-sentencing-api)
[![API docs](https://img.shields.io/badge/API_docs_-view-85EA2D.svg?logo=swagger)](https://remand-and-sentencing-api-dev.hmpps.service.justice.gov.uk/swagger-ui.html)

This is a skeleton project from which to create new kotlin projects from.

# Instructions

If this is a HMPPS project then the project will be created as part of bootstrapping - 
see https://github.com/ministryofjustice/dps-project-bootstrap.

## Running tests
execute the following commands:
```shell
docker compose up remand-and-sentencing-db localstack -d 
./gradlew check
```
