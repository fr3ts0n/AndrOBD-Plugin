package com.fr3ts0n.androbd.plugin.mqtt;


import android.os.Bundle;
import android.preference.MultiSelectListPreference;
import android.preference.PreferenceActivity;

import java.util.Arrays;
import java.util.Comparator;

public class SettingsActivity extends PreferenceActivity
{
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
        // initialize data item selection
        initItemSelection();
    }
    
    /**
     * Initialize data item selection from currently collected data items
     */
    void initItemSelection()
    {
        // get collected data items from valueMap
        CharSequence[] items = new CharSequence[MqttPlugin.valueMap.size()];
        items = MqttPlugin.valueMap.keySet().toArray(items);
        // sort array to make it readable
        Arrays.sort(items, new Comparator<CharSequence>()
        {
            @Override
            public int compare(CharSequence o1, CharSequence o2)
            {
                return o1.toString().compareTo(o2.toString());
            }
        });

        // assign list to preference selection
        MultiSelectListPreference pref = (MultiSelectListPreference)findPreference("data_items");
        pref.setEntries(items);
        pref.setEntryValues(items);
    }
}