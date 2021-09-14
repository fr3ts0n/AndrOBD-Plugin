package com.fr3ts0n.androbd.plugin.csv;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

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
    static final int MAX_SEGMENT_TIMEOUT = 10000;    // maximum time before flushing a segment
    static final int AUTOMATIC_PAUSE = 60000;       // stop recording after this length of time since the last data point

    private static final int NOTIFICATION_ID = 3433;    // Notification ID to update and clear it
    public static final String HOST_PACKAGE = "com.fr3ts0n.ecu.gui.androbd";

    static final PluginInfo myInfo = new PluginInfo( "CsvDataLogger",
                                                     CsvDataLogger.class,
                                                     "Log all measurements to CSV file",
                                                     "Copyright (C) 2021 by hufman",
                                                     "GPLV3+",
                                                     "https://github.com/fr3ts0n/AndrOBD"
                                                   );

    private Handler handler;
    private CsvWriterThread writer;
    private CsvData segment;
    private long lastSaved;
    private long lastWritten;

    private final Runnable automaticTimer = new Runnable()
    {
        @Override
        public void run()
        {
            // check if we need to flush the segment after a timeout
            if (lastWritten + MAX_SEGMENT_TIMEOUT < System.currentTimeMillis())
            {
                writeSegment();
            }

            showNotification();

            if (lastWritten + AUTOMATIC_PAUSE < System.currentTimeMillis())
            {
                stopRecording();
            }

            if (writer != null)
            {
                handler.postDelayed(automaticTimer, 1000);
            }
        }
    };

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
        handler = new Handler();
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
        if (writer == null)
        {
            startRecording();
        } else
        {
            stopRecording();
        }
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

    private void startRecording()
    {
        Log.i(TAG, "Starting recording");
        writer = new CsvWriterThread(getApplicationContext().getExternalFilesDir(null));
        writer.start();
        lastSaved = System.currentTimeMillis();
        lastWritten = System.currentTimeMillis();

        showNotification();

        ViewModel.isRecording = true;
        ViewModel.countDataPoints = 0;
        ViewModel.countDataRows = 0;
        handler.postDelayed(automaticTimer, 1000);

        Toast.makeText(getApplicationContext(), R.string.btn_start_recording, Toast.LENGTH_SHORT).show();
    }

    private void showNotification()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannelManager.createNotificationChannel(getApplicationContext());
        }

        if (!ViewModel.isRecording)
        {
            return;
        }

        Notification.Builder notificationBuilder = new Notification.Builder(this)
                .setOngoing(true)
                .setContentTitle(getText(R.string.lbl_notification_title))
                .setContentText(getString(R.string.lbl_notification_text, ViewModel.countDataPoints, ViewModel.countDataRows))
                .setSmallIcon(R.drawable.ic_notify)
                .setPriority(Notification.PRIORITY_DEFAULT);

        Intent pendingIntent = new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setPackage(HOST_PACKAGE)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        List<ResolveInfo> results = getPackageManager().queryIntentActivities(pendingIntent, 0);
        if (results.size() > 0)
        {
            ResolveInfo result = results.get(0);
            PendingIntent pendingClickIntent = PendingIntent.getActivity(getApplicationContext(),
                    15,
                    pendingIntent.setComponent(new ComponentName(result.activityInfo.packageName, result.activityInfo.name)),
                    PendingIntent.FLAG_UPDATE_CURRENT);
            notificationBuilder.setContentIntent(pendingClickIntent);
        }

        PendingIntent pauseIntent = PendingIntent.getService(getApplicationContext(),
                20,
                new Intent(ACTION)
                        .setComponent(new ComponentName(getApplicationContext(), CsvDataLogger.class))
                        .putExtra("CLASS", this.getClass().getCanonicalName()),
                PendingIntent.FLAG_UPDATE_CURRENT);
        notificationBuilder.addAction(R.drawable.ic_notify,
                getText(R.string.btn_stop_recording),
                pauseIntent);

        Notification notification = notificationBuilder.build();
        ((NotificationManager)getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID, notification);
    }

    private void showFinishedNotification(String name)
    {
        Notification.Builder notificationBuilder = new Notification.Builder(this)
                .setContentTitle(getText(R.string.lbl_notification_title))
                .setContentText(getString(R.string.lbl_notification_finished))
                .setSmallIcon(R.drawable.ic_coin)
                .setPriority(Notification.PRIORITY_DEFAULT);

        PendingIntent pendingClickIntent = PendingIntent.getActivity(getApplicationContext(),
                15,
                new Intent(Intent.ACTION_MAIN)
                        .setComponent(new ComponentName(getApplicationContext(), SettingsActivity.class))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT);
        notificationBuilder.setContentIntent(pendingClickIntent);

        Uri uri = Uri.parse("content://" + getPackageName() + ".provider/" + name);
        Intent shareIntent = new Intent(Intent.ACTION_SEND)
            .putExtra(Intent.EXTRA_STREAM, uri)
            .setType("text/plain")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        PendingIntent shareActionIntent = PendingIntent.getActivity(getApplicationContext(),
                20,
                Intent.createChooser(shareIntent, getString(R.string.lbl_share)),
                PendingIntent.FLAG_UPDATE_CURRENT);
        notificationBuilder.addAction(R.drawable.ic_notify,
                getText(R.string.btn_share_recording),
                shareActionIntent);

        Notification notification = notificationBuilder.build();
        ((NotificationManager)getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID, notification);
    }

    private void hideNotification()
    {
        ((NotificationManager)getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE)).cancel(NOTIFICATION_ID);
    }

    private void stopRecording()
    {
        Log.i(TAG, "Stopping recording");
        hideNotification();

        if (writer != null)
        {
            String name = writer.getFilename();
            writer.close();
            writer.quitSafely();
            writer = null;

            if (name != null)
            {
                showFinishedNotification(name);
            }
        }

        ViewModel.isRecording = false;

        Toast.makeText(getApplicationContext(), R.string.btn_stop_recording, Toast.LENGTH_SHORT).show();
    }

    /**
     * Handle data list update.
     * If this happens while paused, changes the CsvData's columns to use the new layout
     *
     * @param csvString
     * CSV data string in format key;description;min;max;units.
     * One line per data item
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
        ViewModel.countDataPoints += 1;

        if (writer != null)
        {
            if (lastSaved + MIN_ROW_TIMEOUT < System.currentTimeMillis())
            {
                segment.saveRow();
                lastSaved = System.currentTimeMillis();
                ViewModel.countDataRows += 1;
            }

            if (segment.size() > 1000)
            {
                writeSegment();
            }
        }
    }

    private void writeSegment()
    {
        if (writer != null && segment.size() > 0)
        {
            CsvData previous = segment;
            segment = new CsvData(previous);
            writer.write(previous);
            lastWritten = System.currentTimeMillis();
        }
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        if (writer != null && segment.size() > 0)
        {
            CsvData previous = segment;
            segment = new CsvData();        // forget the previous columns, we are shutting down
            writer.write(previous);
        }

        stopRecording();
    }
}
