[![Build Status](https://travis-ci.org/ExpediaDotCom/haystack-dropwizard.svg?branch=master)](https://travis-ci.org/ExpediaDotCom/haystack-dropwizard)
[![License](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg)](https://github.com/ExpediaDotCom/haystack/blob/master/LICENSE)

## Table of Contents

- [Instrumenting dropwizard applications](#Instrumenting-dropwizard-applications)
- [Quick start](#quick-start)
  * [Haystack dropwizard dependency](#haystack-dropwizard-dependency)
  * [Usage](#Usage)
- [See Also](#See-also)

## Instrumenting dropwizard applications
One can use haystack-dropwizard to instrument dropwizard applications and send tracing information to open tracing compliant [Haystack](https://expediadotcom.github.io/haystack) server distributed tracing platform.

This library in turn use [haystack-client-java](https://github.com/ExpediaDotCom/haystack-client-java) which provides an implementation of `io.opentracing.Tracer` and helps create an instance of it.

## Quick Start
This section provides steps required to quickly configure your dropwizard application, to be wired using [OpenTracing JAX-RS Instumentation](https://github.com/opentracing-contrib/java-jaxrs)'s dropwizard integration to haystack.
If you need additional information, please read the subsequent sections in this document.

# Haystack dropwizard dependency
Add the following dependency to your application.

```
   <dependency>
        <groupId>com.expedia.www</groupId>
        <artifactId>haystack-dropwizard</artifactId>
        <version>${haystack-dropwizard.version}</version>
   </dependency>
```

# Usage
To make use of the depency to create and manage traces one needs to do the following.

Instantiate a `HaystackTracerBundle` to be passed to your dropwizard application while bootstrapping. 
Also in case we need to trace the client requests emanating from this app we need to register the clienttracing feature with the client to be used. 
Here is a short example:

```
public class FrontendApplication extends Application<HelloWorldConfiguration> {
    
    // This bundle needs to be always initialised and passed to the app while bootstrapping.
    // bundle that initializes an instance of io.opentracing.Tracer
    private final HaystackTracerBundle<HelloWorldConfiguration> haystackTracerBundle = new HaystackTracerBundle<>();

    // Here we are adding the bundle
    @Override
    public void initialize(Bootstrap<HelloWorldConfiguration> bootstrap) {
        // the following line initializes server tracing and entertains @Traced
        // annotations on Resource methods
        bootstrap.addBundle(this.haystackTracerBundle);
    }

    // If we need to trace client requests emanating from the app,
    // We need to register the clienttracingfeature with the client and then use this client to make the requests.
    @Override
    public void run(HelloWorldConfiguration helloWorldConfiguration,
                    Environment environment) {
        // the following line registers ClientTracingFeature to trace all
        // outbound service calls
        final Client client = ClientBuilder.newBuilder()
                .register(this.haystackTracerBundle.clientTracingFeature(environment))
                .build();

        environment.jersey().register(new Frontend(client));
    }
}
```

Also in the configuration yaml file for the application usually named config.yml, one needs to add the following snippet
with appropriate configs:

For local dispatch, means the traces will not be sent to the haystack server instead they will only be written to local file
```
tracer:
  serviceName: Backend
  enabled: true
  dispatchers:
    - type: logger
      loggerName: dispatcher
```

For remote dispatch, means the traces will be sent to the haystack server using the haystack client
```
tracer:
  serviceName: Backend
  enabled: true
  dispatchers:
    - type: remote
      client:
        type: agent
        host: localhost
        format:
          type: protobuf
```

To find the full example check this example of a simple application with client and server side tracing enabled,
[Haystack Dropwizard Example](https://github.com/ExpediaDotCom/haystack-dropwizard-example).

## See Also 
This library is built on top of [Haystack client java](https://github.com/ExpediaDotCom/haystack-client-java).

Further resources:  
[JAX-RS2]: https://jax-rs.github.io/apidocs/2.1/  
[Jersey]: https://jersey.java.net  
[Dropwizard]: http://www.dropwizard.io  
[Opentracing JAX-RS instrumentation]: https://github.com/opentracing-contrib/java-jaxrs  