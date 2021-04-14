package com.ucv.timer;


import com.ucv.timer.state.TimerState;

public interface TimerServiceListener {
    void onTimeChange(int totalMillis, int millisLeft, TimerState timerState);
}
