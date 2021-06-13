# OpenTelemetry Demo

##Introduction
This repository contains a simple yet complete example of instrumenting your application for 
[distributed tracing](https://opentracing.io/docs/overview/what-is-tracing/#:~:text=Distributed%20tracing%2C%20also%20called%20distributed,and%20what%20causes%20poor%20performance.) 
with [opentelemetry](https://opentelemetry.io/). Examples included here will guide you to run a demo client-server communication 
scenario and monitoring/analyzing tracing with distributed tracing tools like 
[Jaeger](https://github.com/jaegertracing/jaeger), 
[Zipkin](https://github.com/openzipkin/zipkin) and 
[Lightstep](https://lightstep.com/).

This repository contains 2 projects.
* [otel-demo-server](otel-demo-server) -
A simple server implementation that accepts HTTP requests from a client with 
distributed tracing context. The server application is instrumented with opentelemetry tracing and stitches generated tracing 
to tracing generated from client using the received distributed tracing context. 
* [otel-demo-client](otel-demo-client) -
A simple client instrumented opentelemetry tracing, which produces traffic to the server periodically (to mock the traffic)
with passing the distributed tracing context.

This project also contains instructions to run opentelemetry collector, Zipkin and Jaeger on your local machine
to monitor/analyze the generated traces [here](tracing).

## Running the demo

Start the opentelemetry collector, Zipkin and Jaeger by following instructions given in the readme [here](tracing)

Swith to [otel-demo-server](otel-demo-server) and run the application to start a server that accepts and process HTTP requests.

Switch to [otel-demo-client](otel-demo-client) and run the application to generate synthetic traffic on the server.

Observer traces through <br>
Zipkin UI : http://localhost:9411/ <br>
Jaeger UI : http://localhost:16686/