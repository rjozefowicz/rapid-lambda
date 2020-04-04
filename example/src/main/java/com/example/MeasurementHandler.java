package com.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2ProxyRequestEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.inject.Inject;
import java.util.Set;

public class MeasurementHandler implements RequestHandler<APIGatewayV2ProxyRequestEvent, Void> {

    @Inject
    private ObjectMapper objectMapper;

    @Inject
    private MeasurementRepository measurementRepository;

    @Inject
    private Set<NotificationService> notificationServices;

    public Void handleRequest(APIGatewayV2ProxyRequestEvent request, Context context) {
        try {
            final Measurement measurement = objectMapper.readValue(request.getBody(), Measurement.class);
            measurementRepository.persist(measurement);
            notificationServices.forEach(notificationService -> notificationService.send(measurement.getSerialNumber(), request.getBody()));
        } catch (JsonProcessingException e) {
            context.getLogger().log("Exception while processing request");
            e.printStackTrace();
        }
        return null;
    }
}
