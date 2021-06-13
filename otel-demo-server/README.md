# OpenTelemetry - Demo Server

This is a demo server application instrumented with opentelemetry tracing which accepts and process
HTTP requests from [otel-demo-client](../otel-demo-client). The server receives the request with 
distributed tracing context which it extracts and stitches the tracing on server side to complete the
full tracing path. Then the completed traces are collected by the opentelemetry collector and exported to
configured receivers.

The `otel-demo-server` is a standalone application. It can be started as follow.
* Switch to this directory
* Run `./gradlew assemble` to build and assemble the jar
* Run `java -jar .\build\libs\otel-demo-server-1.0.jar` to start the server

Note that the [opentelemetry collecor](../tracing) need to be up and running before start sending 
traffic to the server to collect generated traces.