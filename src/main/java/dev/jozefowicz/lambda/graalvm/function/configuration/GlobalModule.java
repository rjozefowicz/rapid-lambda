package dev.jozefowicz.lambda.graalvm.function.configuration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.google.inject.AbstractModule;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.utils.builder.SdkBuilder;

import java.lang.reflect.Method;
import java.net.http.HttpClient;
import java.util.Optional;

public class GlobalModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(ObjectMapper.class).toInstance(objectMapper());
        bind(HttpClient.class).toInstance(HttpClient.newHttpClient());
        bindIfAvailable("software.amazon.awssdk.services.dynamodb.DynamoDbClient").ifPresent(this::initializeAwsSdkClient);
        bindIfAvailable("software.amazon.awssdk.services.s3.S3Client").ifPresent(this::initializeAwsSdkClient);
        bindIfAvailable("software.amazon.awssdk.services.sqs.SqsClient").ifPresent(this::initializeAwsSdkClient);
        bindIfAvailable("software.amazon.awssdk.services.lambda.LambdaClient").ifPresent(this::initializeAwsSdkClient);
        bindIfAvailable("software.amazon.awssdk.services.ec2.Ec2Client").ifPresent(this::initializeAwsSdkClient);
        bindIfAvailable("software.amazon.awssdk.services.ssm.SsmClient").ifPresent(this::initializeAwsSdkClient);
        bindIfAvailable("software.amazon.awssdk.services.sns.SnsClient").ifPresent(this::initializeAwsSdkClient);
    }

    private void initializeAwsSdkClient(Class clazz) {
        try {
            final Method builderMethod = clazz.getMethod("builder");
            final Object builder = builderMethod.invoke(null, null);
            final Method httpClientBuilderMethod = builder.getClass().getDeclaredMethod("httpClientBuilder", SdkHttpClient.Builder.class);
            httpClientBuilderMethod.setAccessible(true);
            final SdkBuilder withUrlConnection = (SdkBuilder) httpClientBuilderMethod.invoke(builder, UrlConnectionHttpClient.builder());
            bind(clazz).toInstance(withUrlConnection.build());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private ObjectMapper objectMapper() {
        final var objectMapper = new ObjectMapper();
        objectMapper.registerModule(new ParameterNamesModule(JsonCreator.Mode.PROPERTIES));
        return objectMapper;
    }

    private Optional<Class> bindIfAvailable(String className) {
        try {
            return Optional.ofNullable(Class.forName(className));
        } catch(ClassNotFoundException e) {
            return Optional.empty();
        }
    }

}
