# Artchart

Artchart is a project for maintaining records of art pieces and generate several charts of their attributes for
statistical purposes.  
The project is a spring boot application written in kotlin.

## Build

To build the project run `mvn install`.

## Run

To run the project, run `mvn spring-boot:run`.  
To be able to run the project, some environment variables are needed to be provided. Those variables are listed in
`.env.example`.

## Use

To use the running application visit `localhost:8080` to an OpenAPI definition page and call a desired endpoint.  
A web ui is provided on `localhost:8080\site` to manipulate the dataset. (TODO: Not implemented yet).