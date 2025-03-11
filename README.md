# lammy [![Live Tests](https://github.com/aleph0io/lammy/actions/workflows/live-tests.yml/badge.svg)](https://github.com/aleph0io/lammy/actions/workflows/live-tests.yml) [![CodeQL](https://github.com/aleph0io/lammy/actions/workflows/github-code-scanning/codeql/badge.svg)](https://github.com/aleph0io/lammy/actions/workflows/github-code-scanning/codeql)

Lammy is a microframework for building AWS Lambda functions in Java 8+.

## Goals

* Make it easier to build and maintain Lambda functions using Java 8+
* Keep JAR size small

## Non-Goals

* Provide implementations of common use cases (e.g., "copy file to S3")
* Build a framework for microservices in general (i.e., don't rebuild [Quarkus](https://quarkus.io/) or [Micronaut](https://micronaut.io/))

## Quickstart

To create a simple Lambda function with Lammy, add the following dependency to your project:

    <dependency>
        <groupId>io.aleph0</groupId>
        <artifactId>lammy-core</artifactId>
        <version>1.0.0-B0</version>
    </dependency>
    
Next, add the following code to your project:

    /**
     * The JSON-serializable request object
     */
    public class Request {
      private String name;
        
      public String getName() {
        return name;
      }
        
      public void setName(String name) {
        this.name = name;
      }
    }
    
    /**
     * The JSON-serializable response object
     */
    public class Response {
      private String greeting;
        
      public String getGreeting() {
        return greeting;
      }
        
      public void setGreeting(String greeting) {
        this.greeting = greeting;
      }
    }
    
    /**
     * The lambda function implementation
     */
    public class ExampleLambdaFunction
        extends BeanLambdaProcessorBase<Request, Response> {
      @Override
      public Response handleBeanRequest(Request request, Context context) {
        if(request.getName() == null)
          throw new IllegalArgumentException("name is required");
      
          final String name = request.getName();
          
          final String greeting = "Hello, " + getName() + "!";
          
          final Response response=new Response();
          response.setGreeting(greeting);
          
          return response;
      }
    }
    
Congratulations! You have created a working AWS Lambda function.

## Design Philosophy

Lammy's design is heavily influenced by [JAX-RS](https://jakarta.ee/specifications/restful-ws/). Specifically, the framework implement a straightforward, mechanical control flow that centralizes business logic while exposing hooks that libraries and applications can use for things like customization, serialization, exception handling, and so on. Hopefully, Lammy's design feels clear and familiar.

## Model

Lammy uses the following terms to talk about Lambda functions.

### Logical function styles

There are two logical function styles:

* Processor -- a Lambda function that takes an input and produces an output. Example: Given an entity ID, fetch the corresponding entity from a data store and return it.
* Consumer -- a Lambda function that takes an input, but produces no logical output. (Of course, all Lambda functions must return an output per the Lambda API, but consumer outputs are semantically void.) Example: Given an entity update, apply the given update to a data store, then exit.

### Implementation function styles

#### Stream functions

A "stream function" is any AWS Lambda function that operates on byte streams (e.g., `InputStream` and `OutputStream`).  These correspond to [`RequestStreamHandler`](https://javadoc.io/doc/com.amazonaws/aws-lambda-java-core/latest/com/amazonaws/services/lambda/runtime/RequestStreamHandler.html) functions.

Stream-style functions support **interceptors**, which are plugin-style objects that can pre-process function input streams and post-process function output streams, for example to perform base64 encoding and decoding.

Additionally, stream-style functions support **exception writers**, which are plugin-style objects that act as exception handlers for specific exception types. An exception writer can propagate the exception, which is treated as an error by the Lambda runtime; throw a different exception, which is treated as an error by the Lambda runtime; or write a message to the output stream, which is treated as a success by the Lambda runtime.

Lammy provides `StreamLambdaProcessorBase` and `StreamLambdaConsumerBase` for processor and consumer stream-style functions, respectively.

#### Bean functions

A "bean function" is any AWS Lambda function that operates on de/serialized Java bean objects (POJOs) as input/output, as opposed to streams where the serialization is handled inside the Lambda runtime. These correspond to [`RequestHandler`](https://javadoc.io/doc/com.amazonaws/aws-lambda-java-core/latest/com/amazonaws/services/lambda/runtime/RequestHandler.html) functions.

Bean-style functions support **filters**, which are plugin-style objects that can pre-process function inputs and post-process function outputs, for example to perform object validation.

Additionally, bean-style functions support **exception mappers**, which are plugin-style objects that act as exception handlers for specific exception types. An exception writer can propagate the exception, which is treated as an error by the Lambda runtime; throw a different exception, which is treated as an error by the Lambda runtime; or return a message of the same type as the function response type, which is then handled as the function output and treated as a success by the Lambda runtime.

Lammy provides `BeanLambdaProcessorBase` and `BeanLambdaConsumerBase` for processor and consumer bean-style functions, respectively.

#### Streamed bean functions

A "streamed bean function" is any AWS Lambda function that operates on de/serialized Java bean objects (POJOs) as input/output as opposed to streams where the serialization is handled in the function implementation itself. The implementation uses a stream-style [`RequestStreamHandler`](https://javadoc.io/doc/com.amazonaws/aws-lambda-java-core/latest/com/amazonaws/services/lambda/runtime/RequestStreamHandler.html) to interface with the Lambda runtime, then performs serialization to expose a bean-style programming interface for the user.

Because they perform their serialization manually, streamed bean-style functions support both interceptors and filters. This gives users maximum control over how I/O is encoded over the wire and how objects are de/serialized for processing.

Streamed bean functions also support exception mappers.

Lammy provides `StreamedBeanLambdaProcessorBase` and `StreamedBeanLambdaConsumerBase` for processor and consumer bean-style functions, respectively.

## Service loading

Lammy makes heavy use of Java's `ServiceLoader` to simplify the developer's life. The following types can all be loaded automatically using the `ServiceLoader`:

* `CustomPojoSerializer` -- For bean functions and streamed bean functions
* `ContextAwareCustomPojoSerializer` -- For streamed bean functions
* `RequestFilter`, `ResponseFilter` -- For bean functions and streamed bean functions
* `InputInterceptor`, `OutputInterceptor` -- For stream functions and streamed bean functions
* `ExceptionWriter` -- For stream functions
* `ExceptionMapper` -- For bean functions and streamed bean functions

The `CustomPojoSerializer` and `ContextAwareCustomPojoSerializer` objects are always loaded automatically if present.

To load all other objects automatically via `ServiceLoader`, the user must do one of two things:

* Set the environment variable `LAMMY_AUTOLOAD_ALL=TRUE`
* Set individual types to autoload using function base's corresponding configuration object (e.g., `BeanLambdaProcessorConfiguration`) in the function's constructor

## A note on building

When building lammy from source, you may notice tests failing, especially with timeout messages indicating that LocalStack has stopped responding to requests. Try running the offending tests individually like this:

    JAVA_HOME="/Library/Java/JavaVirtualMachines/amazon-corretto-8.jdk/Contents/Home" mvn -pl lammy-test test -Dtest='BeanLambdaFunctionIntegrationTest#givenExceptionMapperServicesAB_whenAutoloadExplicitlyEnabledAndThrowNonMatching_thenPropagate

If the tests pass when run individually but fail when run in the context of the larger build, then then it's likely that the underlying LocalStack instance is running out of memory and crashing. In this situation, try increasing the memory limit for your container engine. For example, in Docker Desktop, increasing the memory limit by going to Settings &rarr; Resources &rarr; Memory Limit to at least 4GB.

