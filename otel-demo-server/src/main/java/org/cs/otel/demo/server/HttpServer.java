package org.cs.otel.demo.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
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

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;

import static io.opentelemetry.api.common.AttributeKey.stringKey;

public class HttpServer {

    private static class HelloHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Extract the context from the HTTP request
            Context context =
                    OpenTelemetry.getGlobalPropagators()
                            .getTextMapPropagator()
                            .extract(Context.current(), exchange, getter);
            Span span = tracer.spanBuilder("GET /")
                    .setParent(context)
                    .setSpanKind(Span.Kind.SERVER)
                    .startSpan();

            try (Scope scope = span.makeCurrent()) {
                // Set the Semantic Convention
                span.setAttribute("component", "http");
                span.setAttribute("http.method", "GET");
                span.setAttribute("http.scheme", "http");
                span.setAttribute("http.host", "localhost:" + HttpServer.port);
                span.setAttribute("http.target", "/");
                // Process the request
                answer(exchange, span);
                span.setStatus(StatusCode.OK);
            } catch (InterruptedException e) {
                span.setStatus(StatusCode.ERROR);
            } finally {
                // Close the span
                span.end();
            }
        }

        private void answer(HttpExchange httpExchange, Span span) throws IOException, InterruptedException {
            // Generate an Event
            span.addEvent("Start Processing");

            // Make dummy db call
            dbCall(tracer, span);

            // Process the request
            String response = "Hello World!";
            httpExchange.sendResponseHeaders(200, response.length());
            OutputStream os = httpExchange.getResponseBody();
            os.write(response.getBytes(Charset.defaultCharset()));
            os.close();
            System.out.println("Served Client: " + httpExchange.getRemoteAddress());

            // Generate an Event with an attribute
            Attributes eventAttributes = Attributes.of(stringKey("answer"), response);
            span.addEvent("Finish Processing", eventAttributes);
        }
    }

    private final com.sun.net.httpserver.HttpServer server;
    private static final int port = 8084;

    // OTel API
    private static Tracer tracer;
    // Extract the context from http headers
    private static final TextMapPropagator.Getter<HttpExchange> getter =
            new TextMapPropagator.Getter<>() {
                @Override
                public Iterable<String> keys(HttpExchange carrier) {
                    return carrier.getRequestHeaders().keySet();
                }

                @Override
                public String get(HttpExchange carrier, String key) {
                    if (carrier.getRequestHeaders().containsKey(key)) {
                        return carrier.getRequestHeaders().get(key).get(0);
                    }
                    return "";
                }
            };

    private HttpServer() throws IOException {
        this(port);
    }

    private HttpServer(int port) throws IOException {
        server = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress(port), 0);
        // Test urls
        server.createContext("/", new HelloHandler());
        server.start();
        System.out.println("Server ready on http://127.0.0.1:" + port);
    }

    private static void initTracing() {
        // Set properties
        System.setProperty("otel.resource.attributes", "service.name=Service B");
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

        // Get the tracer
        TracerSdkManagement tracerManagement = OpenTelemetrySdk.getGlobalTracerManagement();

        // Add span processor
        tracerManagement.addSpanProcessor(spanProcessor);

        tracer = OpenTelemetry.getGlobalTracer("org.cs.otel.demo.client.HttpServer");
    }

    private void stop() {
        server.stop(0);
    }

    // Dummy db call function
    private static void dbCall(Tracer tracer, Span parentSpan) throws InterruptedException {
        // Create root span for DB transactions
        Span dbSpan = tracer.spanBuilder("my-sql")
                .setParent(Context.current().with(parentSpan))
                .startSpan();

        // Set attributes
        dbSpan.setAttribute("operation", "update");

        // do stuff
        long timeWait = 100 + (long) (Math.random() * (1000 - 100));
        Thread.sleep(timeWait);

        // Set status and close span
        dbSpan.setStatus(StatusCode.OK);
        dbSpan.end();
    }

    /**
     * Main method to run the example.
     *
     * @param args It is not required.
     * @throws Exception Something might go wrong.
     */
    public static void main(String[] args) throws Exception {
        initTracing();
        final HttpServer s = new HttpServer();
        // Gracefully close the server
        Runtime.getRuntime().addShutdownHook(new Thread(s::stop));
    }
}