package com.fr3ts0n.androbd.plugin.csv;

import com.fr3ts0n.androbd.plugin.PluginReceiver;
import com.fr3ts0n.androbd.plugin.csv.CsvDataLogger;

/**
 * Created by erwin on 24.12.17.
 */

public class CsvLoggerReceiver extends PluginReceiver
{
    public CsvLoggerReceiver()
    {
        super();
    }

    /**
     * Get class of plugin implementation
     *
     * @return Plugin implementation class
     */
    @Override
    public Class getPluginClass()
    {
        return CsvDataLogger.class;
    }
}
