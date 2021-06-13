# Opentelemetry Collector - Demo

This directory contains the docker configurations to run Opentelemetry collector, Zipkin and Jager
in your local machine. To build and run the demo, switch to this directory and execute
```
docker-compose up
```

Important: Don't forget to update lighstep token in [otel-collector-config](otel-collector-config.yml) if you are pushing 
tracing to lightstep. 

After a successful run, you will be able to access <br>
Zipkin UI : http://localhost:9411/ <br>
Jaeger UI : http://localhost:16686/

Desclaimer : This folder contains an altered version of example manifests found [here](https://github.com/open-telemetry/opentelemetry-collector-contrib/tree/main/examples/tracing)
in [opentelemetry-collector-contrib](https://github.com/open-telemetry/opentelemetry-collector-contrib) repository.