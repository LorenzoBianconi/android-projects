package net.lorenzobianconi.achat;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class AChatSettings extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.settings);
	}
}
