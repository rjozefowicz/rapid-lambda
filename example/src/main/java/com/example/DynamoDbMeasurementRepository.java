package com.example;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;

public class DynamoDbMeasurementRepository implements MeasurementRepository {

    private final String tableName = System.getenv("TABLE_NAME");

    @Inject
    private DynamoDbClient dynamoDbClient;

    @Override
    public void persist(final Measurement measurement) {
        final PutItemRequest putItemRequest = PutItemRequest
                .builder()
                .tableName(tableName)
                .item(buildItem(measurement))
                .build();
        dynamoDbClient.putItem(putItemRequest);
    }

    @Override
    public List<Measurement> findAll() {
        final ScanResponse response = dynamoDbClient.scan(ScanRequest.builder().tableName(tableName).build());
        return response.items().stream().map(this::buildMeasurement).collect(Collectors.toList());
    }

    private Map<String, AttributeValue> buildItem(final Measurement measurement) {
        return Map.of(
                "serialNumber", AttributeValue.builder().s(measurement.getSerialNumber()).build(),
                "pressure", AttributeValue.builder().n(Double.toString(measurement.getPressure())).build(),
                "temperature", AttributeValue.builder().n(Double.toString(measurement.getTemperature())).build()
        );
    }

    private Measurement buildMeasurement(final Map<String, AttributeValue> item) {
        final Measurement measurement = new Measurement();
        measurement.setSerialNumber(item.get("serialNumber").s());
        final AttributeValue pressure = item.get("pressure");
        if (nonNull(pressure)) {
            measurement.setPressure(Double.valueOf(pressure.n()));
        }
        final AttributeValue temperature = item.get("temperature");
        if (nonNull(temperature)) {
            measurement.setTemperature(Double.valueOf(temperature.n()));
        }
        return measurement;
    }
}
