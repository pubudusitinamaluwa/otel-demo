package org.cs.otel.demo.client;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.exporter.otlp.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.TracerSdkManagement;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;

public class HttpClient {

    // OTel API
    private static Tracer tracer;
    // Inject the span context into the request
    private static final TextMapPropagator.Setter<HttpURLConnection> setter =
            URLConnection::setRequestProperty;

    private static void initTracing() {
        // Set properties
        System.setProperty("otel.resource.attributes", "service.name=Service A");
        System.setProperty("otel.exporter", "otlp");
        System.setProperty("otel.exporter.otlp.endpoint", "localhost:55680");
        System.setProperty("otel.exporter.otlp.timeout", "30000");

        // install the W3C Trace Context propagator
        OpenTelemetry.setGlobalPropagators(
                ContextPropagators.create(W3CTraceContextPropagator.getInstance()));

        // Create span exported
        OtlpGrpcSpanExporter spanExporter = OtlpGrpcSpanExporter.getDefault();
        // Build span processor
        BatchSpanProcessor spanProcessor =
                BatchSpanProcessor.builder(spanExporter).setScheduleDelayMillis(100).build();

        // Get the tracer management instance.
        TracerSdkManagement tracerManagement = OpenTelemetrySdk.getGlobalTracerManagement();

        // Set to export spans
        tracerManagement.addSpanProcessor(spanProcessor);

        tracer = OpenTelemetry.getGlobalTracer("org.cs.otel.demo.client.HttpClient");
    }

    private void makeRemoteRequest(Span span) throws IOException {
        int port = 8084;
        URL url = new URL("http://127.0.0.1:" + port);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();

        int status = 0;
        StringBuilder content = new StringBuilder();

        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("component", "http");
            span.setAttribute("http.method", "GET");
            span.setAttribute("http.url", url.toString());

            // Inject the request with the current Context/Span.
            Context currentContext = Context.current();
            OpenTelemetry.getGlobalPropagators()
                    .getTextMapPropagator()
                    .inject(currentContext, con, setter);

            try {
                // Process the request
                con.setRequestMethod("GET");
                status = con.getResponseCode();
                BufferedReader in =
                        new BufferedReader(
                                new InputStreamReader(con.getInputStream(), Charset.defaultCharset()));
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                in.close();
                span.setStatus(StatusCode.OK, "HTTP Code: " + status);
            } catch (Exception e) {
                span.setStatus(StatusCode.ERROR, "HTTP Code: " + status);
            }
        } finally {
            span.end();
        }

        // Output the result of the request
        System.out.println("Response Code: " + status);
        System.out.println("Response Msg: " + content);
    }

    /**
     * Main method to run the example.
     *
     * @param args It is not required.
     */
    public static void main(String[] args) {
        initTracing();
        HttpClient httpClient = new HttpClient();

        // Perform request every 5s
        Thread t =
                new Thread(
                        () -> {
                            while (true) {
                                try {
                                    // Start span
                                    Span span = tracer.spanBuilder("/").setSpanKind(Span.Kind.CLIENT).startSpan();

                                    // Make remote call
                                    httpClient.makeRemoteRequest(span);

                                    // Call a function
                                    functionA(tracer, span);

                                    // Call another function
                                    functionB(tracer, span);

                                    Thread.sleep(5000);
                                } catch (Exception e) {
                                    System.out.println(e.getMessage());
                                }
                            }
                        });
        t.start();
    }

    // Do dummy work
    private static void functionA(Tracer tracer, Span parentSpan) throws InterruptedException {
        // Create root span for service A
        Span functionASpan = tracer.spanBuilder("Function-A")
                .setParent(Context.current().with(parentSpan))
                .startSpan();

        // Set attributes
        functionASpan.setAttribute("version", "1.0");

        // do stuff
        try {
            long timeWait = 1000 + (long) (Math.random() * (2200 - 1000));
            Thread.sleep(timeWait);

            // Set status and close span
            functionASpan.setStatus(StatusCode.OK);
        } finally {
            functionASpan.end();
        }
    }

    private static void functionB(Tracer tracer, Span parentSpan) throws InterruptedException {
        // Create root span for service B
        Span functionBSpan = tracer.spanBuilder("function-B")
                .setParent(Context.current().with(parentSpan))
                .startSpan();
        // do stuff
        try {
            long timeWait = 100 + (long) (Math.random() * (2000 - 100));
            Thread.sleep(timeWait);

            // Add new function
            functionC(tracer, parentSpan);

            // set status
            functionBSpan.setStatus(StatusCode.OK);
        } finally {
            // Close span
            functionBSpan.end();
        }
    }

    private static void functionC(Tracer tracer, Span parentSpan) throws InterruptedException {
        // Create root span for service C
        Span functionCSpan = tracer.spanBuilder("function-C")
                .setParent(Context.current().with(parentSpan))
                .startSpan();
        // do stuff
        try {
            long timeWait = 3000 + (long) (Math.random() * (5000 - 3000));
            Thread.sleep(timeWait);

            // set status
            functionCSpan.setStatus(StatusCode.OK);
        } finally {
            // Close span
            functionCSpan.end();
        }
    }

}
