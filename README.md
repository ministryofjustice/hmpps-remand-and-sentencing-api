# hmpps-remand-and-sentencing-api
[![Ministry of Justice Repository Compliance Badge](https://github-community.service.justice.gov.uk/repository-standards/api/hmpps-remand-and-sentencing-api/badge?style=flat)](https://github-community.service.justice.gov.uk/repository-standards/hmpps-remand-and-sentencing-api)
[![Pipeline [test -> build -> deploy]](https://github.com/ministryofjustice/hmpps-remand-and-sentencing-api/actions/workflows/pipeline.yml/badge.svg?branch=main)](https://github.com/ministryofjustice/hmpps-remand-and-sentencing-api/actions/workflows/pipeline.yml)
[![Docker Repository on ghcr](https://img.shields.io/badge/ghcr.io-repository-2496ED.svg?logo=docker)](https://ghcr.io/ministryofjustice/hmpps-remand-and-sentencing-api)
[![API docs](https://img.shields.io/badge/API_docs_-view-85EA2D.svg?logo=swagger)](https://remand-and-sentencing-api-dev.hmpps.service.justice.gov.uk/swagger-ui.html)
[![Github Pages](https://img.shields.io/badge/github%20pages-121013?style=for-the-badge&logo=github&logoColor=white)](https://ministryofjustice.github.io/hmpps-remand-and-sentencing-api)

This is a skeleton project from which to create new kotlin projects from.

# Instructions

If this is a HMPPS project then the project will be created as part of bootstrapping - 

```

will build the application and run it and HMPPS Auth within a local docker instance.

### Running the application in Intellij

```bash
docker compose pull && docker compose up --scale hmpps-remand-and-sentencing-api=0
If this is a HMPPS project then the project will be created as part of bootstrapping - 
see https://github.com/ministryofjustice/dps-project-bootstrap.
## Running tests
execute the following commands:
```shell
docker compose up remand-and-sentencing-db localstack -d 
./gradlew check
```

will just start a docker instance of HMPPS Auth. The application should then be started with a `dev` active profile
in Intellij.
