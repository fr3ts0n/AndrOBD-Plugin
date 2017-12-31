package com.fr3ts0n.androbd.plugin.csv;

import com.fr3ts0n.androbd.plugin.Plugin;
import com.fr3ts0n.androbd.plugin.PluginInfo;

/**
 * AndrOBD CSV data logger
 *
 * A plugin to log all measured OBD data into CSV file
 */
public class CsvDataLogger
    extends Plugin
    implements Plugin.ConfigurationHandler,
               Plugin.ActionHandler,
               Plugin.DataReceiver
{

    static final PluginInfo myInfo = new PluginInfo( "CsvDataLogger",
                                                     CsvDataLogger.class,
                                                     "Log all measurements to CSV file",
                                                     "Copyright (C) 2017 by fr3ts0n",
                                                     "GPLV3+",
                                                     "https://github.com/fr3ts0n/AndrOBD"
                                                   );

    /**
     * Default constructor
     */
    public CsvDataLogger()
    {
        super();
    }

    /**
     * get own plugin info
     */
    @Override
    public final PluginInfo getPluginInfo()
    {
        return myInfo;
    }

    /**
     * Perform intended action of the plugin
     */
    @Override
    public void performAction()
    {

    }

    /**
     * Handle configuration request.
     * Perform plugin configuration
     */
    @Override
    public void performConfigure()
    {

    }

    /**
     * Handle data list update.
     *
     * @param csvString CSV data string in format key;value
     */
    @Override
    public void onDataListUpdate(String csvString)
    {

    }

    /**
     * Handle data update.
     * @param key Key of data change
     * @param value New value of data change
     */
    @Override
    public void onDataUpdate(String key, String value)
    {

    }
}
