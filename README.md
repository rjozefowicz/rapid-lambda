### Introduction

Simple Java library to build native AWS Lambda functions that limit cold start issues for Java runtime. It uses Java 11 and adds support for AWS SDK V2, Jackson for JSON serialization and Guice as JSR-330 Dependency Injection provider.

Please note that this is still proof of concept.

### Usage

rapid-lambda still requires you to implement `RequestHandler` interface from `aws-lambda-java-core` that contains your function logic. You still should use event classes from `aws-lambda-java-events` if you consumes events from other AWS services. All events have reflection configuration already prepared to be used by `native-image` plugin.

Example bootstrap code:
```java
public class Function {

    public static void main(String[] args) {
        FunctionBootstrap.build(new MeasurementHandler(), APIGatewayProxyRequestEvent.class, FunctionConfiguration.newConfiguration(new Configuration())).bootstrap();
    }

}
```

Please see `example` directory for more details. It contains Function code that consumes measurement that will be persisted in DynamoDB and passed to another Lambda function and sent to SNS topic.

`native-image` requires reflection configuration for your classes that will be deserialized via ObjectMapper or injected via JSR-330 annotations.

In order to build executable you need to run the following command (`JAVA_HOME` points to GraalVM location):

```shell script
mvn clean package
$JAVA_HOME/bin/native-image -H:ReflectionConfigurationFiles=reflection-config.json -jar target/native-function-jar-with-dependencies.jar function.o
```

### Deployment on AWS Lambda

AWS Lambda custom runtime uses Amazon Linux 2 so in order to build native binary it has to be done on that OS e.g. on EC2 instance.

Running `example` on AWS Lambda: 
1. Create EC2 instance with Amazon Linux 2 and AWS CLI installed and configured
2. Create Lambda function with Custom runtime selected and with the following environment variables set:
* `TABLE_NAME` that points to DynamoDB table with serialNumber as Primary Key 
* `NOTIFICATION_FUNCTION_NAME` function to be invoked by `example` function
* `TOPIC_ARN` SNS topic that listens for measurements

Adjust Lambda function settings (timeout and memory) to your needs.

3. Execute the following commands on your EC2 instance
```shell script
// Preparing env
$ wget https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-20.0.0/graalvm-ce-java11-linux-amd64-20.0.0.tar.gz
$ tar xvzf graalvm-ce-java11-linux-amd64-20.0.0.tar.gz
$ cd graalvm-ce-java11-20.0.0/bin/
$ ./gu install native-image
$ export JAVA_HOME=/home/ec2-user/graalvm-ce-java11-20.0.0

// building example function
$ git clone https://github.com/rjozefowicz/rapid-lambda.git
$ cd rapid-lambda
$ mvn install
$ cd example
$ mvn package
$ $JAVA_HOME/bin/native-image -H:ReflectionConfigurationFiles=reflection-config.json -jar target/native-function-jar-with-dependencies.jar native-function.o

// building zip archive - collect all mandatory artifacts in single directory and zip them
$ mkdir build-artifacts
$ cp $JAVA_HOME/lib/libsunec.so build-artifacts/libsunec.so
$ cp $JAVA_HOME/lib/security/cacerts build-artifacts/cacerts
$ mv native-function.o build-artifacts/native-function.o
$ chmod +x build-artifacts/native-function.o
$ cp ../bootstrap build-artifacts/bootstrap
$ zip -r function .

// upload your binary to AWS Lambda function
$ aws lambda update-function-code --function-name YOUR_FUNCTION_NAME --zip-file fileb://build-artifacts/function.zip
```

### AWS SDK V2

Supported clients
* DynamoDB
* Lambda
* S3
* SQS
* SNS
* SSM
* EC2

In order to start using AWS Clients you just need to add maven dependency 

```xml
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>dynamodb</artifactId>
    <version>2.10.76</version>
</dependency>
```

and then inject client in your service class.

```java
@Inject
private DynamoDbClient dynamoDbClient;
```

### Dependency Injection

rapid-lambda partially supports JSR-330 Dependency Injection provided by Google implementation named Guice.

#### Available to inject without additional configuration

* HttpClient from Java 11
* ObjectMapper
* AWS SDK clients (if added as dependency)

#### Custom configuration:

Create binding configuration

```java
public class GlobalModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(Service.class).toInstance(new ServiceImpl());
    }
}
```

then Service can be injected via @Inject annotation

```java
@Inject
private Service service;
```

Injecting multiple instances of single interface:
```java
public class Configuration extends AbstractModule {
    @Override
    protected void configure() {
        Multibinder<NotificationService> multibinder = Multibinder.newSetBinder(binder(), NotificationService.class);
        multibinder.addBinding().toInstance(new SnsNotificationService());
        multibinder.addBinding().toInstance(new LambdaNotificationService());
    }
}
```

injection via
```java
@Inject
private Set<NotificationService> notificationServices;
```

##### Not supported:

binding interfaces to implementation classes e.g. 
```java
bind(Service.class).to(ServiceImpl.class);
```

It requires creating proxies via CGLIB that is not supported by native-image plugin. As alternative you might use direct binding to new class instances e.g.
```java
bind(Service.class).annotatedWith(Names.named("service1")).toInstance(new Service1());
bind(Service.class).annotatedWith(Names.named("service2")).toInstance(new Service2());
```

then it can be autowired with @Named annotation
```java
@Inject
@Named("service1")
private Service service;
```

As an alternative to Guice you might want to try Dagger 2 https://github.com/google/dagger as compile-time dependency injection library.

#### Native-image
Most of AWS services require HTTPS communication with their endpoints. In order to make it work with native binares generated by native-image libsunec.so and cacerts are added to custom runtime archive. More details https://quarkus.io/guides/native-and-ssl#the-sunec-library-and-friends

#### Debuging

native-image plugin is still a new tool in GraalVM portfolio and there is still room to improve. Many well-known libraries and frameworks from Java world are not supported. However there are some tricks that you may want to try if you face any issues with your code. You need to remember that your application is no longer pure AWS Lambda function but standalone Java application. GraalVM authors provide great tool to record all required configuration to build native binary called native-image-agent. You can launch your application as normal Java application with following configuration and it will create configuration files (Lambda endpoints simulator required, look above):
```shell script
java -agentlib:native-image-agent=config-output-dir=/tmp/native-image-config -jar target/native-function-jar-with-dependencies.jar
```

#### Simulating Lambda endpoints

Please read the following documentation https://docs.aws.amazon.com/lambda/latest/dg/runtimes-api.html

The quick way to write such simulator is to use SparkJava (http://sparkjava.com/) or Javalin (https://javalin.io/).

Example code using Javalin:

Maven dependency
```xml
<dependency>
    <groupId>io.javalin</groupId>
    <artifactId>javalin</artifactId>
    <version>3.8.0</version>
</dependency>
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-simple</artifactId>
    <version>1.7.28</version>
</dependency>
```

LambdaRuntimeSimulator.java
```java
public class LambdaRuntimeSimulator {

    private static final BlockingQueue<String> events = new LinkedBlockingQueue<>();

    public static void main(String[] args) {
        Javalin app = Javalin.create().start(7000);
        app.get("/2018-06-01/runtime/init/error", LambdaRuntimeSimulator::logResponse);
        app.get("/2018-06-01/runtime/invocation/next", LambdaRuntimeSimulator::nextInvocation);
        app.post("/2018-06-01/runtime/invocation/:invocationId/response", LambdaRuntimeSimulator::logResponse);
        app.post("/2018-06-01/runtime/invocation/:invocationId/error", LambdaRuntimeSimulator::logResponse);
        app.post("/events", LambdaRuntimeSimulator::newEvent);
    }

    private static void nextInvocation(Context ctx) throws InterruptedException {
        ctx.header("Lambda-Runtime-Aws-Request-Id", UUID.randomUUID().toString());
        ctx.result(events.take());
    }

    private static void logResponse(Context ctx) {
        System.out.println(String.format("Response from %s, body: %s", ctx.fullUrl(), ctx.body()));
    }
    
    private static void newEvent(Context ctx) {
        final String body = ctx.body();
        if (nonNull(body) && !body.isBlank()) {
            events.add(body);
        }
    }

}
```

This code exposes HTTP POST `/events` endpoint that accepts next Lambda event. 

Custom runtimes expects `AWS_LAMBDA_RUNTIME_API` environment variable to point to Lambda HTTP API. For above example it should point to `localhost:7000`
