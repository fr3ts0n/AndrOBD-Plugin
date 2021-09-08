package com.fr3ts0n.androbd.plugin.csv;

import android.app.ListActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SettingsActivity extends ListActivity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        ListView view = ((ListView)findViewById(android.R.id.list));
        view.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener()
        {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id)
            {
                Map<String, String> item = (Map<String, String>) parent.getItemAtPosition(position);
                Toast.makeText(SettingsActivity.this, item.get("name"), Toast.LENGTH_SHORT).show();
                Uri uri = Uri.parse("content://" + getPackageName() + ".provider/" + item.get("name"));
                Intent intent = new Intent(Intent.ACTION_SEND)
                        .putExtra(Intent.EXTRA_STREAM, uri)
                        .setType("text/plain")
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(intent, getString(R.string.lbl_share)));
                return false;
            }
        });
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        redraw();
    }

    private String getFilesize(File file) {
        long size = file.length();
        if (size < 1024)
            return size + " B";

        NumberFormat formatter = new DecimalFormat("#0.0");
        double dSize = size / 1024;
        if (dSize < 1024)
            return formatter.format(dSize) + " KB";
        dSize = dSize / 1024;
        return formatter.format(dSize) + " MB";
    }

    private List<Map<String, String>> getContents() {
        File directory = getApplicationContext().getExternalFilesDir(null);
        List<Map<String, String>> contents = new ArrayList<>();
        if (directory != null)
        {
            for (File file: directory.listFiles())
            {
                Map<String, String> item = new HashMap<>();
                if (file.isFile())
                {
                    item.put("name", file.getName());
                    item.put("size", getFilesize(file));
                }
                contents.add(item);
            }
        }
        return contents;
    }

    private void redraw()
    {
        ((TextView) findViewById(R.id.txt_isActive)).setText(Boolean.toString(ViewModel.isRecording));

        List<Map<String, String>> contents = getContents();
        ListAdapter adapter = new SimpleAdapter(this,
                contents, R.layout.item,
                new String[] {"name", "size"},
                new int[] {R.id.txt_name, R.id.txt_size});
        setListAdapter(adapter);
    }
}
