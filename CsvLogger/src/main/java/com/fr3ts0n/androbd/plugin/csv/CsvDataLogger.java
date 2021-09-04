package com.fr3ts0n.androbd.plugin.csv;

import android.content.Intent;

import com.fr3ts0n.androbd.plugin.Plugin;
import com.fr3ts0n.androbd.plugin.PluginInfo;

import java.util.ArrayList;
import java.util.List;

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

    static final String TAG = "CsvDataLogger";
    static final int MIN_ROW_TIMEOUT = 100;      // minimum time between rows of data
    static final int MAX_SEGMENT_TIMEOUT = 3000;    // maximum time before flushing a segment

    static final PluginInfo myInfo = new PluginInfo( "CsvDataLogger",
                                                     CsvDataLogger.class,
                                                     "Log all measurements to CSV file",
                                                     "Copyright (C) 2017 by fr3ts0n",
                                                     "GPLV3+",
                                                     "https://github.com/fr3ts0n/AndrOBD"
                                                   );

    private CsvWriterThread writer;
    private CsvData segment;
    private long lastSaved;
    private long lastWritten;

    /**
     * Default constructor
     */
    public CsvDataLogger()
    {
        super();
        segment = new CsvData();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        writer = new CsvWriterThread(getApplicationContext().getExternalFilesDir(null));
        writer.start();
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
        Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    /**
     * Handle data list update.
     *
     * @param csvString CSV data string in format key;value
     */
    @Override
    public void onDataListUpdate(String csvString)
    {
        // replace the column order if we haven't yet written the first segment
        if (writer == null || !writer.isOpen())
        {
            String[] dataListRows = csvString.split("\n");
            List<String> columns = new ArrayList<>(dataListRows.length);
            for (String dataListRow: dataListRows)
            {
                String[] fields = dataListRow.split(";");
                if (fields.length > 3) {
                    columns.add(fields[0]);
                }
            }
            segment.setColumns(columns);
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
        segment.setData(key, value);

        if (lastSaved + MIN_ROW_TIMEOUT < System.currentTimeMillis()) {
            segment.saveRow();
            lastSaved = System.currentTimeMillis();
        }

        if (segment.size() > 1000) {
            writeSegment();
        }

        if (lastWritten == 0) {
            lastWritten = System.currentTimeMillis();
        }
        if (lastWritten + MAX_SEGMENT_TIMEOUT < System.currentTimeMillis()) {
            writeSegment();
        }
    }

    private void writeSegment() {
        CsvData previous = segment;
        segment = new CsvData(previous);
        writer.write(previous);
        lastWritten = System.currentTimeMillis();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        if (segment.size() > 0) {
            CsvData previous = segment;
            segment = new CsvData();        // forget the previous columns, we are shutting down
            writer.write(previous);
            writer.close();
            writer.quitSafely();
        }
    }
}
