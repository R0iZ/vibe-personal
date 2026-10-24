package com.maydings.vibelauncher;

/**
 * One weather snapshot for the home widget. Built by a {@link WeatherProvider}; the UI never talks to
 * an API itself.
 */
final class Weather {

    /** How the sky looks, shared across providers so icons stay the same. */
    enum Condition {
        CLEAR, CLOUDY, FOG, DRIZZLE, RAIN, SNOW, THUNDER, UNKNOWN
    }

    final String city;
    final float temperatureC;
    final String description;
    final Condition condition;
    final Integer humidityPercent;   // null when the provider didn't send it
    final Float windMs;
    final Float pressureHpa;

    Weather(String city, float temperatureC, String description, Condition condition,
            Integer humidityPercent, Float windMs, Float pressureHpa) {
        this.city = city;
        this.temperatureC = temperatureC;
        this.description = description;
        this.condition = condition;
        this.humidityPercent = humidityPercent;
        this.windMs = windMs;
        this.pressureHpa = pressureHpa;
    }
}
