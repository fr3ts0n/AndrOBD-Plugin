package com.fr3ts0n.androbd.plugin.sensorprovider;


import android.Manifest;
import android.content.IntentFilter;
import android.os.Bundle;
import android.preference.MultiSelectListPreference;
import android.preference.PreferenceActivity;
import android.util.Log;

import com.fr3ts0n.androbd.plugin.Plugin;

import java.security.Permission;
import java.util.Arrays;
import java.util.Comparator;

public class SettingsActivity extends PreferenceActivity
{
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
    }
}