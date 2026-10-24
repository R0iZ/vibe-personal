package com.maydings.vibelauncher;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Locale;

/**
 * Weather from Open-Meteo: no API key. Geocodes a city name when coordinates aren't already known.
 * https://open-meteo.com/
 */
final class OpenMeteoProvider implements WeatherProvider {

    private static final String FORECAST = "https://api.open-meteo.com/v1/forecast"
            + "?latitude=%s&longitude=%s"
            + "&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m,surface_pressure"
            + "&wind_speed_unit=ms&timezone=auto";
    private static final String GEOCODE = "https://geocoding-api.open-meteo.com/v1/search?name=%s&count=1"
            + "&language=%s";

    @Override
    public Weather fetch(WeatherQuery query) throws IOException {
        double lat;
        double lon;
        String city = query.city;
        if (query.latitude != null && query.longitude != null) {
            lat = query.latitude;
            lon = query.longitude;
            if (city.isEmpty()) city = "";   // widget can show temperature without a place name
        } else {
            if (city.isEmpty()) throw new IOException("no city");
            Geocoded g = geocode(city, query.language);
            lat = g.lat;
            lon = g.lon;
            city = g.name;
        }
        try {
            String url = String.format(Locale.US, FORECAST, lat, lon);
            JSONObject root = new JSONObject(Http.getText(url, Collections.emptyMap()));
            JSONObject cur = root.getJSONObject("current");
            int code = cur.optInt("weather_code", -1);
            Weather.Condition condition = fromWmo(code);
            String description = describeWmo(code, query.language);
            Integer humidity = cur.has("relative_humidity_2m") ? cur.getInt("relative_humidity_2m") : null;
            Float wind = cur.has("wind_speed_10m") ? (float) cur.getDouble("wind_speed_10m") : null;
            Float pressure = cur.has("surface_pressure") ? (float) cur.getDouble("surface_pressure") : null;
            return new Weather(city, (float) cur.getDouble("temperature_2m"), description, condition,
                    humidity, wind, pressure);
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }

    private static Geocoded geocode(String name, String language) throws IOException {
        try {
            String url = String.format(Locale.US, GEOCODE,
                    URLEncoder.encode(name, StandardCharsets.UTF_8.name()),
                    URLEncoder.encode(shortLang(language), StandardCharsets.UTF_8.name()));
            JSONObject root = new JSONObject(Http.getText(url, Collections.emptyMap()));
            JSONArray results = root.optJSONArray("results");
            if (results == null || results.length() == 0) throw new IOException("city not found: " + name);
            JSONObject r = results.getJSONObject(0);
            return new Geocoded(r.getDouble("latitude"), r.getDouble("longitude"), r.optString("name", name));
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }

    private static String shortLang(String tag) {
        int dash = tag.indexOf('-');
        return dash < 0 ? tag : tag.substring(0, dash);
    }

    static Weather.Condition fromWmo(int code) {
        if (code == 0) return Weather.Condition.CLEAR;
        if (code == 1 || code == 2 || code == 3) return Weather.Condition.CLOUDY;
        if (code == 45 || code == 48) return Weather.Condition.FOG;
        if (code >= 51 && code <= 57) return Weather.Condition.DRIZZLE;
        if (code >= 61 && code <= 67) return Weather.Condition.RAIN;
        if (code >= 71 && code <= 77) return Weather.Condition.SNOW;
        if (code >= 80 && code <= 82) return Weather.Condition.RAIN;
        if (code >= 85 && code <= 86) return Weather.Condition.SNOW;
        if (code >= 95 && code <= 99) return Weather.Condition.THUNDER;
        return Weather.Condition.UNKNOWN;
    }

    /** Short labels; Russian when the UI language is Russian, otherwise English. */
    static String describeWmo(int code, String language) {
        boolean ru = shortLang(language).equalsIgnoreCase("ru");
        if (code == 0) return ru ? "Ясно" : "Clear";
        if (code == 1) return ru ? "Преимущественно ясно" : "Mainly clear";
        if (code == 2) return ru ? "Переменная облачность" : "Partly cloudy";
        if (code == 3) return ru ? "Пасмурно" : "Overcast";
        if (code == 45 || code == 48) return ru ? "Туман" : "Fog";
        if (code >= 51 && code <= 57) return ru ? "Морось" : "Drizzle";
        if (code >= 61 && code <= 67) return ru ? "Дождь" : "Rain";
        if (code >= 71 && code <= 77) return ru ? "Снег" : "Snow";
        if (code >= 80 && code <= 82) return ru ? "Ливень" : "Showers";
        if (code >= 85 && code <= 86) return ru ? "Снегопад" : "Snow showers";
        if (code >= 95 && code <= 99) return ru ? "Гроза" : "Thunderstorm";
        return ru ? "Погода" : "Weather";
    }

    private static final class Geocoded {
        final double lat, lon;
        final String name;

        Geocoded(double lat, double lon, String name) {
            this.lat = lat;
            this.lon = lon;
            this.name = name;
        }
    }
}
