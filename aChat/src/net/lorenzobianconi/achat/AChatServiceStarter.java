package net.lorenzobianconi.achat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AChatServiceStarter extends BroadcastReceiver {
	public void onReceive(Context context, Intent intent) {
		Intent startService = new Intent(context, AChatService.class);
		context.startService(startService);
	}
}
