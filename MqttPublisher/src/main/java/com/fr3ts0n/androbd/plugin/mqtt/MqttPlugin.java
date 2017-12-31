package com.fr3ts0n.androbd.plugin.mqtt;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
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

    /**
     * The data collection
     */
    HashMap<String, String> valueMap = new HashMap<>();

    SharedPreferences prefs;
    String mqtt_prefix = "";

    /** MQTT communication parameter */
    String brokerHostName;
    int brokerPortNumber;
    String mUsername;
    String mPassword;
    String mClientId = null;
    boolean mRetain = true;

    int update_period = 30;

    Timer timer;

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
     * The task to publish current data
     */
    TimerTask publishTask = new TimerTask()
    {
        @Override
        public void run()
        {
            performAction();
        }
    };

    /**
     * Schedule cyclic MQTT updates
     *
     * @param updatePeriod Time in seconds between cyclic MQTT publish updates
     */
    void scheduleMqttUpdates(long updatePeriod)
    {
        // cancel old timer
        if(timer != null)
            timer.cancel();

        // set new timer
        timer = new Timer();
        // schedule publishing task on timer
        timer.schedule(publishTask, updatePeriod * 1000, updatePeriod * 1000);
    }

    @Override
    public void onCreate()
    {
        super.onCreate();

        // get preferences
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        // get all shred preference values
        onSharedPreferenceChanged(prefs, null);

        // set a proper client id if we have none
        if (mClientId == null || mClientId.trim().equals(""))
        {
            mClientId = MqttClient.generateClientId();
        }
    }

    @Override
    public void onDestroy()
    {
        if(timer != null)
            timer.cancel();

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
        {
            int newPeriod = getPrefsInt(sharedPreferences, UPDATE_PERIOD, 30);
            if (key == null || update_period != newPeriod)
            {
                update_period = newPeriod;
                scheduleMqttUpdates(update_period);
            }
        }

        if(key == null || MQTT_HOSTNAME.equals(key))
            brokerHostName = sharedPreferences.getString(MQTT_HOSTNAME, "localhost");

        if(key == null || MQTT_PORT.equals(key))
            brokerPortNumber = getPrefsInt(sharedPreferences, MQTT_PORT, 1883);

        if(key == null || MQTT_USERNAME.equals(key))
            mUsername = sharedPreferences.getString(MQTT_USERNAME, "guest");

        if(key == null || MQTT_PASSWORD.equals(key))
            mPassword = sharedPreferences.getString(MQTT_PASSWORD, "guest");

        if(key == null || MQTT_RETAIN.equals(key))
            mRetain   = sharedPreferences.getBoolean(MQTT_RETAIN, true);
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
                    // get data items
                    String topic = mqtt_prefix + entry.getKey();
                    String value = entry.getValue();

                    // set up MQTT topic / message
                    final MqttTopic messageTopic = client.getTopic(topic);
                    final MqttMessage message = new MqttMessage(value.getBytes());
                    message.setRetained(mRetain);
                    // publish topic
                    messageTopic.publish(message);
                    // Log message
                    Log.d(toString(), "Published data. Topic: " + messageTopic.getName() + " Retain Flag: " + message.isRetained() + "  Message: " + message + " QoS:" + message.getQos());
                }
            }

            // disconnect client
            client.disconnect();
        }
        catch (MqttException e)
        {
            Log.e(toString(), "Publish", e);
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
