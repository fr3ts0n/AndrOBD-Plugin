package com.fr3ts0n.androbd.plugin.sensorprovider;


import android.os.Bundle;
import android.preference.PreferenceActivity;

public class SettingsActivity extends PreferenceActivity
{
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(com.fr3ts0n.androbd.plugin.R.xml.settings);
    }
}