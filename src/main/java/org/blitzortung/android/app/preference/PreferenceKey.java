package org.blitzortung.android.app.preference;

import android.content.SharedPreferences;
import org.blitzortung.android.app.controller.LocationHandler;
import org.blitzortung.android.data.provider.DataProviderType;

import java.util.HashMap;
import java.util.Map;

public enum PreferenceKey {
    USERNAME("username", ""),
    PASSWORD("password", ""),
    RASTER_SIZE("raster_size", "10000"),
    MAP_TYPE("map_mode", "SATELLITE"),
    MAP_FADE("map_fade", "50"),
    COLOR_SCHEME("color_scheme", "blitzortung.org"),
    QUERY_PERIOD("query_period", "60"),
    BACKGROUND_QUERY_PERIOD("background_query_period", "0"),
    SHOW_PARTICIPANTS("show_participants", "false"),
    SHOW_LOCATION("location", "true"),
    ALARM_ENABLED("alarm_enabled", "false"),
    ALARM_SOUND_SIGNAL("alarm_sound_signal", ""),
    ALARM_VIBRATION_SIGNAL("alarm_vibration_signal", "10"),
    NOTIFICATION_DISTANCE_LIMIT("notification_distance_limit", "100"),
    SIGNALING_DISTANCE_LIMIT("signaling_distance_limit", "25"),
    REGION("region", "1"),
    DATA_SOURCE("data_source", DataProviderType.HTTP.toString()),
    MEASUREMENT_UNIT("measurement_unit", "km"),
    DO_NOT_SLEEP("do_not_sleep", "false"),
    INTERVAL_DURATION("interval_duration", "120"),
    HISTORIC_TIMESTEP("historic_timestep", "30"),
    LOCATION_MODE("location_mode", LocationHandler.LocationProvider.NETWORK.getType()),
    LOCATION_LONGITUDE("location_longitude", "11.0"),
    LOCATION_LATITUDE("location_latitude", "49.0");
    
    private final String key;
    private final String defaultValue;

    private PreferenceKey(String key, String defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
    }

    @Override
    public String toString()
    {
        return key;
    }

    private static final Map<String, PreferenceKey> stringToValueMap = new HashMap<String, PreferenceKey>();
    static {
        for (PreferenceKey key : PreferenceKey.values()) {
            String keyString = key.toString();
            if (stringToValueMap.containsKey(keyString)) {
                throw new IllegalStateException(String.format("key value '%s' already defined", keyString));
            }
            stringToValueMap.put(keyString, key);
        }
    }

    public static PreferenceKey fromString(String string) {
        return stringToValueMap.get(string);
    }

    public String getValue(SharedPreferences sharedPreferences) {
        return sharedPreferences.getString(key, defaultValue);
    }
}
