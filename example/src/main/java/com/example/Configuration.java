package com.example;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

public class Configuration extends AbstractModule {
    @Override
    protected void configure() {
        bind(MeasurementRepository.class).toInstance(new DynamoDbMeasurementRepository());
        Multibinder<NotificationService> multibinder = Multibinder.newSetBinder(binder(), NotificationService.class);
        multibinder.addBinding().toInstance(new SnsNotificationService());
        multibinder.addBinding().toInstance(new LambdaNotificationService());
    }
}
