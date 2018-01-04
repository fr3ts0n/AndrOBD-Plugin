package com.fr3ts0n.androbd.plugin.mqtt;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;

import com.fr3ts0n.androbd.plugin.Plugin;
import com.fr3ts0n.androbd.plugin.PluginInfo;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * AndrOBD MQTT publishing plugin
 * <p>
 * Publish AndrOBD measurements to MQTT broker
 */

public class MqttPlugin
    extends     Plugin
    implements  Plugin.ConfigurationHandler,
                Plugin.ActionHandler,
                Plugin.DataReceiver,
                SharedPreferences.OnSharedPreferenceChangeListener
{
    static final PluginInfo myInfo = new PluginInfo( "MqttPublisher",
                                                     MqttPlugin.class,
                                                     "MQTT publish AndrOBD measurements",
                                                     "Copyright (C) 2017 by fr3ts0n",
                                                     "GPLV3+",
                                                     "https://github.com/fr3ts0n/AndrOBD"
                                                   );

    /** Preference keys */
    static final String MQTT_PREFIX = "mqtt_prefix";
    static final String UPDATE_PERIOD = "update_period";
    static final String MQTT_HOSTNAME = "mqtt_hostname";
    static final String MQTT_PORT = "mqtt_port";
    static final String MQTT_USERNAME = "mqtt_username";
    static final String MQTT_PASSWORD = "mqtt_password";
    static final String MQTT_RETAIN = "mqtt_retain";
    static final String MQTT_QOS = "mqtt_qos";
    static final String DATA_ITEMS = "data_items";

    /**
     * The data collection
     */
    static HashMap<String, String> valueMap = new HashMap<>();

    SharedPreferences prefs;
    String mqtt_prefix = "";

    /**
     * MQTT communication parameter
     */
    
    /** MQTT host name / IP address */
    String brokerHostName;
    /** MQTT broker port number */
    int brokerPortNumber;
    /** MQTT login name */
    String mUsername;
    /** MQTT login password */
    String mPassword;
    /** Retain preselection value */
    boolean mRetain = true;
    /** QOS preselection value */
    int mQos = 0;
    /** set of items to be published */
    HashSet<String> mSelectedItems = new HashSet<>();
    /** MQTT client id */
    String mClientId = null;
    /** Period between publishing updates */
    int update_period = 30;
    
    /**
     * Get preference int value
     *
     * @param key preference key name
     * @param defaultValue numeric default value
     *
     * @return preference int value
     */
    public static int getPrefsInt(SharedPreferences prefs, String key, int defaultValue)
    {
        int result = defaultValue;

        try
        {
            result = Integer.valueOf(prefs.getString(key, String.valueOf(defaultValue)));
        }
        catch( Exception ex)
        {
            // log error message
            Log.e("Prefs", String.format("Preference '%s'(%d): %s", key, result, ex.toString()));
        }
        return result;
    }
    
    /**
     * Working thread for cyclic publishing updates
     */
    Thread updateThread = new Thread()
    {
        public void run()
        {
            Log.i("Thread", "started");
            while (!interrupted())
            {
                try
                {
                    sleep(update_period * 1000);
                    Log.i("Thread", "Publish data");
                    performAction();
                }
                catch (InterruptedException e)
                {
                }
            }
            Log.i("Thread", "finished");
        }
    };

    @Override
    public void onCreate()
    {
        super.onCreate();

        // get preferences
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        prefs.registerOnSharedPreferenceChangeListener(this);
        // get all shared preference values
        onSharedPreferenceChanged(prefs, null);

        // set a proper client id if we have none
        if (mClientId == null || mClientId.trim().equals(""))
        {
            mClientId = MqttClient.generateClientId();
        }
        updateThread.start();
    }

    @Override
    public void onDestroy()
    {
        // interrupt cyclic thread
        updateThread.interrupt();

        // forget about settings changes
        prefs.unregisterOnSharedPreferenceChangeListener(this);

        super.onDestroy();
    }

    /**
     * Called when a shared preference is changed, added, or removed. This
     * may be called even if a preference is set to its existing value.
     * <p>
     * <p>This callback will be run on your main thread.
     *
     * @param sharedPreferences The {@link SharedPreferences} that received
     *                          the change.
     * @param key               The key of the preference that was changed, added, or
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
    {
        if(key == null || MQTT_PREFIX.equals(key))
            mqtt_prefix = sharedPreferences.getString(MQTT_PREFIX, "AndrOBD/");

        if(key == null || UPDATE_PERIOD.equals(key))
            update_period = getPrefsInt(sharedPreferences, UPDATE_PERIOD, 30);

        if(key == null || MQTT_HOSTNAME.equals(key))
            brokerHostName = sharedPreferences.getString(MQTT_HOSTNAME, "localhost");

        if(key == null || MQTT_PORT.equals(key))
            brokerPortNumber = getPrefsInt(sharedPreferences, MQTT_PORT, 1883);

        if(key == null || MQTT_USERNAME.equals(key))
            mUsername = sharedPreferences.getString(MQTT_USERNAME, "guest");

        if(key == null || MQTT_PASSWORD.equals(key))
            mPassword = sharedPreferences.getString(MQTT_PASSWORD, "guest");
    
        if(key == null || MQTT_QOS.equals(key))
            mQos   = getPrefsInt(sharedPreferences, MQTT_QOS, 0);

        if(key == null || MQTT_RETAIN.equals(key))
            mRetain   = sharedPreferences.getBoolean(MQTT_RETAIN, true);
    
        if(key == null || DATA_ITEMS.equals(key))
            mSelectedItems = (HashSet<String>)sharedPreferences.getStringSet(DATA_ITEMS, mSelectedItems);
    }

    /**
     * get own plugin info
     */
    @Override
    public PluginInfo getPluginInfo()
    {
        return myInfo;
    }

    /**
     * Perform intended action of the plugin
     */
    @Override
    public void performAction()
    {
        MqttClient client;

        // Nothing to be sent - finished!
        if(valueMap.isEmpty()) return;

        // set URL
        final String BROKER_URL = "tcp://" + brokerHostName + ":" + brokerPortNumber;

        try {
            client = new MqttClient(BROKER_URL, mClientId, new MemoryPersistence());
            final MqttConnectOptions options = new MqttConnectOptions();
            if(mUsername != null && !mUsername.trim().equals("")) {
                options.setUserName(mUsername);
                options.setPassword(mPassword.toCharArray());
            }
            client.connect(options);


            synchronized (valueMap)
            {
                // loop through data items
                for (Map.Entry<String, String> entry : valueMap.entrySet())
                {
                    // if item is selected to be published ...
                    if(mSelectedItems.contains(entry.getKey()))
                    {
                        // get topic and value
                        String topic = mqtt_prefix + entry.getKey();
                        String value = entry.getValue();
                        // publish topic
                        client.publish(topic, value.getBytes(), mQos, mRetain);
                        // Log message
                        Log.d("MQTT", "Published data. Topic: " + topic + " Message: " + value);
                    }
                }
            }

            // disconnect client
            client.disconnect();
        }
        catch (MqttException e)
        {
            Log.e("MQTT", "Publish", e);
        }

    }

    /**
     * Handle configuration request.
     * Perform plugin configuration
     */
    @Override
    public void performConfigure()
    {
        Intent cfgIntent = new Intent(getApplicationContext(), SettingsActivity.class);
        cfgIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(cfgIntent);
    }

    /**
     * Handle data list update.
     *
     * @param csvString CSV data string in format key;value
     */
    @Override
    public void onDataListUpdate(String csvString)
    {
        synchronized (valueMap)
        {
            valueMap.clear();
            // CSV data is ignored, since we are interested in key/value only
        }
    }

    /**
     * Handle data update.
     * @param key Key of data change
     * @param value New value of data change
     */
    @Override
    public void onDataUpdate(String key, String value)
    {
        synchronized (valueMap)
        {
            valueMap.put(key, value);
        }
    }
}
