package com.govind.FYP;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;

public class PrefsActivity extends PreferenceActivity {
	
	// Debug tag
	public static final String TAG = PrefsActivity.class.getSimpleName();

	
	// When preference activity is created
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		// Super
		super.onCreate(savedInstanceState);
		
		// Title of activity set
		setTitle (R.string.titlePrefs);
		
		// Preferences rendered
		Log.d(TAG, "Entered preferences");
		addPreferencesFromResource(R.xml.prefs);
	}
	
	
}
