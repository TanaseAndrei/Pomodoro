package com.ucv.timer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.ucv.R;
import com.ucv.about.AboutActivity;
import com.ucv.sensor.SensorReader;
import com.ucv.settings.SettingsActivity;
import com.ucv.timer.state.HandHoveredState;
import com.ucv.timer.state.PeriodState;
import com.ucv.utils.ThemeManager;

import com.ucv.timer.state.TimerState;

public class TimerActivity extends AppCompatActivity implements View.OnClickListener, TimerServiceListener {

    private FloatingActionButton startButton;
    private ImageButton stopButton;
    private ImageButton skipButton;
    private TimerService timerService;
    private TimerView timerView;
    private TimerState currentTimerState;
    private PeriodState currentTimerPeriod;
    private int consecutivePeriods;
    private TextView currentPeriodTextView;
    private TextView periodsUntilBigBreakTextView;
    private SensorReader sensorReader;
    private HandHoveredState handHoveredState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(this.getClass().getSimpleName(), "I'm in onCreate() from TimerActivity");
        setupTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        handHoveredState = new HandHoveredState();
        sensorReader = new SensorReader(this, handHoveredState);
        startButton = findViewById(R.id.startPauseButton);
        stopButton = findViewById(R.id.stopButton);
        skipButton = findViewById(R.id.skipButton);
        timerView = findViewById(R.id.timerView);
        currentPeriodTextView = findViewById(R.id.currentPeriodTextView);
        periodsUntilBigBreakTextView = findViewById(R.id.periodsUntilBigBreakTextView);
        startButton.setOnClickListener(this);
        stopButton.setOnClickListener(this);
        skipButton.setOnClickListener(this);
        this.setTitle("");
    }

    @Override
    protected void onStart() {
        Log.d(this.getClass().getSimpleName(), "I'm in onStart() from TimerActivity");
        super.onStart();
        // Bind TimerService.
        Intent intent = new Intent(this, TimerService.class);
        startService(intent);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorReader.getSensorManager().registerListener(sensorReader, sensorReader.getProximitySensor(), SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorReader.getSensorManager().unregisterListener(sensorReader);
    }

    @Override
    protected void onRestart() {
        Log.d(this.getClass().getSimpleName(), "I'm in onRestart() from TimerActivity");
        // Redraw timerView in case any settings changed
        super.onRestart();
        timerView.stopAnimation();
        updateButtons(timerService.getCurrentTimerState(), timerService.getConsecutiveWorkPeriods());
        initializeTimerView();
        showCurrentStateText();
        setPeriodCounter();
    }

    @Override
    protected void onStop() {
        Log.d(this.getClass().getSimpleName(), "I'm in onStop() from TimerActivity");
        super.onStop();
        unbindService(serviceConnection);
    }

    @Override
    protected void onDestroy() {
        Log.d(this.getClass().getSimpleName(), "I'm in onDestroy() from TimerActivity");
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(this.getClass().getSimpleName(), "I'm in onCreateOptionsMenu() from TimerActivity");
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(this.getClass().getSimpleName(), "I'm in onOptionsItemSelected() from TimerActivity");
        switch (item.getItemId()) {
            case R.id.settings_menu:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.about_menu:
                startActivity(new Intent(this, AboutActivity.class));
                return true;
            default: break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {
        Log.d(this.getClass().getSimpleName(), "I'm in onClick() from TimerActivity");
        showCurrentStateText();
        if (view == startButton) {
            timerService.onStartPauseButtonClick();
        } else if (view == stopButton) {
            timerService.startTimer();
            timerService.onStopButtonClick();
        } else if (view == skipButton) {
            timerService.onSkipButtonClickInActivity();
        }
    }

    public void handHovered() {
        Log.d(this.getClass().getSimpleName(), "I'm on handHovered() from SensorReader");
        Intent intention = new Intent(this, TimerService.class);
        intention.setAction("HAND_HOVERED");
        startService(intention);
    }

    private void showCurrentStateText() {
        Log.d(this.getClass().getSimpleName(), "I'm in showCurrentStateText() from TimerActivity");
        PeriodState newPeriod = timerService.getCurrentPeriod();
        if (currentTimerPeriod != newPeriod) {
            currentTimerPeriod = newPeriod;
            String currentSession = "";
            switch (currentTimerPeriod) {
                case WORK:
                    currentSession = getString(R.string.work_time_text);
                    break;
                case BREAK:
                    currentSession = getString(R.string.break_time_text);
                    break;
                case BIG_BREAK:
                    currentSession = getString(R.string.long_break_text);
                    break;
            }
            currentPeriodTextView.setText(currentSession);
        }
    }

    private void setPeriodCounter() {
        Log.d(this.getClass().getSimpleName(), "I'm in setPeriodCounter() from TimerActivity");
        short newPeriodCount = timerService.getPeriodsLeftUntilBigBreak();
        if (newPeriodCount == 0) {
            periodsUntilBigBreakTextView.setText("");
        } else if (newPeriodCount == 1) {
            periodsUntilBigBreakTextView.setText(R.string.one_session_until_long_break_text);
        } else {
            periodsUntilBigBreakTextView.setText(newPeriodCount + getString(R.string.sessions_until_long_break_text));
        }
    }

    public void onTimeChange(final int totalMillis, final int millisLeft, final TimerState timerState) {
        Log.d(this.getClass().getSimpleName(), "I'm in onTimeChange() from TimerActivity");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setPeriodCounter();

                showCurrentStateText();

                updateButtons(timerState, timerService.getConsecutiveWorkPeriods());

                updateTimerView(timerState, totalMillis, millisLeft);
            }
        });
    }

    private void updateTimerView(TimerState currentTimerState, int millisTotal, int millisLeft) {
        Log.d(this.getClass().getSimpleName(), "I'm in updateTimerView() from TimerActivity");
        switch (currentTimerState) {
            case STARTED:
                timerView.onTimerStarted(millisTotal, millisLeft);
                break;
            case PAUSED:
                timerView.onTimerPaused(millisTotal, millisLeft);
                break;
            case STOPPED:
                timerView.onTimerStopped(millisTotal, millisLeft);
                break;
            default:
                timerView.onTimerUpdate(millisTotal, millisLeft);
        }
    }

    public void initializeTimerView() {
        Log.d(this.getClass().getSimpleName(), "I'm in initializeTimerView() from TimerActivity");
        updateTimerView(timerService.getCurrentTimerState(), timerService.getMillisTotal(), timerService.getMillisLeft());
    }

    public void updateButtons(TimerState timerState, int currentConsecutivePeriods) {
        Log.d(this.getClass().getSimpleName(), "I'm in updateButtons() from TimerActivity");
        if (currentTimerState != timerState || consecutivePeriods != currentConsecutivePeriods) {
            setButtonsState(timerState, currentConsecutivePeriods);
            currentTimerState = timerState;
            consecutivePeriods = currentConsecutivePeriods;
        }
    }

    private void setButtonsState(TimerState timerState, int consecutivePeriods) {
        Log.d(this.getClass().getSimpleName(), "I'm in setButtonsState() from TimerActivity");
        switch (timerState) {
            case STARTED:
                Log.d(this.getClass().getSimpleName(), "STARTED");
                startButton.setImageResource(R.drawable.ic_pause);
                stopButton.setVisibility(View.VISIBLE);
                break;
            case STOPPED:
                Log.d(this.getClass().getSimpleName(), "STOPPED");
                if (consecutivePeriods < 1) {
                    stopButton.setVisibility(View.INVISIBLE);
                } else {
                    stopButton.setVisibility(View.VISIBLE);
                }
                startButton.setImageResource(R.drawable.ic_play);
                break;
            case PAUSED:
                Log.d(this.getClass().getSimpleName(), "PAUSED");
                startButton.setImageResource(R.drawable.ic_play);
                break;
        }
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d(this.getClass().getSimpleName(), "I'm in onServiceConnected() from TimerActivity");
            timerService = ((TimerService.LocalBinder) iBinder).getService();
            timerService.registerTimerListener(TimerActivity.this);
            updateButtons(timerService.getCurrentTimerState(), timerService.getConsecutiveWorkPeriods());
            initializeTimerView();
            showCurrentStateText();
            setPeriodCounter();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(this.getClass().getSimpleName(), "I'm in onServiceDisconnected() from TimerActivity");
            timerService.unregisterTimerListener();
        }
    };

    private void setupTheme() {
        new ThemeManager(this)
                .setLightTheme(R.style.TimerActivityLight)
                .applyTheme();
    }
}
