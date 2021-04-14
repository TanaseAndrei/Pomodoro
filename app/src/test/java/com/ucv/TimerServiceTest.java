package com.ucv;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.util.concurrent.TimeoutException;

import com.ucv.timer.TimerService;

import static org.junit.Assert.assertEquals;


@RunWith(RobolectricTestRunner.class)
public class TimerServiceTest {

    private TimerService timerService;


    @Before
    public void bindService() throws TimeoutException{
        timerService = Robolectric.buildService(TimerService.class).get();
    }


    @Test
    public void startTimer_shouldChangeStateToStarted_whenCalled(){
        timerService.startTimer();
        assertEquals(TimerService.TimerState.STARTED, timerService.getCurrentTimerState());
    }


    @Test
    public void stopTimer_shouldChangeStateToStopped_whenCalled(){
        timerService.startTimer();
        timerService.stopTimer();
        assertEquals(TimerService.TimerState.STOPPED, timerService.getCurrentTimerState());
    }


    @Test
    public void pauseTimer_shouldChangeStateToPaused_whenCalled(){
        timerService.startTimer();
        timerService.pauseTimer();
        assertEquals(TimerService.TimerState.PAUSED, timerService.getCurrentTimerState());
    }


    @Test
    public void stopTimer_shouldIncrementConsecutiveWorkPeriod_whenCalled(){
        timerService.startTimer();
        timerService.stopTimer();
        assertEquals(1, timerService.getConsecutiveWorkPeriods());
    }


    @Test
    public void onStopButtonClick_shouldResetConsecutiveWorkPeriod_whenCalled(){
        timerService.onStopButtonClick();
        assertEquals(TimerService.TimerState.STOPPED, timerService.getCurrentTimerState());
        assertEquals(0, timerService.getConsecutiveWorkPeriods());
    }


    @Test
    public void onSkipButtonClick_shouldSkipToNextPeriod_whenCalled(){
        timerService.onSkipButtonClickInActivity();
        assertEquals(TimerService.PeriodState.BREAK, timerService.getCurrentPeriod());
    }

    @Test
    public void onSkipButtonClick_shouldIncrementConsecutiveWorkPeriod_whenCalled(){
        timerService.onSkipButtonClickInActivity();
        assertEquals(1, timerService.getConsecutiveWorkPeriods());
    }

}
