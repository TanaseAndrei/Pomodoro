package com.ucv.utils;


import android.content.Context;

public class ThemeManager{

    private final Context context;
    private int lightTheme;

    public ThemeManager(Context context){
        this.context = context;
    }

    public ThemeManager setLightTheme(int id){
        lightTheme = id;
        return this;
    }

    public void applyTheme(){
        context.setTheme(lightTheme);
    }
}
