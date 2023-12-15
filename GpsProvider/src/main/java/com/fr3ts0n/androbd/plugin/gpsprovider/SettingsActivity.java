package com.fr3ts0n.androbd.plugin.gpsprovider;


import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;

public class SettingsActivity extends PreferenceActivity
{
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(com.fr3ts0n.androbd.plugin.R.xml.settings);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            requestPermissions(new String[]{ Manifest.permission.ACCESS_COARSE_LOCATION,
                                             Manifest.permission.ACCESS_COARSE_LOCATION },
                               0);
        }
    }
}