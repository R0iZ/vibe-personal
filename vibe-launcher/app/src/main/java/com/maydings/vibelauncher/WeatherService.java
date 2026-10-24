package com.maydings.vibelauncher;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Looper;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Picks a {@link WeatherProvider} from settings, resolves location (GPS when allowed, else the
 * configured city), and refreshes the home widget on a timer. Photos and weather stay local to the
 * launcher UI — nothing is uploaded.
 */
final class WeatherService {

    interface Listener {
        void onWeather(Weather weather);
        void onWeatherError();
    }

    /** Default city when location isn't available and the user hasn't set one (Moscow). */
    static final String DEFAULT_CITY = "Moscow";

    private static final long REFRESH_MS = 15 * 60 * 1000L;
    private static final String PROVIDER_OPEN_METEO = "open_meteo";
    private static final String PROVIDER_OWM = "openweathermap";

    private final Context app;
    private final VibeSettings settings;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "weather");
        t.setDaemon(true);
        return t;
    });
    private Listener listener;
    private final Runnable tick = this::refreshNow;
    private boolean started;

    WeatherService(Context context, VibeSettings settings) {
        this.app = context.getApplicationContext();
        this.settings = settings;
    }

    void setListener(Listener listener) {
        this.listener = listener;
    }

    void start() {
        if (started) return;
        started = true;
        refreshNow();
    }

    void stop() {
        started = false;
        main.removeCallbacks(tick);
    }

    void close() {
        stop();
        worker.shutdownNow();
    }

    /** Re-reads settings and fetches again (after Save in the dialog). */
    void refreshNow() {
        main.removeCallbacks(tick);
        if (!started) return;
        worker.execute(() -> {
            Weather weather = null;
            try {
                weather = provider().fetch(query());
            } catch (Exception ignored) {
            }
            Weather result = weather;
            main.post(() -> {
                if (!started) return;
                if (result != null && listener != null) listener.onWeather(result);
                else if (listener != null) listener.onWeatherError();
                main.postDelayed(tick, REFRESH_MS);
            });
        });
    }

    static boolean isOpenWeatherMap(String id) {
        return PROVIDER_OWM.equals(id);
    }

    static String openMeteoId() {
        return PROVIDER_OPEN_METEO;
    }

    static String openWeatherMapId() {
        return PROVIDER_OWM;
    }

    private WeatherProvider provider() {
        String id = settings.weatherProvider();
        if (PROVIDER_OWM.equals(id) && !settings.openWeatherMapKey().isEmpty()) {
            return new OpenWeatherMapProvider(settings.openWeatherMapKey());
        }
        // Default and fallback: Open-Meteo needs no key.
        return new OpenMeteoProvider();
    }

    private WeatherQuery query() {
        String city = settings.weatherCity().trim();
        String lang = languageTag();
        Location loc = lastLocation();
        if (loc != null) {
            // Prefer a configured city name for display; coordinates still refine the forecast.
            String label = city.isEmpty() ? "" : city;
            return new WeatherQuery(label, loc.getLatitude(), loc.getLongitude(), lang);
        }
        if (city.isEmpty()) city = DEFAULT_CITY;
        return new WeatherQuery(city, null, null, lang);
    }

    private String languageTag() {
        String chosen = settings.language();
        if (!chosen.isEmpty()) return chosen;
        Locale locale = app.getResources().getConfiguration().getLocales().get(0);
        return locale != null ? locale.toLanguageTag() : "en";
    }

    private Location lastLocation() {
        if (app.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && app.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return null;
        }
        LocationManager lm = app.getSystemService(LocationManager.class);
        if (lm == null) return null;
        Location best = null;
        for (String provider : lm.getProviders(true)) {
            try {
                Location l = lm.getLastKnownLocation(provider);
                if (l == null) continue;
                if (best == null || l.getTime() > best.getTime()) best = l;
            } catch (SecurityException ignored) {
            }
        }
        return best;
    }
}
