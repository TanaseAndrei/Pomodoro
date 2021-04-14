package com.ucv.sensor;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.widget.Toast;

import com.ucv.timer.TimerActivity;
import com.ucv.timer.TimerService;
import com.ucv.timer.state.HandHoveredState;

public class SensorReader implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor proximitySensor;
    private final TimerActivity timerActivity;
    private final HandHoveredState handHoveredState;

    public SensorReader(TimerActivity timerActivity, HandHoveredState handHoveredState) {
        this.timerActivity = timerActivity;
        this.handHoveredState = handHoveredState;
        retrieveSensor();
    }
    
    @Override
    public void onSensorChanged(SensorEvent event) {
        Log.d(this.getClass().getSimpleName(), "I'm on onSensorChanged() from SensorReader");
        if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            float proximitySensorValue = event.values[0];
            if(isInRange(proximitySensorValue) && !handHoveredState.getHandHovered()) {
                handHoveredState.handWasHovered();
                timerActivity.handHovered();
            } else if (proximitySensorValue <= 0) {
                handHoveredState.resetHandHovered();
            }
        }
    }

    private boolean isInRange(float value) {
        return value <= proximitySensor.getMaximumRange() && value > 0;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public SensorManager getSensorManager() {
        return sensorManager;
    }

    public Sensor getProximitySensor() {
        return proximitySensor;
    }

    private void retrieveSensor() {
        sensorManager = (SensorManager) timerActivity.getSystemService(Context.SENSOR_SERVICE);
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
    }
}
