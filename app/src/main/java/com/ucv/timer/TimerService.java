package com.ucv.timer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.ucv.R;
import com.ucv.timer.state.PeriodState;
import com.ucv.timer.state.TimerState;

import static com.ucv.utils.Utils.getTimeString;

public class TimerService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener {

    private volatile TimerState timerState = TimerState.STOPPED;
    private Thread timerThread;
    private final Object timerThreadLock = new Object();
    int timerNotificationId = 135001;
    String timerControlsNotificationChannelId = "com.ucv.timer_controls";
    String timerFinishedNotificationChannelId = "com.ucv.timer_finished";
    private static final String startActionIntentString = "com.ucv.ACTION_TIMER_START";
    private static final String pauseActionIntentString = "com.ucv.ACTION_TIMER_PAUSE";
    private static final String stopActionIntentString = "com.ucv.ACTION_TIMER_STOP";
    private static final String skipActionIntentString = "com.ucv.ACTION_TIMER_SKIP";
    private final WorkTimerActionReceiver actionReceiver = new WorkTimerActionReceiver();
    private final IBinder iBinder = new LocalBinder();
    public static TimerService thisTimerService;
    private volatile PeriodState currentPeriod = PeriodState.WORK;
    private int consecutiveWorkPeriods = 0;
    private int periodsUntilBreak = 3;
    private volatile int timeMillisLeft = 0;
    private volatile long timeMillisStarted = 0;
    private volatile int totalMillis = 0;
    private volatile long stopTimeMillis = 0;
    private PendingIntent startActionIntent;
    private PendingIntent stopActionIntent;
    private PendingIntent pauseActionIntent;
    private PendingIntent openMainActivityIntent;
    private PendingIntent skipActionIntent;
    SharedPreferences sharedPreferences;
    private TimerServiceListener serviceListener;

    public class LocalBinder extends Binder {
        TimerService getService() {
            return TimerService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return iBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onCreate() {
        Log.d(this.getClass().getSimpleName(), "I'm in onCreate() from TimerService");
        super.onCreate();

        createIntentFilter();
        initControlIntents();

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        checkNumberOfSessionsUntilBreak(sharedPreferences);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(this.getClass().getSimpleName(), "I'm on onStartCommand() from SensorReader");
        if (intent != null && intent.getAction() != null && intent.getAction().equals("HAND_HOVERED")) {
            Log.d(this.getClass().getSimpleName(), "I'm in first branch of onStartCommand() from SensorReader");
            onStartPauseButtonClick();
        } else {
            Log.d(this.getClass().getSimpleName(), "I'm in onStartCommand() from TimerService");
            thisTimerService = this;
        }
        return START_NOT_STICKY;
    }

    private void createIntentFilter() {
        Log.d(this.getClass().getSimpleName(), "I'm in createIntentFilter() from TimerService");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addCategory(Intent.CATEGORY_DEFAULT);
        intentFilter.addAction(startActionIntentString);
        intentFilter.addAction(pauseActionIntentString);
        intentFilter.addAction(stopActionIntentString);
        intentFilter.addAction(skipActionIntentString);
        registerReceiver(actionReceiver, intentFilter);
    }

    private void initControlIntents() {
        Log.d(this.getClass().getSimpleName(), "I'm in initControlIntents() from TimerService");
        Intent intentMainActivity = new Intent(this, TimerActivity.class);
        intentMainActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        openMainActivityIntent = PendingIntent.getActivity(this, 1, intentMainActivity, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent startIntent = new Intent(startActionIntentString);
        startActionIntent = PendingIntent.getBroadcast(this, 100, startIntent, 0);

        Intent pauseIntent = new Intent(pauseActionIntentString);
        pauseActionIntent = PendingIntent.getBroadcast(this, 100, pauseIntent, 0);

        Intent stopIntent = new Intent(stopActionIntentString);
        stopActionIntent = PendingIntent.getBroadcast(this, 100, stopIntent, 0);

        Intent skipIntent = new Intent(skipActionIntentString);
        skipActionIntent = PendingIntent.getBroadcast(this, 100, skipIntent, 0);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel timerControlsNotificationChannel = new NotificationChannel(timerControlsNotificationChannelId,
                    "Timer controls", NotificationManager.IMPORTANCE_LOW);
            timerControlsNotificationChannel.setDescription("Pause, Resume os Stop timer with notification");
            notificationManager.createNotificationChannel(timerControlsNotificationChannel);

            NotificationChannel timerFinishNotificationChannel = new NotificationChannel(timerFinishedNotificationChannelId,
                    "Timer Finish Notification", NotificationManager.IMPORTANCE_HIGH);
            timerFinishNotificationChannel.setDescription("Get notified when cycle ends");
            timerFinishNotificationChannel.setLightColor(Color.RED);
            timerFinishNotificationChannel.enableVibration(true);
            notificationManager.createNotificationChannel(timerFinishNotificationChannel);
        }
    }

    @Override
    public void onDestroy() {
        Log.d(this.getClass().getSimpleName(), "I'm in onDestroy() from TimerService");
        stopTimer();
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        try {
            unregisterReceiver(actionReceiver);
        } catch (IllegalArgumentException e) {

        }
    }

    public void startTimer() {
        Log.d(this.getClass().getSimpleName(), "I'm in startTimer() from TimerService");
        thisTimerService = this;

        synchronized (timerThreadLock) {
            if (timerState == TimerState.STOPPED) {
                resetTimeCounter();

                timerState = TimerState.STARTED;

                restartThread();
            } else if (timerState == TimerState.PAUSED) {
                timerState = TimerState.STARTED;
                timerThreadLock.notifyAll();
            }
        }
    }

    private void moveToNextPeriod() {
        Log.d(this.getClass().getSimpleName(), "I'm in moveToNextPeriod() from TimerService");
        if (currentPeriod == PeriodState.WORK) {
            consecutiveWorkPeriods++;
        } else if (currentPeriod == PeriodState.BIG_BREAK) {
            consecutiveWorkPeriods = 0;
        }
        currentPeriod = getNextPeriod(consecutiveWorkPeriods);
    }

    private void checkNumberOfSessionsUntilBreak(SharedPreferences sharedPrefs) {
        Log.d(this.getClass().getSimpleName(), "I'm in checkNumberOfSessionsUntilBreak() from TimerService");
        periodsUntilBreak = sharedPrefs.getInt(getString(R.string.sessions_until_long_break_preference_key),
                Integer.valueOf(getString(R.string.sessions_until_long_break_default_value)));
        if (consecutiveWorkPeriods > periodsUntilBreak) consecutiveWorkPeriods = periodsUntilBreak;
    }

    private void resetTimeCounter() {
        Log.d(this.getClass().getSimpleName(), "I'm in resetTimeCounter() from TimerService");
        timeMillisStarted = System.currentTimeMillis();
        totalMillis = getTimeLeftMillis(currentPeriod);
        timeMillisLeft = totalMillis;
        stopTimeMillis = timeMillisStarted + totalMillis;
    }

    private void restartThread() {
        Log.d(this.getClass().getSimpleName(), "I'm in restartThread() from TimerService");
        timerThread = new Thread(new TimerRunnable());
        timerThread.start();
    }

    public void pauseTimer() {
        Log.d(this.getClass().getSimpleName(), "I'm in pauseTimer() from TimerService");
        synchronized (timerThreadLock) {
            if (timerState == TimerState.STARTED) {
                timerState = TimerState.PAUSED;
                timerThreadLock.notifyAll();
                sendTime(totalMillis, timeMillisLeft);
            }
        }
    }

    public void stopTimer() {
        Log.d(this.getClass().getSimpleName(), "I'm in stopTimer() from TimerService");
        if (timerState != TimerState.STOPPED) {
            synchronized (timerThreadLock) {
                moveToNextPeriod();
                timerState = TimerState.STOPPED;
                timerThreadLock.notifyAll();
            }
            sendTime(getTimeLeftMillis(currentPeriod), getTimeLeftMillis(currentPeriod));
        }
    }

    public void skipPeriod() {
        Log.d(this.getClass().getSimpleName(), "I'm in skipPeriod() from TimerService");
        synchronized (timerThreadLock) {
            if (timerState == TimerState.STOPPED) {
                moveToNextPeriod();
                sendTime(getTimeLeftMillis(currentPeriod), getTimeLeftMillis(currentPeriod));
            } else {
                stopTimer();
            }
        }
    }

    public void onStopButtonClick() {
        Log.d(this.getClass().getSimpleName(), "I'm in onStopButtonClick() from TimerService");
        currentPeriod = PeriodState.BREAK;
        consecutiveWorkPeriods = 0;
        stopTimer();
        stopForeground(true);
    }

    public void onStartPauseButtonClick() {
        Log.d(this.getClass().getSimpleName(), "I'm in onStartPauseButtonClick() from TimerService");
        if (timerState == TimerState.STARTED) {
            pauseTimer();
        } else {
            startTimer();
        }
    }

    public void onSkipButtonClickInActivity() {
        Log.d(this.getClass().getSimpleName(), "I'm in onSkipButtonClickInActivity() from TimerService");
        skipPeriod();
        stopForeground(true);
    }

    public PeriodState getNextPeriod(int consecutiveWorkPeriods) {
        Log.d(this.getClass().getSimpleName(), "I'm in getNextPeriod() from TimerService");
        if (currentPeriod == PeriodState.WORK) {
            if (periodsUntilBreak != 0 && consecutiveWorkPeriods >= periodsUntilBreak) {
                return PeriodState.BIG_BREAK;
            } else {
                return PeriodState.BREAK;
            }
        }
        return PeriodState.WORK;
    }

    public int getTimeLeftMillis(PeriodState period) {
        Log.d(this.getClass().getSimpleName(), "I'm in getTimeLeftMillis() from TimerService");
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        int timeLeft = 0;
        switch (period) {
            case WORK:
                timeLeft = sharedPrefs.getInt(getString(R.string.work_time_minutes_preference_key),
                        Integer.parseInt(getString(R.string.work_time_minutes_default_value)));
                break;
            case BREAK:
                timeLeft = sharedPrefs.getInt(getString(R.string.small_break_time_minutes_preference_key),
                        Integer.parseInt(getString(R.string.small_break_time_minutes_default_value)));
                break;
            case BIG_BREAK:
                timeLeft = sharedPrefs.getInt(getString(R.string.long_break_time_minutes_preference_key),
                        Integer.parseInt(getString(R.string.long_break_time_minutes_default_value)));
                break;
        }
        return timeLeft * 60000;
    }

    private void sendTime(int totalMillis, int millisLeft) {
        Log.d(this.getClass().getSimpleName(), "I'm in sendTime() from TimerService");
        if (serviceListener != null)
            serviceListener.onTimeChange(totalMillis, millisLeft, timerState);
    }

    public void registerTimerListener(TimerServiceListener listener) {
        Log.d(this.getClass().getSimpleName(), "I'm in registerTimerListener() from TimerService");
        serviceListener = listener;
    }

    public void unregisterTimerListener() {
        Log.d(this.getClass().getSimpleName(), "I'm in unregisterTimerListener() from TimerService");
        serviceListener = null;
    }

    public int getMillisLeft() {
        Log.d(this.getClass().getSimpleName(), "I'm in getMillisLeft() from TimerService");
        if (timerState == TimerState.STOPPED) {
            return getTimeLeftMillis(currentPeriod);
        } else return timeMillisLeft;
    }

    public int getMillisTotal() {
        Log.d(this.getClass().getSimpleName(), "I'm in getMillisTotal() from TimerService");
        if (timerState == TimerState.STOPPED) {
            return getTimeLeftMillis(currentPeriod);
        } else return totalMillis;
    }

    public TimerState getCurrentTimerState() {
        Log.d(this.getClass().getSimpleName(), "I'm in getCurrentTimerState() from TimerService");
        return timerState;
    }

    public short getPeriodsLeftUntilBigBreak() {
        Log.d(this.getClass().getSimpleName(), "I'm in getPeriodsLeftUntilBigBreak() from TimerService");
        if (periodsUntilBreak - consecutiveWorkPeriods <= 0) {
            return (short) periodsUntilBreak;
        }
        return (short) (periodsUntilBreak - consecutiveWorkPeriods);
    }

    private void timerRunningNotification() {
        Log.d(this.getClass().getSimpleName(), "I'm in timerRunningNotification() from TimerService");
        Notification.Builder builder =
                new Notification.Builder(this)
                        .setSmallIcon(R.mipmap.ic_notification_icon)
                        .setColor(getResources().getColor(R.color.colorPrimary))
                        .setContentTitle(getSessionName(currentPeriod))
                        .setContentText(getTimeString(timeMillisLeft) + getString(R.string.notification_text))
                        .setContentIntent(openMainActivityIntent)
                        .addAction(new Notification.Action(R.drawable.ic_pause_black_24dp,
                                getString(R.string.pause_notification_action_title), pauseActionIntent))
                        .addAction(new Notification.Action(R.drawable.ic_skip_next_black_24dp,
                                getString(R.string.skip_notification_action_title), skipActionIntent))
                        .addAction(new Notification.Action(R.drawable.ic_stop_black_24dp,
                                getString(R.string.stop_notification_action_title), stopActionIntent))
                        .setProgress(totalMillis, timeMillisLeft, false)
                        .setVisibility(Notification.VISIBILITY_PUBLIC)
                        .setPriority(Notification.PRIORITY_MAX)
                        .setCategory(Notification.CATEGORY_ALARM);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            builder.setChannelId(timerControlsNotificationChannelId);
        }

        startForeground(timerNotificationId, builder.build());
    }

    private void timerPausedNotification() {
        Log.d(this.getClass().getSimpleName(), "I'm in timerPausedNotification() from TimerService");
        Notification.Builder builder =
                new Notification.Builder(this)
                        .setSmallIcon(R.mipmap.ic_notification_icon)
                        .setColor(getResources().getColor(R.color.colorPrimary))
                        .setContentTitle(getSessionName(currentPeriod))
                        .setContentText(getTimeString(timeMillisLeft) + getString(R.string.notification_text))
                        .setContentIntent(openMainActivityIntent)
                        .addAction(new Notification.Action(R.drawable.ic_play_arrow_black_24dp,
                                getString(R.string.resume_notification_action_title), startActionIntent))
                        .addAction(new Notification.Action(R.drawable.ic_skip_next_black_24dp,
                                getString(R.string.skip_notification_action_title), skipActionIntent))
                        .addAction(new Notification.Action(R.drawable.ic_stop_black_24dp,
                                getString(R.string.stop_notification_action_title), stopActionIntent))
                        .setVisibility(Notification.VISIBILITY_PUBLIC)
                        .setPriority(Notification.PRIORITY_MAX)
                        .setCategory(Notification.CATEGORY_ALARM);
        builder.setChannelId(timerControlsNotificationChannelId);
        startForeground(timerNotificationId, builder.build());
    }

    private void timerFinishedNotification() {
        Log.d(this.getClass().getSimpleName(), "I'm in timerFinishedNotification() from TimerService");
        Notification.Builder builder =
                new Notification.Builder(this)
                        .setSmallIcon(R.mipmap.ic_notification_icon)
                        .setColor(getResources().getColor(R.color.colorPrimary))
                        .setContentTitle(getSessionName(currentPeriod) + getString(R.string.finished_notification_title))
                        .setContentText(getString(R.string.finished_notification_text)
                                + getSessionName(getNextPeriod(consecutiveWorkPeriods + 1)) + "?")
                        .setContentIntent(openMainActivityIntent)
                        .addAction(new Notification.Action(R.drawable.ic_play_arrow_black_24dp,
                                getString(R.string.start_notification_action_title), startActionIntent))
                        .addAction(new Notification.Action(R.drawable.ic_skip_next_black_24dp,
                                getString(R.string.skip_notification_action_title), skipActionIntent))
                        .addAction(new Notification.Action(R.drawable.ic_stop_black_24dp,
                                getString(R.string.stop_notification_action_title), stopActionIntent))
                        .setVisibility(Notification.VISIBILITY_PUBLIC)
                        .setDefaults(Notification.DEFAULT_ALL)
                        .setPriority(Notification.PRIORITY_MAX)
                        .setCategory(Notification.CATEGORY_ALARM);
        builder.setChannelId(timerFinishedNotificationChannelId);
        startForeground(timerNotificationId, builder.build());
    }

    private String getSessionName(PeriodState period) {
        Log.d(this.getClass().getSimpleName(), "I'm in getSessionName() from TimerService");
        switch (period) {
            case WORK:
                return getString(R.string.work_time_text);
            case BIG_BREAK:
                return getString(R.string.long_break_text);
            case BREAK:
                return getString(R.string.break_time_text);
            default:
                return "";
        }
    }

    public PeriodState getCurrentPeriod() {
        Log.d(this.getClass().getSimpleName(), "I'm in getCurrentPeriod() from TimerService");
        return currentPeriod;
    }

    public int getConsecutiveWorkPeriods() {
        Log.d(this.getClass().getSimpleName(), "I'm in getConsecutiveWorkPeriods() from TimerService");
        return consecutiveWorkPeriods;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.d(this.getClass().getSimpleName(), "I'm in onSharedPreferenceChanged() from TimerService");
        checkNumberOfSessionsUntilBreak(sharedPreferences);
    }

    private class TimerRunnable implements Runnable {
        @Override
        public void run() {
            Log.d(this.getClass().getSimpleName(), "I'm in run() from TimerRunnable that is inside TimerService");
            synchronized (timerThreadLock) {
                while (timerState == TimerState.STARTED) {

                    timeMillisLeft = (int) (stopTimeMillis - System.currentTimeMillis());

                    sendTime(totalMillis, timeMillisLeft);
                    timerRunningNotification();
                    if (timeMillisLeft <= 0) {
                        timerFinishedNotification();
                        stopTimer();
                    }

                    try {
                        timerThreadLock.wait(995);
                    } catch (InterruptedException e) {

                    }
                    while (timerState == TimerState.PAUSED) {
                        timerPausedNotification();
                        try {
                            timerThreadLock.wait();
                        } catch (InterruptedException e) {

                        }
                        stopTimeMillis = System.currentTimeMillis() + timeMillisLeft;
                    }
                }
            }
        }
    }

    public static class WorkTimerActionReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(this.getClass().getSimpleName(), "I'm in onReceive() from WorkTimerActionReceiver that is inside TimerService");
            if (thisTimerService != null) {
                String action = intent.getAction();
                if (action.equalsIgnoreCase(startActionIntentString)) {
                    thisTimerService.startTimer();
                } else if (action.equalsIgnoreCase(pauseActionIntentString)) {
                    thisTimerService.pauseTimer();
                } else if (action.equalsIgnoreCase(stopActionIntentString)) {
                    thisTimerService.onStopButtonClick();
                } else if (action.equalsIgnoreCase(skipActionIntentString)) {
                    thisTimerService.skipPeriod();
                    thisTimerService.startTimer();
                }
            }
        }

    }
}