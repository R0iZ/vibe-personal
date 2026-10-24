package com.maydings.vibelauncher;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Locale;

/**
 * Weather from OpenWeatherMap Current Weather API. Needs an API key from the settings dialog.
 * https://openweathermap.org/current
 */
final class OpenWeatherMapProvider implements WeatherProvider {

    private static final String BY_CITY = "https://api.openweathermap.org/data/2.5/weather?q=%s&appid=%s"
            + "&units=metric&lang=%s";
    private static final String BY_COORDS = "https://api.openweathermap.org/data/2.5/weather?lat=%s&lon=%s"
            + "&appid=%s&units=metric&lang=%s";

    private final String apiKey;

    OpenWeatherMapProvider(String apiKey) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
    }

    @Override
    public Weather fetch(WeatherQuery query) throws IOException {
        if (apiKey.isEmpty()) throw new IOException("OpenWeatherMap key not set");
        String lang = shortLang(query.language);
        String url;
        try {
            if (query.latitude != null && query.longitude != null) {
                url = String.format(Locale.US, BY_COORDS, query.latitude, query.longitude, apiKey, lang);
            } else if (!query.city.isEmpty()) {
                url = String.format(Locale.US, BY_CITY,
                        URLEncoder.encode(query.city, StandardCharsets.UTF_8.name()), apiKey, lang);
            } else {
                throw new IOException("no city");
            }
            JSONObject root = new JSONObject(Http.getText(url, Collections.emptyMap()));
            JSONObject main = root.getJSONObject("main");
            JSONObject wind = root.optJSONObject("wind");
            JSONObject weather0 = root.getJSONArray("weather").getJSONObject(0);
            String city = root.optString("name", query.city);
            String description = weather0.optString("description", "");
            if (!description.isEmpty()) {
                description = Character.toUpperCase(description.charAt(0)) + description.substring(1);
            }
            Weather.Condition condition = fromOwm(weather0.optString("main", ""), weather0.optInt("id", 0));
            Integer humidity = main.has("humidity") ? main.getInt("humidity") : null;
            Float windMs = wind != null && wind.has("speed") ? (float) wind.getDouble("speed") : null;
            Float pressure = main.has("pressure") ? (float) main.getDouble("pressure") : null;
            return new Weather(city, (float) main.getDouble("temp"), description, condition,
                    humidity, windMs, pressure);
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }

    private static String shortLang(String tag) {
        int dash = tag.indexOf('-');
        return dash < 0 ? tag : tag.substring(0, dash);
    }

    static Weather.Condition fromOwm(String main, int id) {
        String m = main == null ? "" : main.toLowerCase(Locale.US);
        if (m.contains("thunder")) return Weather.Condition.THUNDER;
        if (m.contains("drizzle")) return Weather.Condition.DRIZZLE;
        if (m.contains("rain")) return Weather.Condition.RAIN;
        if (m.contains("snow")) return Weather.Condition.SNOW;
        if (m.contains("mist") || m.contains("fog") || m.contains("haze") || m.contains("smoke")) {
            return Weather.Condition.FOG;
        }
        if (m.contains("cloud")) return Weather.Condition.CLOUDY;
        if (m.contains("clear")) return Weather.Condition.CLEAR;
        // Fallback on the numeric id groups OpenWeather documents.
        if (id >= 200 && id < 300) return Weather.Condition.THUNDER;
        if (id >= 300 && id < 400) return Weather.Condition.DRIZZLE;
        if (id >= 500 && id < 600) return Weather.Condition.RAIN;
        if (id >= 600 && id < 700) return Weather.Condition.SNOW;
        if (id >= 700 && id < 800) return Weather.Condition.FOG;
        if (id == 800) return Weather.Condition.CLEAR;
        if (id > 800) return Weather.Condition.CLOUDY;
        return Weather.Condition.UNKNOWN;
    }
}
