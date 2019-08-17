package com.fr3ts0n.androbd.plugin.gpsprovider;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.fr3ts0n.androbd.plugin.Plugin;
import com.fr3ts0n.androbd.plugin.PluginInfo;

public class GpsProvider
		extends Plugin
		implements
		LocationListener,
		Plugin.DataProvider,
		Plugin.ConfigurationHandler,
		Plugin.ActionHandler
{
	// System Location Manager
	LocationManager locationManager;

	static final PluginInfo myInfo = new PluginInfo("GpsProvider",
	                                                GpsProvider.class,
	                                                "AndrOBD GPS provider",
	                                                "Copyright (C) 2018 by fr3ts0n",
	                                                "GPLV3+",
	                                                "https://github.com/fr3ts0n/AndrOBD-Plugin"
	);

	/**
	 * GPS data fields to be sent
	 */
	public enum GpsField
	{
		GPS_LATITUDE("°", 0, 360),
		GPS_LONGITUDE("°", 0, 360),
		GPS_ALTITUDE("m", 0, 8848),
		GPS_BEARING("°", 0, 360),
		GPS_SPEED("km/h", 0, 250);

		private String units;
		private double min;
		private double max;

		GpsField(String _units, double _min, double _max)
		{
			units = _units;
			min = _min;
			max = _max;
		}

		public static String toCsv()
		{
			StringBuilder result = new StringBuilder();
			for (GpsField field : values())
			{
				result.append(field.name()).append(";");
				result.append(field.name()).append(";");
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

		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		// check permissions
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
		{
			if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) !=
					PackageManager.PERMISSION_GRANTED &&
					checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) !=
							PackageManager.PERMISSION_GRANTED)
			{
				Log.e(getPackageName(), "Location permissions missing");
				return;
			}
		}
		// Register the listener with the Location Manager to receive location updates
		try
		{
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
		}
		catch (Exception ex)
		{
			Log.e(getPackageName(), ex.toString());
		}
		try
		{
			locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
		}
		catch (Exception ex)
		{
			Log.e(getPackageName(), ex.toString());
		}
	}

	@Override
	public void onDestroy()
	{
		locationManager.removeUpdates(this);
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
		// ensure header will be sent again
		headerSent = false;

		// Attempt to get fine GPS location ...
		// check permissions
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
		{
			if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) !=
					PackageManager.PERMISSION_GRANTED &&
					checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) !=
							PackageManager.PERMISSION_GRANTED)
			{
				Log.e(getPackageName(), "Location permissions missing");
				return;
			}
		}
		Location loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

		// If no fine location is available ...
		if (loc == null)
		{
			// Attempt to get coarse network location
			loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		}
		// send data update
		onLocationChanged(loc);
	}

	@Override
	public void onLocationChanged(Location location)
	{
		// ensure data list is sent
		sendDataList(GpsField.toCsv());
		// if a valid location is provided ...
		if (location != null)
		{
			// send data updates of location parameters
			Log.i(toString(), location.toString());
			sendDataUpdate(GpsField.GPS_LATITUDE.name(), String.valueOf(location.getLatitude()));
			sendDataUpdate(GpsField.GPS_LONGITUDE.name(), String.valueOf(location.getLongitude()));
			sendDataUpdate(GpsField.GPS_ALTITUDE.name(), String.valueOf(location.getAltitude()));
			sendDataUpdate(GpsField.GPS_BEARING.name(), String.valueOf(location.getBearing()));
			sendDataUpdate(GpsField.GPS_SPEED.name(), String.valueOf(location.getSpeed()));
		}
		else
		{
			Log.e(toString(), "NO GPS location");
		}
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras)
	{
		if (status == LocationProvider.AVAILABLE)
		{
			// check permissions
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
			{
				if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) !=
						PackageManager.PERMISSION_GRANTED &&
						checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) !=
								PackageManager.PERMISSION_GRANTED)
				{
					Log.e(getPackageName(), "Location permissions missing");
					return;
				}
			}
			onLocationChanged(locationManager.getLastKnownLocation(provider));
		}
	}

	@Override
	public void onProviderEnabled(String provider)
	{
	}

	@Override
	public void onProviderDisabled(String provider)
	{
	}

}
