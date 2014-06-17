package org.blitzortung.android.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.*;
import android.preference.PreferenceManager;
import android.util.Log;
import org.blitzortung.android.alert.AlertParameters;
import org.blitzortung.android.alert.event.AlertEvent;
import org.blitzortung.android.alert.AlertHandler;
import org.blitzortung.android.alert.factory.AlertObjectFactory;
import org.blitzortung.android.location.LocationEvent;
import org.blitzortung.android.location.LocationHandler;
import org.blitzortung.android.app.controller.NotificationHandler;
import org.blitzortung.android.app.view.PreferenceKey;
import org.blitzortung.android.data.DataChannel;
import org.blitzortung.android.data.DataHandler;
import org.blitzortung.android.data.provider.result.DataEvent;
import org.blitzortung.android.data.provider.result.StatusEvent;
import org.blitzortung.android.protocol.Consumer;
import org.blitzortung.android.protocol.ListenerContainer;
import org.blitzortung.android.util.Period;

import java.util.HashSet;
import java.util.Set;

public class AppService extends Service implements Runnable, SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String RETRIEVE_DATA_ACTION = "retrieveData";
    public static final String WAKE_LOCK_TAG = "boAndroidWakeLock";

    private final Handler handler;

    private int period;

    private int backgroundPeriod;

    private final Period updatePeriod;

    private boolean updateParticipants;

    private boolean enabled;

    private DataHandler dataHandler;
    private AlertHandler alertHandler;
    private boolean alertEnabled;
    private LocationHandler locationHandler;

    private final IBinder binder = new DataServiceBinder();

    private AlarmManager alarmManager;

    private PendingIntent pendingIntent;

    private PowerManager.WakeLock wakeLock;

    ListenerContainer<DataEvent> dataListenerContainer = new ListenerContainer<DataEvent>() {
        @Override
        public void addedFirstConsumer() {
            resumeDataService();
        }

        @Override
        public void removedLastConsumer() {
            suspendDataService();
        }
    };

    ListenerContainer<AlertEvent> alertListenerContainer = new ListenerContainer<AlertEvent>() {
        @Override
        public void addedFirstConsumer() {
        }

        @Override
        public void removedLastConsumer() {
        }
    };

    @SuppressWarnings("UnusedDeclaration")
    public AppService() {
        this(new Handler(), new Period());
        Log.d(Main.LOG_TAG, "AppService() created with new handler");
    }

    protected AppService(Handler handler, Period updatePeriod) {
        Log.d(Main.LOG_TAG, "AppService() create");
        this.handler = handler;
        this.updatePeriod = updatePeriod;
    }

    public int getPeriod() {
        return period;
    }

    public int getBackgroundPeriod() {
        return backgroundPeriod;
    }

    public long getLastUpdate() {
        return updatePeriod.getLastUpdateTime();
    }

    public void reloadData() {
        dataListenerContainer.broadcast(DataHandler.CLEAR_DATA_EVENT);

        if (isEnabled()) {
            restart();
        } else {
            Set<DataChannel> updateTargets = new HashSet<DataChannel>();
            updateTargets.add(DataChannel.STROKES);
            dataHandler.updateData(updateTargets);
        }
    }

    public DataHandler getDataHandler() {
        return dataHandler;
    }

    public AlertHandler getAlertHandler() {
        return alertHandler;
    }

    private final Consumer<DataEvent> dataEventConsumer = new Consumer<DataEvent>() {
        @Override
        public void consume(DataEvent event) {
            if (!dataListenerContainer.isEmpty()) {
                dataListenerContainer.storeAndBroadcast(event);
            }
            if (alertEnabled) {
                alertHandler.getDataEventConsumer().consume(event);
            }
            releaseWakeLock();
        }
    };

    public ListenerContainer<DataEvent> getDataListenerContainer() {
        return dataListenerContainer;
    }

    private final Consumer<AlertEvent> alertEventConsumer = new Consumer<AlertEvent>() {
        @Override
        public void consume(AlertEvent event) {
            alertListenerContainer.broadcast(event);
        }
    };

    public ListenerContainer<AlertEvent> getAlertListenerContainer() {
        return alertListenerContainer;
    }

    public void addDataConsumer(Consumer<DataEvent> dataConsumer) {
        dataListenerContainer.addListener(dataConsumer);
    }

    public void removeDataConsumer(Consumer<DataEvent> dataConsumer) {
        dataListenerContainer.removeListener(dataConsumer);
    }

    public void addAlertConsumer(Consumer<AlertEvent> alertConsumer) {
        alertListenerContainer.addListener(alertConsumer);
    }

    public void removeAlertListener(Consumer<AlertEvent> alertConsumer) {
        alertListenerContainer.removeListener(alertConsumer);
    }

    public void removeLocationListener(Consumer<LocationEvent> locationConsumer) {
        locationHandler.removeUpdates(locationConsumer);
    }

    public void addLocationListener(Consumer<LocationEvent> locationListener) {
        locationHandler.requestUpdates(locationListener);
    }

    public AlertEvent getAlertEvent() {
        return alertHandler.getAlertEvent();
    }

    public class DataServiceBinder extends Binder {
        AppService getService() {
            Log.d(Main.LOG_TAG, "DataServiceBinder.getService() " + AppService.this);
            return AppService.this;
        }
    }

    @Override
    public void onCreate() {
        Log.i(Main.LOG_TAG, "AppService.onCreate()");
        super.onCreate();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferences.registerOnSharedPreferenceChangeListener(this);

        if (wakeLock == null) {
            Log.d(Main.LOG_TAG, "AppService.onCreate() create wakelock");
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG);
        }

        if (dataHandler == null) {
            dataHandler = new DataHandler(wakeLock, preferences, getPackageInfo());
            dataHandler.setDataListener(dataEventConsumer);
        }

        locationHandler = new LocationHandler(this, preferences);
        AlertParameters alertParameters = new AlertParameters();
        alertParameters.updateSectorLabels(this);
        alertHandler = new AlertHandler(locationHandler, preferences, this,
                (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE),
                new NotificationHandler(this),
                new AlertObjectFactory(), alertParameters);

        onSharedPreferenceChanged(preferences, PreferenceKey.QUERY_PERIOD);
        onSharedPreferenceChanged(preferences, PreferenceKey.ALERT_ENABLED);
        onSharedPreferenceChanged(preferences, PreferenceKey.BACKGROUND_QUERY_PERIOD);
        onSharedPreferenceChanged(preferences, PreferenceKey.SHOW_PARTICIPANTS);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(Main.LOG_TAG, "AppService.onStartCommand() startId: " + startId + " " + intent);

        if (intent != null && RETRIEVE_DATA_ACTION.equals(intent.getAction())) {
            acquireWakeLock();

            Log.v(Main.LOG_TAG, "AppService.onStartCommand() acquired wake lock " + wakeLock);

            handler.removeCallbacks(this);
            handler.post(this);
        }

        return START_STICKY;
    }

    private void acquireWakeLock() {
        wakeLock.acquire();
    }

    public void releaseWakeLock() {
        if (wakeLock.isHeld()) {
            try {
                wakeLock.release();
                Log.v(Main.LOG_TAG, "AppService.releaseWakeLock() " + wakeLock);
            } catch (RuntimeException e) {
                Log.v(Main.LOG_TAG, "AppService.releaseWakeLock() failed", e);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(Main.LOG_TAG, "AppService.onBind() " + intent);

        return binder;
    }

    @Override
    public void run() {

        if (dataListenerContainer.isEmpty()) {
            Log.v(Main.LOG_TAG, "AppService.run() in background");

            dataHandler.updateDatainBackground();
        } else {
            releaseWakeLock();

            long currentTime = Period.getCurrentTime();
            if (dataHandler != null) {
                Set<DataChannel> updateTargets = new HashSet<DataChannel>();

                if (updatePeriod.shouldUpdate(currentTime, period)) {
                    updatePeriod.setLastUpdateTime(currentTime);
                    updateTargets.add(DataChannel.STROKES);

                    if (updateParticipants && updatePeriod.isNthUpdate(10)) {
                        updateTargets.add(DataChannel.PARTICIPANTS);
                    }
                }

                if (!updateTargets.isEmpty()) {
                    dataHandler.updateData(updateTargets);
                }

                final String statusString = "" + updatePeriod.getCurrentUpdatePeriod(currentTime, period) + "/" + period;
                dataListenerContainer.broadcast(new StatusEvent(statusString));
            }
            // Schedule the next update
            handler.postDelayed(this, 1000);
        }
    }

    public void restart() {
        updatePeriod.restart();
        enable();
    }

    public void resumeDataService() {
        if (dataHandler.isRealtime()) {
            Log.v(Main.LOG_TAG, "AppService.resumeDataService() realtime data");
            enable();
        } else {
            Log.v(Main.LOG_TAG, "AppService.resumeDataService() historic data");
        }

        discardAlarm();
    }

    public boolean suspendDataService() {
        Log.v(Main.LOG_TAG, "AppService.suspendDataService()");
        disable();

        if (backgroundPeriod == 0) {
            locationHandler.removeUpdates(alertHandler.getLocationEventConsumer());
        } else {
            createAlarm();
        }

        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(Main.LOG_TAG, "AppService.onDestroy()");
    }

    public void enable() {
        enabled = true;
        handler.removeCallbacks(this);
        handler.post(this);
    }

    public boolean isEnabled() {
        return enabled;
    }

    protected void disable() {
        handler.removeCallbacks(this);
        enabled = false;
    }

    public void setDataHandler(DataHandler dataHandler) {
        this.dataHandler = dataHandler;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String keyString) {
        onSharedPreferenceChanged(sharedPreferences, PreferenceKey.fromString(keyString));
    }

    private void onSharedPreferenceChanged(SharedPreferences sharedPreferences, PreferenceKey key) {
        switch (key) {
            case ALERT_ENABLED:
                alertEnabled = sharedPreferences.getBoolean(key.toString(), false);

                if (dataListenerContainer.isEmpty() && backgroundPeriod > 0) {
                    if (alertEnabled) {
                        alertHandler.setAlertListener(alertEventConsumer);
                        createAlarm();
                    } else {
                        alertHandler.unsetAlertListener();
                        discardAlarm();
                    }
                }
                break;

            case QUERY_PERIOD:
                period = Integer.parseInt(sharedPreferences.getString(key.toString(), "60"));
                break;

            case BACKGROUND_QUERY_PERIOD:
                int previousBackgroundPeriod = backgroundPeriod;
                backgroundPeriod = Integer.parseInt(sharedPreferences.getString(key.toString(), "0"));

                if (dataListenerContainer.isEmpty() && alertEnabled) {
                    if (previousBackgroundPeriod == 0 && backgroundPeriod > 0) {
                        Log.v(Main.LOG_TAG, String.format("AppService.onSharedPreferenceChanged() create alarm with backgroundPeriod=%d", backgroundPeriod));
                        alertHandler.setAlertListener(alertEventConsumer);
                        createAlarm();
                    } else if (previousBackgroundPeriod > 0 && backgroundPeriod == 0) {
                        Log.v(Main.LOG_TAG, String.format("AppService.onSharedPreferenceChanged() discard alarm", backgroundPeriod));
                        alertHandler.unsetAlertListener();
                        discardAlarm();
                    }
                } else {
                    Log.v(Main.LOG_TAG, String.format("AppService.onSharedPreferenceChanged() backgroundPeriod=%d", backgroundPeriod));
                }
                break;

            case SHOW_PARTICIPANTS:
                updateParticipants = sharedPreferences.getBoolean(key.toString(), true);
                break;
        }
    }

    private void createAlarm() {
        discardAlarm();

        if (dataListenerContainer.isEmpty() && backgroundPeriod > 0) {
            Log.v(Main.LOG_TAG, String.format("AppService.createAlarm() %d", backgroundPeriod));
            Intent intent = new Intent(this, AppService.class);
            intent.setAction(RETRIEVE_DATA_ACTION);
            pendingIntent = PendingIntent.getService(this, 0, intent, 0);
            alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (alarmManager != null) {
                alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, 0, backgroundPeriod * 1000, pendingIntent);
            } else {
                Log.e(Main.LOG_TAG, "AppService.createAlarm() failed");
            }
        }
    }

    private void discardAlarm() {
        if (alarmManager != null) {
            Log.v(Main.LOG_TAG, "AppService.discardAlarm()");
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();

            pendingIntent = null;
            alarmManager = null;
        }
    }

    private PackageInfo getPackageInfo() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }
}
