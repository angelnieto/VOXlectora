package es.ricardo.voxlectora;

import java.util.ArrayList;

import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.content.SharedPreferences;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.gesture.Prediction;
import android.gesture.GestureOverlayView.OnGesturePerformedListener;
import android.graphics.Rect;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import es.ricardo.servicio_voxlectora.EscuchadorCascos;
import es.ricardo.servicio_voxlectora.Servicio;
import es.ricardo.voxlectora.utils.Utils;

/**
 * Clase encargada de gestionar la Activity propia de la pantalla de confirmación
 */
public class ConfirmacionActivity extends Activity implements OnGesturePerformedListener{

	private TextView tv;
	
	GestureOverlayView gestosView;
	GestureLibrary libreriaGestos;
	
	MediaPlayer mp;
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
	}
	
	@Override
	protected void onResume() {
		Log.i(getClass().getName(), "onResume()");

		super.onResume();
		
		setContentView(R.layout.layout_confirmacion);
		
		if(soportaBarraTitulo) {
            getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.layout_titulo);
        }
		//Agrego la capa para detectar gestos
		 gestosView=(GestureOverlayView) View.inflate(getApplicationContext(), R.layout.capa_gestos, null);
		 libreriaGestos = GestureLibraries.fromRawResource(this, R.raw.gestures);
		 ConfirmacionActivity.this.addContentView(gestosView, new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT));
		 
		 if (!libreriaGestos.load()) 
		     this.finish();

	     String textoAlmacenado=settings.getString(getString(R.string.texto), null);
	     
		 if(textoAlmacenado!=null && !"".equals(textoAlmacenado)) {
			 mostrarTexto(textoAlmacenado);
		 } else {
			 mostrarTexto(getString(R.string.nada));
		 }
		 mostrarVeces();
		 establecerFuente();
		 
		 String accionLanzamiento=settings.getString(getString(R.string.lanzamiento), null);
		 
		 if(accionLanzamiento==null) {
			 mp = MediaPlayer.create(this, R.raw.vincular_confirmacion);
		 } else {
			 mp = MediaPlayer.create(this, R.raw.desvincular_confirmacion);
		 }
		 mp.setOnCompletionListener(new OnCompletionListener() {

				public void onCompletion(MediaPlayer mp) {	
					gestosView.addOnGesturePerformedListener(ConfirmacionActivity.this);
				}
				
			});
		  
		  mp.start();
	}

	@Override
	public void onGesturePerformed(GestureOverlayView overlay, Gesture gesture) {
		ArrayList<Prediction> predictions = libreriaGestos.recognize(gesture);
		
		if (!predictions.isEmpty() && predictions.get(0).score > 5.0) {
			if(mp.isPlaying())
				mp.stop();
			//mp.release();
			
		    String result = predictions.get(0).name;
		 
		     if(getString(R.string.antihorario).equalsIgnoreCase(result)){
		    	//Introduzco la variable centinela que le indique al servicio que debe lanzar la aplicación al retirar los auriculares
		         String accionLanzamiento=settings.getString(getString(R.string.lanzamiento), null);

				 IntentFilter receiverFilter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
				 Intent serviceIntent = new Intent(getApplicationContext(), Servicio.class);
				 if(accionLanzamiento==null) {
					 editor.putString(getString(R.string.lanzamiento), getString(R.string.cascos));

					 if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
						 Log.i(getClass().getName(), "Starting the service in >=26 Mode");
						 getApplicationContext().startService(serviceIntent);
					 } else {
						 Log.i(getClass().getName(), "Starting the service in <26 Mode");
						 getApplicationContext().startService(serviceIntent);
					 }
				 } else {
					 editor.remove(getString(R.string.lanzamiento));

					 if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
						 Log.i(getClass().getName(), "Stopping the service in >=26 Mode");
						 getApplicationContext().stopService(serviceIntent);
					 } else {
						 Log.i(getClass().getName(), "Stopping the service in <26 Mode");
						 getApplicationContext().stopService(serviceIntent);
					 }
				 }
		         editor.commit();
		         
		         gestosView.removeOnGesturePerformedListener(ConfirmacionActivity.this);
		         
		         salir();
		     }else if(getString(R.string.vaiven).equalsIgnoreCase(result)){
		    	 //showToast("vaivén");
		    	 volver();
		     }
		}
	}

	/**
	 * Muestra mensajes emergentes
	 *
	 * @param toast
     */
 	 public void showToast(final String toast) {
   	    runOnUiThread(new Runnable() {
   	      @Override
   	      public void run() {
   	        Toast.makeText(getApplicationContext(), toast, Toast.LENGTH_SHORT).show();
   	      }
   	    });
   	  }
 	 
 	private void mostrarTexto( String text ){
		tv = findViewById(R.id.texto1);
		tv.setTextColor(getResources().getColor(R.color.morado));
		tv.setText(text);
	}
 	
 	private void volver(){
		finish();
	}
 	
	private void salir(){
		if(mp!=null && mp.isPlaying()) {
			mp.stop();
		}
		//mp = MediaPlayer.create(ConfirmacionActivity.this, R.raw.reiniciar);
		//mp.setOnCompletionListener(new OnCompletionListener() {

//				public void onCompletion(MediaPlayer mp) {
					MediaPlayer player = MediaPlayer.create(ConfirmacionActivity.this, R.raw.salir);
					player.setOnCompletionListener(new OnCompletionListener() {
										
						public void onCompletion(MediaPlayer mp) {
							//Dado que la ResultadoActivity se destruye al entrar en esta Activity, tengo que indicarle cuando vuelvo lo que debe hacer
							editor.putBoolean(getString(R.string.salir), true);
							editor.commit();
						
							setResult(RESULT_CANCELED);
							finish();
						}
					});
					player.start();
//				}
				
//			});
		  
//		  mp.start();
	}
	
	private void mostrarVeces(){
		
		if(!Boolean.parseBoolean(getString(R.string.esSAD))){
			int veces=settings.getInt(getString(R.string.veces), 0);
	        if(veces!=0){
	        	//Escalo la barra inferior acorde a la resolución de la pantalla
		    	WindowManager windowManager =  (WindowManager) getSystemService(WINDOW_SERVICE);
		    	Rect pantalla=new Rect();
		    	windowManager.getDefaultDisplay().getRectSize(pantalla);
		    	        	
	        	LinearLayout marcador=(LinearLayout) findViewById(R.id.rayos1);
	        	marcador.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, Math.round(pantalla.height()/12),Gravity.BOTTOM));
	        	
	        	for(int i=15;i>15-veces && i>0;i--){
	        		ImageView imagen= (ImageView) marcador.getChildAt(i-1);
	        		imagen.setImageResource(R.drawable.raya_off);
	        	}
	        	
	        	findViewById(R.id.rayos1).setVisibility(View.VISIBLE);
	        }
		}
	}
	
	private void establecerFuente() {
		WindowManager windowManager =  (WindowManager) getSystemService(WINDOW_SERVICE);
    	Rect pantalla=new Rect();
    	windowManager.getDefaultDisplay().getRectSize(pantalla);
    	
		if(pantalla.width()<600)
    		tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, Float.parseFloat(getString(R.string.telefono)));
    	else if(pantalla.width()<800)
    		tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, Float.parseFloat(getString(R.string.hibrido)));
    	else
    		tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, Float.parseFloat(getString(R.string.tableta)));
	}
	
	@Override
	protected void onPause(){
		Log.i(getClass().getName(), "onPause()");

		super.onPause();

		if(mp!=null && mp.isPlaying())
			mp.stop();
		
		if(Utils.isHomeButtonPressed(getApplicationContext())){
			editor.putBoolean(getString(R.string.home), true);
		    editor.putBoolean(getString(R.string.saltar), true);
		    editor.commit();
		     
		    this.setResult(RESULT_CANCELED);
			
			finish();
        }
	}
	
	@Override
	protected void onDestroy(){
		Log.i(getClass().getName(), "onDestroy()");
		super.onDestroy();
	}

}