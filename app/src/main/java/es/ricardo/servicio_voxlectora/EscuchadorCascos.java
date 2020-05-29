package es.ricardo.servicio_voxlectora;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import es.ricardo.voxlectora.R;

/**
 * Clase que escucha el evento de extracción de los cascos
 */
public class EscuchadorCascos extends BroadcastReceiver {
	
	private boolean centinela=false;

	@Override
	public void onReceive(Context context, Intent intent) {

		Log.i(getClass().getName(), "acción recibida : " + intent.getAction());

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

		if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
				int state = intent.getIntExtra(context.getString(R.string.estado), -1);

				Log.i(getClass().getName(), "state = " + state + " , centinela = " + centinela);

				if (state == 0) {
					if (centinela) {

						SharedPreferences.Editor editor = settings.edit();
						editor.putBoolean(context.getString(R.string.escuchador), true);
						editor.commit();

						Intent i = new Intent();
						i.setClassName(context.getString(R.string.paquete), context.getString(R.string.clase));
						i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						context.startActivity(i);

						Toast.makeText(context, "Auricular extraído", Toast.LENGTH_SHORT).show();
						Log.i(getClass().getName(), "Auricular extraído");

						centinela = false;
					}
				} else {
					centinela = true;

					Log.i(getClass().getName(), "activity in settings : "+ settings.getInt(context.getString(R.string.activity), 0));
					switch (settings.getInt(context.getString(R.string.activity), 0)) {
						case 0:
						case 1:
							context.sendBroadcast(new Intent("1"));
							break;
						case 2:
							context.sendBroadcast(new Intent("2"));
							break;
						case 3:
							context.sendBroadcast(new Intent("3"));
							break;
						case 4:
							context.sendBroadcast(new Intent("4"));
							break;
						default:
							break;
					}

				}
			}
	}

}