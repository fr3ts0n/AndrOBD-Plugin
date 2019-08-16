package com.example.pluginhost;

import com.fr3ts0n.androbd.plugin.Plugin;
import com.fr3ts0n.androbd.plugin.mgr.PluginManager;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by erwin on 26.12.17.
 */

public class MainActivity
        extends PluginManager
        implements Plugin.DataReceiver
{
    Timer timer;

    @Override
    protected void onStart()
    {
        super.onStart();
        setManagerView();

        // perform cyclic updates of several values for testing
        TimerTask task = new TimerTask()
        {
            String topics[] = { "topic1", "test", "testTopic", "idontknow" };
            int value = 0;

            @Override
            public void run()
            {
                for (String topic : topics)
                {
                    pluginHandler.sendDataUpdate(topic, String.valueOf(value++));
                }
            }
        };
        timer = new Timer();
        timer.schedule(task, 0, 500);
    }

    @Override
    protected void onStop()
    {
        timer.cancel();
        super.onStop();
    }

    @Override
    public void onDataListUpdate(String csvString)
    {
        // ToDo: Handle data list reception
    }

    @Override
    public void onDataUpdate(String key, String value)
    {
        // ToDo: Handle data updates
    }
}
