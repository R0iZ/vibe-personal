package com.maydings.vibelauncher;

/** Where and in which language to ask a {@link WeatherProvider}. */
final class WeatherQuery {

    /** Display / search name; may be empty when only coordinates are known. */
    final String city;
    /** Null when the city must be resolved by name. */
    final Double latitude;
    final Double longitude;
    /** BCP 47 language tag for descriptions when the API supports it (e.g. "ru"). */
    final String language;

    WeatherQuery(String city, Double latitude, Double longitude, String language) {
        this.city = city == null ? "" : city;
        this.latitude = latitude;
        this.longitude = longitude;
        this.language = language == null || language.isEmpty() ? "en" : language;
    }
}
