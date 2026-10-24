package com.maydings.vibelauncher;

import java.io.IOException;

/**
 * A weather source the home widget can use. Add another implementation and wire it in
 * {@link WeatherService} when a new API is needed; the widget stays the same.
 */
interface WeatherProvider {

    /** Fetches current conditions for this place. Worker threads only. */
    Weather fetch(WeatherQuery query) throws IOException;
}
