package com.ucv.settings;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.preference.DialogPreference;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.NumberPicker;

import com.ucv.R;

public class NumberPickerDialogPreference extends DialogPreference {

    public static String attributeNamespace = "com.ucv.pomodoro";

    // allowed range
    private int maxValue;
    private  int minValue;

    private NumberPicker picker;
    private int value;

    public NumberPickerDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public NumberPickerDialogPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(AttributeSet attrs){
        minValue = attrs.getAttributeIntValue(attributeNamespace, "min_value", 0);
        maxValue = attrs.getAttributeIntValue(attributeNamespace, "max_value", 100);
        setPositiveButtonText(R.string.number_picker_dialog_positive_button_text);
        setNegativeButtonText(R.string.number_picker_dialog_negative_button_text);
    }

    @Override
    protected View onCreateDialogView() {
        Log.d(this.getClass().getSimpleName(), "I'm in onCreateDialogView() in NumberPickerDialogPrefernce");
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = Gravity.CENTER;

        picker = new NumberPicker(getContext());
        picker.setLayoutParams(layoutParams);

        FrameLayout dialogView = new FrameLayout(getContext());
        dialogView.addView(picker);

        return dialogView;
    }

    @Override
    protected void onBindDialogView(View view) {
        Log.d(this.getClass().getSimpleName(), "I'm in onBindDialogView() in NumberPickerDialogPrefernce");
        super.onBindDialogView(view);
        picker.setMinValue(minValue);
        picker.setMaxValue(maxValue);
        picker.setValue(getValue());
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        Log.d(this.getClass().getSimpleName(), "I'm in onDialogClosed() in NumberPickerDialogPrefernce");
        if (positiveResult) {
            picker.clearFocus();
            int newValue = picker.getValue();
            if (callChangeListener(newValue)) {
                setValue(newValue);
            }
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        Log.d(this.getClass().getSimpleName(), "I'm in onGetDefaultValue() in NumberPickerDialogPrefernce");
        return a.getInt(index, minValue);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        Log.d(this.getClass().getSimpleName(), "I'm in onSetInitialValue() in NumberPickerDialogPrefernce");
        setValue(restorePersistedValue ? getPersistedInt(minValue) : (Integer) defaultValue);
    }

    public void setValue(int value) {
        Log.d(this.getClass().getSimpleName(), "I'm in setValue() in NumberPickerDialogPrefernce");
        this.value = value;
        persistInt(this.value);
    }

    public int getValue() {
        return this.value;
    }
}