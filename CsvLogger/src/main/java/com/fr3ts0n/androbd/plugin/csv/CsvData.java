package com.fr3ts0n.androbd.plugin.csv;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implements a CSV data table that aggregates the latest data as each field is updated
 * The client is expected to call saveRow on a periodic basis to commit the data to a row
 * and then writeOutput to save the entire CsvData to a file and start a new CsvData
 *
 * This implementation is append-only, and is meant to be a segment of a queue of data.
 * Meaning this CsvData is meant to only hold a few thousand rows of data before being
 * flushed to a OutputStreamWriter and swapped out for a new CsvData
 */
public class CsvData
{
    // the column order in the output file
    private List<String> columns;
    // remember a String.hashCode to reference a single instance of that String
    private Map<String, String> columnInstances;
    // the latest values of the fields, to collect at each row flush threshold
    private Map<String, String> latestValues;
    // each of the rows that have been flushed, timestamp to a latestValues-style map
    private Map<Long, Map<String, String>> rows;

    // whether any new columns have been added in this segment, compared to the previous
    private boolean hasNewColumns;
    // whether any new data has been added since the last row was saved
    private boolean hasPendingData;

    /**
     * Creates a blank CsvData table
     */
    public CsvData()
    {
        columns = new ArrayList<>();
        columnInstances = new HashMap<>();
        latestValues = new HashMap<>();
        rows = new HashMap<>();
        hasNewColumns = false;
        hasPendingData = false;
    }

    /**
     * Creates a blank CsvData table, but with the given columns already planned
     *
     * @param columns The expected columns in order, will be the first columns to be written
     */
    public CsvData(List<String> columns)
    {
        this();
        this.columns = columns;
        for (String column: columns)
        {
            columnInstances.put(column, column);
        }
    }

    /**
     * Creates a blank CsvData starting with the same columns as this previous CsvData segment
     *
     * @param previous The CsvData to copy the columns and column order from
     */
    public CsvData(CsvData previous)
    {
        columns = new ArrayList<>(previous.columns);
        columnInstances = new HashMap<>(previous.columnInstances);
        latestValues = new HashMap<>(previous.latestValues);
        rows = new HashMap<>();
        hasNewColumns = false;
        hasPendingData = false;
    }

    /**
     * Sets the set of columns to this new list
     * Any new columns from later setData calls may add new columns to then end
     * @param columns
     */
    public void setColumns(List<String> columns)
    {
        this.columns = new ArrayList<>(columns);
        this.columnInstances = new HashMap<>(this.columns.size());
        for (String key: this.columns) {
            this.columnInstances.put(key, key);
        }
        hasNewColumns = false;
    }

    /**
     * Changes this column key to be using this value
     *
     * @param key   The name of the column
     * @param value The value to store in this column
     */
    public void setData(String key, String value)
    {
        String previousKey = columnInstances.get(key);
        if (previousKey == null)
        {
            columns.add(key);
            columnInstances.put(key, key);
            previousKey = key;
            hasNewColumns = true;
        }

        latestValues.put(previousKey, value);
    }

    /**
     * Commits the current latestValues to a new timestamped row in the CSV file
     * <p>
     * Future optimization: drop intermediate identical rows
     */
    public void saveRow()
    {
        long timestamp = System.currentTimeMillis();
        Map<String, String> row = new HashMap<>(latestValues);
        rows.put(timestamp, row);
    }

    /**
     * @return the epoch timestamp (in UTC) of the first row, or the current time if empty
     */
    public long getStartTime()
    {
        if (rows.isEmpty())
        {
            return System.currentTimeMillis();
        }

        Long[] timestamps = rows.keySet().toArray(new Long[0]);
        Arrays.sort(timestamps);
        return timestamps[0];
    }

    /**
     * @return the epoch timestamp (in UTC) of the last row, or the current time if empty
     */
    public long getEndTime()
    {
        if (rows.isEmpty())
        {
            return System.currentTimeMillis();
        }

        Long[] timestamps = rows.keySet().toArray(new Long[0]);
        Arrays.sort(timestamps);
        return timestamps[timestamps.length - 1];
    }

    /**
     * @return the number of saved rows
     */
    public int size()
    {
        return rows.size();
    }

    /**
     * @return whether any new columns have been added since the creation of this table
     */
    public boolean hasNewColumns()
    {
        return hasNewColumns;
    }

    /**
     * @return whether any new data has been set since the last saveRow call
     */
    public boolean hasPendingData()
    {
        return hasPendingData;
    }

    /**
     * Writes the data table to the given writer, optionally with headers
     * Always writes out the entire table for each call, intending to be discarded afterward
     *
     * @param writer        The output writer to save the row to
     * @param includeHeader Whether to include a first row of headers with the field names
     */
    public void writeOutput(OutputStreamWriter writer, boolean includeHeader) throws IOException
    {
        if (includeHeader)
        {
            StringBuilder row = new StringBuilder();
            row.append("timestamp");
            for (String column: columns)
            {
                row.append(',');
                row.append(quoteCell(column));
            }
            row.append("\r\n");
            writer.write(row.toString());
        }

        Long[] timestamps = rows.keySet().toArray(new Long[0]);
        Arrays.sort(timestamps);
        for (Long timestamp: timestamps)
        {
            Map<String, String> rowData = rows.get(timestamp);
            StringBuilder row = new StringBuilder();
            row.append(timestamp);
            for (String column: columns)
            {
                row.append(',');
                String cellData = rowData.get(column);
                if (cellData == null)
                {
                    cellData = "";
                }
                row.append(quoteCell(cellData));
            }
            row.append("\r\n");
            writer.write(row.toString());
        }
    }

    private String quoteCell(String cell)
    {
        if (cell.contains("\"") || cell.contains(",") || cell.contains("\n"))
        {
            return "\"" + cell.replaceAll("\"", "\"\"") + "\n";
        }
        // no quoting needed
        return cell;
    }
}
