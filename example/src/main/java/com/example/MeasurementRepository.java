package com.example;

import java.util.List;

public interface MeasurementRepository {

    void persist(Measurement measurement);

    List<Measurement> findAll();

}
