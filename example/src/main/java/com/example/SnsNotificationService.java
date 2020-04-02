package com.example;

import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import javax.inject.Inject;
import java.util.Map;

public class SnsNotificationService implements NotificationService {

    private String topicArn = System.getenv("TOPIC_ARN");

    @Inject
    private SnsClient snsClient;

    @Override
    public void send(String serialNumber, String message) {
        final PublishRequest publishRequest = PublishRequest
                .builder()
                .messageAttributes(Map.of("serial_number", MessageAttributeValue.builder().dataType("String").stringValue(serialNumber).build()))
                .message(message)
                .topicArn(topicArn)
                .build();
        final PublishResponse publish = snsClient.publish(publishRequest);
        System.out.println(publish.messageId());
    }
}
