package es.ricardo.voxlectora;

import android.hardware.Camera.CameraInfo;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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

	private final BroadcastReceiver abcd = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			Log.i(getClass().getName(), "onReceive()");

			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(IntroActivity.this);
			if(!settings.getBoolean(getString(R.string.home), false)) {
				finish();
			}
		}

	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);

		Log.i(getClass().getName(), "onCreate()");
		
		soportaBarraTitulo = requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = settings.edit();
		if(!settings.getBoolean(getString(R.string.escuchador), false)){
			if(!settings.getBoolean(getString(R.string.back), false)) {
				editor.remove(getString(R.string.cascos));
			}
		}else {
			editor.putBoolean(getString(R.string.cascos), true);
		}
		editor.remove(getString(R.string.escuchador));
		editor.commit();

		registerReceiver(abcd, new IntentFilter("1"));
	}
		
	@Override
	protected void onResume() {
		super.onResume();

        Log.i(getClass().getName(), "onResume()");
		
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = settings.edit();
		
		editor.remove(getString(R.string.texto));
		editor.remove(getString(R.string.veces));
		editor.remove(getString(R.string.back));

		/////////	Descomentar en caso de que no se inicie la app   ///////
		//editor.remove("saltar");
		
		editor.commit();

		Log.i(getClass().getName(), "salir = " + settings.getBoolean(getString(R.string.salir), false) +
				" , saltar = " + settings.getBoolean(getString(R.string.saltar), false) +
				" , checkCascosExtraídos = " + checkCascosExtraidos(settings) +
				" , HOME = " + settings.getBoolean(getString(R.string.home), false) +
				" , cascosAnterior = " + settings.getBoolean(getString(R.string.cascosAnterior), false) +
				" , cascos = " + settings.getBoolean(getString(R.string.cascos), false));
		
		if(!settings.getBoolean(getString(R.string.salir), false) && !settings.getBoolean(getString(R.string.saltar), false)){
			if(!checkCascosExtraidos(settings)
				&& !(settings.getBoolean(getString(R.string.home), false) && settings.getBoolean(getString(R.string.cascosAnterior), false) && !settings.getBoolean(getString(R.string.cascos), false))){
				 
		    	//registro la variable de comunicación con el Escuchador
				editor.putInt(getString(R.string.activity),1);
				editor.commit();

				 Window window = getWindow();
				 window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
				            + WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
				            + WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
				            + WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
								
				setContentView(R.layout.layout_intro);
				
				if(soportaBarraTitulo)
					window.setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.layout_titulo);
				
				View overView=View.inflate(getApplicationContext(), R.layout.segundacapa, null);
				this.addContentView(overView, new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT));
				
				irPrevisualizacion = findViewById(R.id.button);
							
				v = findViewById(R.id.surfaceIntro);
				
				 if(hasBackCamera()){
						if(isOnline()){
								irPrevisualizacion.setEnabled(true);
								setVideoUri(R.raw.video_presentacion);
								continuar=true;
						 }else{
							 setVideoUri(R.raw.video_no_internet);
						 }
				}else{
					setVideoUri(R.raw.video_no_camara);
				}
				 
				v.setOnCompletionListener(new OnCompletionListener() {
						
						@Override
						public void onCompletion(MediaPlayer arg0) {
							if(continuar)
								irPrevisualizacion();
							else
								finish();
						}
				});
				
				irPrevisualizacion.setOnClickListener(new OnClickListener() {
					
					public void onClick(View v) {
						irPrevisualizacion.setEnabled(false);
						
						if(continuar){
							IntroActivity.this.v.stopPlayback();
							irPrevisualizacion();
						}
					}
				});
				
				v.start();
				
				if(settings.getBoolean(getString(R.string.cascos), false) && settings.getBoolean(getString(R.string.cascosAnterior), false)) {
					editor.remove(getString(R.string.home));
				}
				if(settings.getBoolean(getString(R.string.cascos), false)) {
					editor.putBoolean(getString(R.string.cascosAnterior), true);
				} else {
					editor.remove(getString(R.string.cascosAnterior));
				}
			}else{
				editor.remove(getString(R.string.home));
				editor.remove(getString(R.string.cascosAnterior));
								
				finish();
			}
		}else{
			editor.remove(getString(R.string.home));
				
			finish();
		}
		editor.commit();
	}

    private boolean checkCascosExtraidos(SharedPreferences settings) {
        return settings.getBoolean(getString(R.string.home), false) && settings.getBoolean(getString(R.string.cascos), false) && !settings.getBoolean(getString(R.string.cascosAnterior), false);
    }

    private void setVideoUri(int IDVideo) {
		Uri uri = Uri.parse("android.resource://"+getPackageName()+"/"+IDVideo);
		v.setVideoURI(uri);
	}

	private void irPrevisualizacion(){
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = settings.edit();
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
				SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
				if(settings.getBoolean(getString(R.string.salir), false))
					detener();
				if(settings.getBoolean(getString(R.string.saltar), false)){
					SharedPreferences.Editor editor = settings.edit();
					editor.remove(getString(R.string.saltar));
					editor.commit();
				}
					
			}
	}

	@Override
	protected void onPause(){
		super.onPause();
		Log.i(getClass().getName(), "onPause()");

		if(v!=null && v.isPlaying()) {
            v.stopPlayback();
        }
		if(Utils.isHomeButtonPressed(getApplicationContext())){
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
			SharedPreferences.Editor editor = settings.edit();
			editor.putBoolean(getString(R.string.home), true);
			editor.commit();
		}
		detener();
	}
	
	private void detener(){
		//Borro la variable centinela
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = settings.edit();
		if(settings.getBoolean(getString(R.string.salir), false)){
			editor.remove(getString(R.string.salir));
			if(!this.isFinishing()){
			    Log.i(getClass().getName(), "Finalizando la app...");
				finish();
			}
		}
		editor.commit();
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
		super.onDestroy();
        Log.i(getClass().getName(), "onDestroy()");
		
		//Borro la variable centinela
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		if(settings.getBoolean(getString(R.string.salir), false)){
			SharedPreferences.Editor editor = settings.edit();
			editor.remove(getString(R.string.salir));
			editor.commit();
		}
				
		unregisterReceiver(abcd);
	}

}