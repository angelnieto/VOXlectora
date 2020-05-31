package es.ricardo.voxlectora;

import android.hardware.Camera.CameraInfo;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.VideoView;

import es.ricardo.voxlectora.utils.Utils;

/**
 *  Clase encargada de manejar la Activity del video introductorio
 */

public class IntroActivity extends Activity{
	
	private static final int ACTION_VALUE=1;

	private Button irPrevisualizacion;

	private VideoView v = null;
	private boolean continuar=false;
	private boolean soportaBarraTitulo=false;

    private SharedPreferences settings;
    private SharedPreferences.Editor editor;

	@Override
	protected void onCreate(Bundle savedInstanceState){
		Log.i(getClass().getName(), "onCreate()");

		super.onCreate(savedInstanceState);

		soportaBarraTitulo = requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);

        settings = PreferenceManager.getDefaultSharedPreferences(this);
		editor = settings.edit();
        editor.remove(getString(R.string.home));

		/////////	Descomentar en caso de que no se inicie la app   ///////
		editor.remove(getString(R.string.saltar));

        editor.apply();

	}
		
	@Override
	protected void onResume() {
		Log.i(getClass().getName(), "onResume()");

		super.onResume();

		editor.remove(getString(R.string.texto));
		editor.remove(getString(R.string.veces));
		editor.remove(getString(R.string.back));

		editor.commit();

		Log.i(getClass().getName(), "salir = " + settings.getBoolean(getString(R.string.salir), false) +
				" , saltar = " + settings.getBoolean(getString(R.string.saltar), false) +
				" , HOME = " + settings.getBoolean(getString(R.string.home), false));
		
		if(!settings.getBoolean(getString(R.string.salir), false) && !settings.getBoolean(getString(R.string.saltar), false)){
				Window window = getWindow();
				window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
						+ WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
						+ WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
						+ WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

				setContentView(R.layout.layout_intro);

				if (soportaBarraTitulo)
					window.setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.layout_titulo);

				View overView = View.inflate(getApplicationContext(), R.layout.segundacapa, null);
				this.addContentView(overView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

				irPrevisualizacion = findViewById(R.id.button);

				v = findViewById(R.id.surfaceIntro);

				if (hasBackCamera()) {
					if (isOnline()) {
						irPrevisualizacion.setEnabled(true);
						setVideoUri(R.raw.video_presentacion);
						continuar = true;
					} else {
						setVideoUri(R.raw.video_no_internet);
					}
				} else {
					setVideoUri(R.raw.video_no_camara);
				}

				v.setOnCompletionListener(new OnCompletionListener() {

					@Override
					public void onCompletion(MediaPlayer arg0) {
						if (continuar) {
							irPrevisualizacion();
						} else {
							finish();
						}
					}
				});

				irPrevisualizacion.setOnClickListener(new OnClickListener() {

					public void onClick(View v) {
						irPrevisualizacion.setEnabled(false);

						if (continuar) {
							IntroActivity.this.v.stopPlayback();
							irPrevisualizacion();
						}
					}
				});

				v.start();

		}else{
			editor.remove(getString(R.string.home));
			editor.commit();
			finish();
		}
	}

    private void setVideoUri(int IDVideo) {
		Uri uri = Uri.parse("android.resource://"+getPackageName()+"/"+IDVideo);
		v.setVideoURI(uri);
	}

	private void irPrevisualizacion(){
		editor.remove(getString(R.string.home));
		editor.commit();
		
		Intent i=new Intent(this,CamaraActivity.class);
		startActivityForResult(i, ACTION_VALUE);
	}
	
	//Método una vez se vuelve a esta ventana
    @Override
	protected void onActivityResult(int requestCode,int resultCode,Intent data){
        Log.i(getClass().getName(), "onActivityResult()");

		if(requestCode == ACTION_VALUE && resultCode==RESULT_CANCELED){
				if(settings.getBoolean(getString(R.string.salir), false)) {
					detener();
				}
				if(settings.getBoolean(getString(R.string.saltar), false)){
					editor.remove(getString(R.string.saltar));
					editor.commit();
				}
					
		}
	}

	@Override
	protected void onPause(){
		Log.i(getClass().getName(), "onPause()");

		super.onPause();

		if(v!=null && v.isPlaying()) {
            v.stopPlayback();
        }
		if(Utils.isHomeButtonPressed(getApplicationContext())){
			editor.putBoolean(getString(R.string.home), true);
			editor.commit();
            detener();
		}
	}
	
	private void detener(){
		//Borro la variable centinela
		if(settings.getBoolean(getString(R.string.salir), false)){
			editor.remove(getString(R.string.salir));
            editor.commit();
    	}
        if(!this.isFinishing()){
			    Log.i(getClass().getName(), "Finalizando la app...");
				finish();
		}
	}
	
	private boolean hasBackCamera() {
        int n = android.hardware.Camera.getNumberOfCameras();
        CameraInfo info = new CameraInfo();
        for (int i = 0; i < n; i++) {
            android.hardware.Camera.getCameraInfo(i, info);
            if (info.facing == CameraInfo.CAMERA_FACING_BACK) {
                 return true;
            }
        }
        return false;
    }
	
	private boolean isOnline() {
	    ConnectivityManager cm =(ConnectivityManager) this.getSystemService(CONNECTIVITY_SERVICE);

	    return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnectedOrConnecting();
	}

    @Override
	protected void onDestroy(){
		Log.i(getClass().getName(), "onDestroy()");

		super.onDestroy();

		//Borro la variable centinela
		if(settings.getBoolean(getString(R.string.salir), false)){
			editor.remove(getString(R.string.salir));
			editor.commit();
		}
				
	}

}