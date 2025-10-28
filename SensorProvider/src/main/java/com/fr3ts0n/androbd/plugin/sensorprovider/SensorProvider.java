package com.fr3ts0n.androbd.plugin.sensorprovider;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import com.fr3ts0n.androbd.plugin.Plugin;
import com.fr3ts0n.androbd.plugin.PluginInfo;

public class SensorProvider
		extends Plugin
		implements
		SensorEventListener,
		Plugin.DataProvider,
		Plugin.ConfigurationHandler,
		Plugin.ActionHandler
{
	// System Sensor Manager
	private SensorManager mSensorManager;

	static final PluginInfo myInfo = new PluginInfo("SensorProvider",
	                                                SensorProvider.class,
	                                                "AndrOBD sensor data provider",
	                                                "Copyright (C) 2018 by fr3ts0n",
	                                                "GPLV3+",
	                                                "https://github.com/fr3ts0n/AndrOBD-Plugin"
	);

	/**
	 * Sensor data fields to be sent
	 */
	public enum DataField
	{
		ACC_X("m/s²", -9.81, 9.81),
		ACC_Y("m/s²", -9.81, 9.81),
		ACC_Z("m/s²", -9.81, 9.81);

		private String units;
		private double min;
		private double max;

		DataField(String _units, double _min, double _max)
		{
			units = _units;
			min = _min;
			max = _max;
		}

		public static String toCsv(Context context)
		{
			StringBuilder result = new StringBuilder();
			for (DataField field : values())
			{
				result.append(field.name()).append(";");
				result.append(getStringResourceByName(context,
				                                      field.name())).append(";");
				result.append(field.min).append(";");
				result.append(field.max).append(";");
				result.append(field.units).append("\n");
			}
			return result.toString();
		}
	}

	@Override
	public void onCreate()
	{
		super.onCreate();
		// get sensor manager
		mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
		// get acceleration sensor
		Sensor mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		// register change listener to approx. 3/s
		if(mAccelerometer != null)
			mSensorManager.registerListener(this,
											 mAccelerometer,
											333000);
	}

	@Override
	public void onDestroy()
	{
		mSensorManager.unregisterListener(this);
		super.onDestroy();
	}

	@Override
	public PluginInfo getPluginInfo()
	{
		return myInfo;
	}

	@Override
	public void performConfigure()
	{
		headerSent = false;

		Intent cfgIntent = new Intent(getApplicationContext(), SettingsActivity.class);
		cfgIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(cfgIntent);
	}

	@Override
	public void performAction()
	{
		headerSent = false;
	}

	@Override
	public void onSensorChanged(SensorEvent event)
	{
		Log.i(toString(), event.toString());

		// ensure data headers are sent
		sendDataList(DataField.toCsv(this));
		// send data updates
		switch(event.sensor.getType())
		{
			case Sensor.TYPE_ACCELEROMETER:
				sendDataUpdate(DataField.ACC_X.name(), String.valueOf(event.values[0]));
				sendDataUpdate(DataField.ACC_Y.name(), String.valueOf(event.values[1]));
				sendDataUpdate(DataField.ACC_Z.name(), String.valueOf(event.values[2]));
				break;
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy)
	{
	}

}
