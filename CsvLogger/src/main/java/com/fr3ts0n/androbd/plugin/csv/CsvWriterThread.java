package com.fr3ts0n.androbd.plugin.csv;

import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.fr3ts0n.androbd.plugin.csv.CsvDataLogger.TAG;

/**
 * A HandlerThread to asynchronously write out CsvData objects to the given directory
 * It will open new files automatically, named by the start timestamp of the first CsvData
 * Successive CsvData objects will be appended to the current file
 * Writing a CsvData with different columns will split to a new file
 */
public class CsvWriterThread extends HandlerThread
{
    private List<CsvData> queue;
    private Handler handler;

    private File path;
    private File outputFile;
    private SimpleDateFormat timestampFormatter;
    private OutputStreamWriter writer;
    private boolean alreadyLoggedError;

    private Runnable consumer = new Runnable()
    {
        @Override
        public void run()
        {
            writeOut();
        }
    };

    private Runnable closer = new Runnable()
    {
        @Override
        public void run()
        {
            boolean isEmpty;
            synchronized (this)
            {
                isEmpty = queue.size() == 0;
            }

            if (isEmpty)
            {
                closeOut();     // close out the writer
            } else
            {
                close();        // try again
            }
        }
    };

    public CsvWriterThread(File path)
    {
        super("CsvWriterThread");

        queue = new ArrayList<>();
        handler = null;
        this.path = path;
        timestampFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HHmmss'Z'");
        writer = null;
        alreadyLoggedError = false;

        testWrite();
    }

    private void testWrite()
    {
        try
        {
            File destination = new File(path, "test.txt");
            OutputStreamWriter test = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(destination)));
            test.write("Test write successful\r\n");
            test.close();
        } catch (IOException e)
        {
            Log.e(TAG, "Error confirming write permission", e);
        }
    }

    @Override
    protected void onLooperPrepared()
    {
        super.onLooperPrepared();
        handler = new Handler(this.getLooper());
    }

    /**
     * Add this CsvData object to be written out
     * Actually adds it to a queue to be written out asynchronously
     *
     * @param data The segment to write to storage
     */
    public void write(CsvData data)
    {
        synchronized (this)
        {
            queue.add(data);
        }
        if (handler != null)
        {
            handler.post(consumer);
        }
    }

    private OutputStreamWriter openWriter(CsvData segment) throws IOException
    {
        Date timestamp = new Date(segment.getStartTime());
        String rfcTimestamp = timestampFormatter.format(timestamp);
        String filename = "androbd_" + rfcTimestamp + ".csv";
        File destination = new File(path, filename);
        this.outputFile = destination;
        return new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(destination)));
    }

    public String getFilename()
    {
        if (outputFile != null)
        {
            return outputFile.getName();
        }
        return null;
    }

    public boolean isOpen() {
        return writer != null;
    }

    private void writeOut()
    {
        CsvData segment = null;
        synchronized (this)
        {
            if (queue.size() > 0)
            {
                segment = queue.remove(0);
            }
        }

        if (segment != null && segment.size() > 0)
        {
            writeOut(segment);

            // check one more time for any new segments to flush
            handler.removeCallbacks(consumer);
            handler.post(consumer);
        }
    }

    private void writeOut(CsvData segment)
    {
        try
        {
            // if new columns have been added, start a new file
            if (writer != null && segment.hasNewColumns())
            {
                writer.close();
                writer = null;
            }

            // if we don't have a writer, open one
            if (writer == null)
            {
                writer = openWriter(segment);

                segment.writeOutput(writer, true);
            } else
            {
                segment.writeOutput(writer, false);
            }
            writer.flush();
        } catch (IOException e)
        {
            if (!alreadyLoggedError)
            {
                Log.w(TAG, "Error while outputting csv data", e);
            } else
            {
                Log.w(TAG, "Error while outputting csv data: " + e.getMessage());
            }
            alreadyLoggedError = true;
        }
    }

    public void close()
    {
        handler.post(closer);
    }

    private void closeOut()
    {
        if (writer != null)
        {
            try
            {
                writer.close();
            } catch (IOException e)
            {
                Log.w(TAG, "Error while finishing writing csv data", e);
            }
            writer = null;
            outputFile = null;
        }
    }

    @Override
    public boolean quitSafely()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
        {
            super.quitSafely();
        } else
        {
            // technically will keep the Handler alive slightly longer than the parent code,
            // but will let the data finish writing
            handler.post(new Runnable()
            {
                @Override
                public void run()
                {
                    CsvWriterThread.this.quit();
                }
            });
        }
        return true;
    }
}
