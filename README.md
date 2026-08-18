# Artchart

Artchart is a project for maintaining records of art pieces and generate several charts of their attributes for
statistical purposes.  
The project is a spring boot application written in kotlin.

## Build

To build the project run `mvn install`.

## Run

To run the project, first create a `.env` file in the project root based on `.env.example`, then use the platform-specific
script:

- PowerShell: `./run.ps1`
- Bash: `./run.sh`

The script loads the variables from `.env` and runs `mvn spring-boot:run` through the Maven wrapper. Additional Maven
arguments can be passed to either script, for example `./run.sh -Dspring-boot.run.profiles=dev`.

## Use

After starting the application, the following entry points are available:

- Web UI: `http://localhost:8080/site`
- Swagger UI: `http://localhost:8080/swagger`
- OpenAPI definition: `http://localhost:8080/apidocs`
- REST API: `http://localhost:8080/api/**`
- Charts: `http://localhost:8080/chart/**`

The web UI is rendered with Thymeleaf and uses HTMX for incremental page updates.

Artwork media is served from the directory configured by `ARTCHART_MEDIA_ROOT`. The expected file layout is
`<ARTCHART_MEDIA_ROOT>/<SFW|NSFW>/<year>/<fileName>`, where the first directory is selected from `Art.isNsfw`. Missing
files are displayed with a placeholder image.
