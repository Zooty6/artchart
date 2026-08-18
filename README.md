# Artchart

Artchart is a project for maintaining records of art pieces and generate several charts of their attributes for
statistical purposes.  
The project is a spring boot application written in kotlin.

## Build

To build the project run `mvn install`.

## Run

To run the project, run `mvn spring-boot:run`.  
To be able to run the project, a sqlite database and some environment variables are needed to be provided. Those
variables are listed in
`.env.example`.

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
