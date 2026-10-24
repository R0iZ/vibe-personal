package com.maydings.vibelauncher;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

/** The launcher's own settings, entered under Settings > Vibe Launcher settings and kept on the device. */
final class VibeSettings {

    private static final String FILE = "vibe_settings";
    private static final String TMDB_KEY = "tmdbKey";
    private static final String YOUTUBE_KEY = "youtubeKey";
    private static final String PLEX_TOKEN = "plexToken";
    private static final String COLUMNS = "columns";
    private static final String LANGUAGE = "language";
    private static final String HIDE_INACTIVE_INPUTS = "hideInactiveInputs";
    private static final String AUTO_HIDE_BAR = "autoHideBar";
    private static final String WEATHER_PROVIDER = "weatherProvider";
    private static final String OWM_KEY = "openWeatherMapKey";
    private static final String WEATHER_CITY = "weatherCity";
    private static final String WALLPAPER_MODE = "wallpaperMode";
    private static final String WALLPAPER_URI = "wallpaperUri";
    private static final String WALLPAPER_FOLDER = "wallpaperFolder";
    private static final String WALLPAPER_LABEL = "wallpaperLabel";

    private final SharedPreferences prefs;

    VibeSettings(Context context) {
        prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    /** TMDB API key (v3) or read access token, or "" when not set. */
    String tmdbKey() {
        return prefs.getString(TMDB_KEY, "");
    }

    /** YouTube Data API key, or "" when not set. */
    String youtubeKey() {
        return prefs.getString(YOUTUBE_KEY, "");
    }

    /** Plex account token (X-Plex-Token), or "" when not set. */
    String plexToken() {
        return prefs.getString(PLEX_TOKEN, "");
    }

    /** Tiles per row on Home (4, 5 or 6), or the given default when not chosen. */
    int columns(int fallback) {
        int c = prefs.getInt(COLUMNS, fallback);
        return c >= 4 && c <= 6 ? c : fallback;
    }

    void setColumns(int columns) {
        prefs.edit().putInt(COLUMNS, columns).apply();
    }

    /** The chosen language's tag (see Lang), or "" to follow the TV's own. */
    String language() {
        return prefs.getString(LANGUAGE, "");
    }

    /** Saved at once (commit): Home is recreated in the new language straight after. */
    @SuppressLint("ApplySharedPref")
    void setLanguage(String tag) {
        prefs.edit().putString(LANGUAGE, tag).commit();
    }

    /** Whether the Inputs menu leaves out inputs with nothing connected. */
    boolean hideInactiveInputs() {
        return prefs.getBoolean(HIDE_INACTIVE_INPUTS, false);
    }

    void setHideInactiveInputs(boolean hide) {
        prefs.edit().putBoolean(HIDE_INACTIVE_INPUTS, hide).apply();
    }

    /**
     * Auto-hide (on by default): the top bar stays out of sight on the dock and with the picture alone, and
     * the rows stay down while the top bar has focus.
     */
    boolean autoHideBar() {
        return prefs.getBoolean(AUTO_HIDE_BAR, true);
    }

    void setAutoHideBar(boolean hide) {
        prefs.edit().putBoolean(AUTO_HIDE_BAR, hide).apply();
    }

    /**
     * Weather source id: {@link WeatherService#openMeteoId()} (default, no key) or
     * {@link WeatherService#openWeatherMapId()}.
     */
    String weatherProvider() {
        return prefs.getString(WEATHER_PROVIDER, WeatherService.openMeteoId());
    }

    void setWeatherProvider(String id) {
        prefs.edit().putString(WEATHER_PROVIDER, id == null ? WeatherService.openMeteoId() : id).apply();
    }

    /** OpenWeatherMap API key, or "" when not set. */
    String openWeatherMapKey() {
        return prefs.getString(OWM_KEY, "");
    }

    void setOpenWeatherMapKey(String key) {
        prefs.edit().putString(OWM_KEY, key == null ? "" : key.trim()).apply();
    }

    /**
     * City name for weather when location isn't available. Empty means use
     * {@link WeatherService#DEFAULT_CITY} as the fallback.
     */
    String weatherCity() {
        return prefs.getString(WEATHER_CITY, "");
    }

    void setWeatherCity(String city) {
        prefs.edit().putString(WEATHER_CITY, city == null ? "" : city.trim()).apply();
    }

    /** {@link WallpaperController#MODE_OFF}, {@code MODE_SINGLE} or {@code MODE_SLIDESHOW}. */
    int wallpaperMode() {
        return prefs.getInt(WALLPAPER_MODE, WallpaperController.MODE_OFF);
    }

    void setWallpaperMode(int mode) {
        prefs.edit().putInt(WALLPAPER_MODE, mode).apply();
    }

    /** Content or file URI of the single wallpaper image. */
    String wallpaperUri() {
        return prefs.getString(WALLPAPER_URI, "");
    }

    void setWallpaperUri(String uri) {
        prefs.edit().putString(WALLPAPER_URI, uri == null ? "" : uri).apply();
    }

    /** MediaStore bucket id or filesystem path for slideshow. */
    String wallpaperFolder() {
        return prefs.getString(WALLPAPER_FOLDER, "");
    }

    void setWallpaperFolder(String folderId) {
        prefs.edit().putString(WALLPAPER_FOLDER, folderId == null ? "" : folderId).apply();
    }

    /** Human-readable name of the chosen file or folder, for the settings summary. */
    String wallpaperLabel() {
        return prefs.getString(WALLPAPER_LABEL, "");
    }

    void setWallpaperLabel(String label) {
        prefs.edit().putString(WALLPAPER_LABEL, label == null ? "" : label).apply();
    }

    void save(String tmdbKey, String youtubeKey, String plexToken) {
        prefs.edit().putString(TMDB_KEY, tmdbKey.trim()).putString(YOUTUBE_KEY, youtubeKey.trim())
                .putString(PLEX_TOKEN, plexToken.trim()).apply();
    }

    /** Saves weather provider, OWM key and city together (from the Home tab). */
    void saveWeather(String provider, String owmKey, String city) {
        prefs.edit()
                .putString(WEATHER_PROVIDER, provider == null ? WeatherService.openMeteoId() : provider)
                .putString(OWM_KEY, owmKey == null ? "" : owmKey.trim())
                .putString(WEATHER_CITY, city == null ? "" : city.trim())
                .apply();
    }
}
