# OpenTelemetry - Demo Client

This is a demo client application created for generation synthetic traffic on [otel-demo-server](../otel-demo-server)
application periodically. The client application itself is instrumented with opentelemetry tracing. While generating 
traffic on the server, the client will pass the distributed tracing context to the server simulating a distributed 
tracing scenario between two microservices.

The server will extract the distributed tracing context from the request and stitch tracing generated in server side to
complete the request trace. The completed trace is then collected by the [opentelemetnry collector](../tracing) and
exported to Zipkin, Jager and Lightstep.

This application is a standalone application. To run the client,
* Switch to this directory
* Run `./gradlew assemble` to build and assemble the jar
* Run `java -jar .\build\libs\otel-demo-client-1.0.jar` to start the client

Note that the `opentelemetry collector` and `otel-demo-server` should be running before starting the 
client as it will start to send request to the server and export traces.