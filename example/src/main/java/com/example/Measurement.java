package com.example;

public class Measurement {

    private String serialNumber;
    private double temperature;
    private double pressure;

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public double getPressure() {
        return pressure;
    }

    public void setPressure(double pressure) {
        this.pressure = pressure;
    }

    @Override
    public String toString() {
        return "Measurement{" +
                "serialNumber='" + serialNumber + '\'' +
                ", temperature=" + temperature +
                ", pressure=" + pressure +
                '}';
    }
}
