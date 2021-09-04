package com.fr3ts0n.androbd.plugin.csv;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class SettingsActivity extends Activity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        redraw();
    }

    private void redraw()
    {
        ((TextView) findViewById(R.id.txt_isActive)).setText(Boolean.toString(ViewModel.isActive));
    }
}
