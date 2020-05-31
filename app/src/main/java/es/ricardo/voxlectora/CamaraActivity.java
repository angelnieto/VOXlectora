package es.ricardo.voxlectora;

import android.os.Bundle;
import android.app.Activity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.media.MediaPlayer;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.ShutterCallback;
import android.hardware.Camera.PictureCallback;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import es.ricardo.voxlectora.utils.Utils;

/**
 * Activity encargada del manejo de la captura de las fotografías
 */
public class CamaraActivity extends Activity implements SurfaceHolder.Callback{
	
	private Camera miCamara;
	private Button takePicture;
	private MediaPlayer mp;
	private String gradosARotar = "0";
	private float coordenadaY;
    private float coordenadaX;

    private SharedPreferences settings;
    private SharedPreferences.Editor editor;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.i(getClass().getName(), "onCreate()");

		super.onCreate(savedInstanceState);

		settings = PreferenceManager.getDefaultSharedPreferences(this);
		editor = settings.edit();
		
	    if(!settings.getBoolean(getString(R.string.salir), false) && !settings.getBoolean(getString(R.string.saltar), false) && !settings.getBoolean(getString(R.string.home), false)){
			getWindow().setFormat(PixelFormat.TRANSPARENT);
			requestWindowFeature(Window.FEATURE_NO_TITLE);
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
	
			setContentView(R.layout.layout_camara);
			
			SurfaceView mySurfaceView = findViewById(R.id.surface);
	
			SurfaceHolder mySurfaceHolder = mySurfaceView.getHolder();
			mySurfaceHolder.addCallback(this);

			View overView= View.inflate(getApplicationContext(), R.layout.segundacapa, null);
			this.addContentView(overView, new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT));
			
			takePicture = findViewById(R.id.button);
			takePicture.setText(R.string.texto_previsualizacion);
			takePicture.setOnClickListener(new OnClickListener() {
	
			public void onClick(View v) {
	
					AutoFocalizador autoFocusCallBack = new AutoFocalizador();
					miCamara.autoFocus(autoFocusCallBack);
					
					takePicture.setEnabled(false); 
				}
			});
			
			SensorEventListener sensorEventListener=new SensorEventListener() {
				
				public void onSensorChanged(SensorEvent event) {
					coordenadaX=event.values[0];
					coordenadaY=event.values[1];
					
	                WindowManager lWindowManager =  (WindowManager) getSystemService(WINDOW_SERVICE);
	               
		            int lRotation = lWindowManager.getDefaultDisplay().getRotation();
					switch (lRotation) {
						case Surface.ROTATION_90:
							if (miCamara != null) {
								miCamara.setDisplayOrientation(0);
							}
							gradosARotar(lRotation);
							break;
						case Surface.ROTATION_270:
							if (miCamara != null) {
								miCamara.setDisplayOrientation(180);
							}
							gradosARotar(lRotation);
							break;
						case Surface.ROTATION_180:
						case Surface.ROTATION_0:
							break;
					}
				}

				public void onAccuracyChanged(Sensor sensor, int accuracy) {
					//method not tested
				}
			};
			
			//El sensor es el que determina el volteado de la previsualización en pantalla 
			SensorManager sm=(SensorManager)getSystemService(Context.SENSOR_SERVICE);
			sm.registerListener(sensorEventListener, sm.getSensorList(Sensor.TYPE_ACCELEROMETER ).get(0),SensorManager.SENSOR_DELAY_NORMAL);
		} else {
			editor.remove(getString(R.string.saltar));
			editor.commit();
			
			setResult(RESULT_CANCELED);
			finish();
		}
	}
	
	@Override protected void onResume(){
		Log.i(getClass().getName(), "onResume()");
		super.onResume();

		if(takePicture!=null){
	    	takePicture.setText(R.string.texto_previsualizacion);
			takePicture.setEnabled(true);
		}
		
		if(mp!=null && mp.isPlaying()) {
			mp.stop();
		}
	}

	private void gradosARotar(int rotacion){
		if(Math.abs(coordenadaX)<1.3 && coordenadaY<-1) {            //portrait reverse
			gradosARotar = "270";
		} else if(Math.abs(coordenadaX)<1.3 && coordenadaY>1) {        //portrait
			gradosARotar = "90";
		} else if(rotacion==Surface.ROTATION_90) {                    //landscape reverse
			gradosARotar = "0";
		} else {                                                    //landscape
			gradosARotar = "180";
		}
	}

	/**
	 * Método llamado cuando la pantalla cambia a modo cámara
	 *
	 * @param holder
	 * @param format
	 * @param width
     * @param height
     */
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

		try {
			Parameters p=miCamara.getParameters();
			p.setFlashMode(Parameters.FLASH_MODE_AUTO);
			
			Camera.Size size = getMejorFormatoPantalla(width, height);
		    p.setPreviewSize(size.width, size.height);
			
			miCamara.setParameters(p);
			
			miCamara.setPreviewDisplay(holder);
			miCamara.startPreview();
			
		} catch (IOException e) {
			takePicture.setEnabled(false);
			mp=MediaPlayer.create(CamaraActivity.this, R.raw.error_camara);
			mp.start();
			Log.e(getClass().getName(), e.getMessage(),e);
		}
	}
	
	private Camera.Size getMejorFormatoPantalla(int width, int height){
	        Camera.Size result=null;    
	        Parameters p = miCamara.getParameters();
	        for (Camera.Size size : p.getSupportedPreviewSizes()) 
	            if (size.width<=width && size.height<=height) 
	                if (result==null) {
	                    result=size;
	                } else {
	                    int resultArea=result.width*result.height;
	                    int newArea=size.width*size.height;

	                    if (newArea>resultArea) 
	                        result=size;
	                }
        
	    return result;
	}

    /**
     * Método que se llama al cargar la imagen de previsualización
     *
     * @param holder
     */
	public void surfaceCreated(SurfaceHolder holder) {
			miCamara = Camera.open();
			
			mp = MediaPlayer.create(CamaraActivity.this, R.raw.camara_enfocando);
			mp.setVolume(20, 20);
            mp.start();
	}

    private class AutoFocalizador implements Camera.AutoFocusCallback {
		
		@Override
	    public void onAutoFocus(boolean success, Camera camera) {
			
			ShutterCallback myShutterCallback = new ShutterCallback() {
				
				public void onShutter() {
					MediaPlayer.create(CamaraActivity.this, R.raw.camera_click).start();
				}
			};

			PictureCallback myPictureCallback = new PictureCallback() {
			
				public void onPictureTaken(byte[] data, Camera myCamera) {
					takePicture.setText("");
				}
			};
			
			PictureCallback myJpeg = new PictureCallback() {
		
				public void onPictureTaken(byte[] data, Camera myCamera) {

					if(data != null)
						done(data);
				}
				
				void done(byte[] tempdata){
					
					String imageFilePath = getUriArchivoImagen().getPath();

					FileOutputStream out = null;
		           	try {
					       out = new FileOutputStream(imageFilePath);
		           		
					       Options options = new Options();
					       options.inJustDecodeBounds = true;
					       BitmapFactory.decodeByteArray(tempdata, 0, tempdata.length, options);
					       
					       //El nuevo tamaño máximo al que queremos reescalar la imagen
					       final int REQUIRED_SIZE=1000;

					       //Buscamos la escala correcta. Debería ser potencia de 2
					       int scale=1;
					       while(options.outWidth/scale/2>=REQUIRED_SIZE && options.outHeight/scale/2>=REQUIRED_SIZE)
					           scale*=2;

					       //Decodificamos la imagen con el nuevo tamaño
					       Options o2 = new Options();
					       o2.inSampleSize=scale;
					       Bitmap bmp=BitmapFactory.decodeByteArray(tempdata, 0, tempdata.length, o2);
					       					      
					       Matrix matrix = new Matrix();
					       matrix.postRotate(Integer.parseInt(gradosARotar));
					      			      
					       Bitmap resizedBitmap = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(),bmp.getHeight(), matrix, false);
					        
					       resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);

                            //Borro el texto del procesamiento anterior
                            editor.remove(getString(R.string.texto));
                            editor.commit();

                         //Llamo a la siguiente pantalla
                         Intent results = new Intent(CamaraActivity.this, ResultadoActivity.class);
                         startActivityForResult(results, Utils.CAMARA_ACTIVITY_REQUEST_CODE);
				    } catch (FileNotFoundException e) {
				   			mp=MediaPlayer.create(CamaraActivity.this, R.raw.error_captura);
				   			mp.start();
                        Log.e(getClass().getName(), e.getMessage(),e);
				   	 }finally{
						if(out != null){
							try {
								out.close();
							} catch( IOException e ){
                                Log.e(getClass().getName(), e.getMessage(),e);
							}
						}
					}

				}
			
			};
			
	    	if(success)
	    		MediaPlayer.create(CamaraActivity.this, R.raw.alarma_foco).start();
	    			    		
	    	miCamara.takePicture(myShutterCallback, myPictureCallback, myJpeg);
	    }
	}

    /**
     * Método que se llama al quitar la previsualización de la cámara en pantalla
     *
     * @param holder
     */
	public void surfaceDestroyed(SurfaceHolder holder) {
		miCamara.stopPreview();
		miCamara.release();
		miCamara = null;
		takePicture.setEnabled(false);
	}
	
	private Uri getUriArchivoImagen(){
		return Uri.fromFile(getImagen());
	}
	
	private File getImagen(){
		//File mediaStorageDir = new File(Environment.getExternalStorageDirectory(),getString(R.string.raizMovil));
		File mediaStorageDir = new File(this.getFilesDir(),getString(R.string.raizMovil));
		// Crear el directorio de almacenamiento si no existe
	    if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs())
	            return null;
	    
	    //Crear un archivo con la imagen
	    return new File(mediaStorageDir.getPath() + File.separator + getString(R.string.nombre_imagen));
	}

	   // Método una vez se vuelve a esta ventana
		@Override
		protected void onActivityResult(int requestCode,int resultCode,Intent data){
			Log.i(getClass().getName(), "onActivityResult()");

			if(requestCode == Utils.CAMARA_ACTIVITY_REQUEST_CODE && resultCode==RESULT_CANCELED){
					setResult(RESULT_CANCELED);
					if(settings.getBoolean(getString(R.string.salir), false) || settings.getBoolean(getString(R.string.home), false) || settings.getBoolean(getString(R.string.saltar), false)) {
						finish();
					}
			}
		}
		
		@Override
		protected void onPause(){
			Log.i(getClass().getName(), "onPause()");
			super.onPause();

			if(mp!=null && mp.isPlaying()){
				mp.stop();
				mp.release();
			}
				
			if(Utils.isHomeButtonPressed(getApplicationContext()))
				detener();
		}
		
		@Override
		protected void onDestroy(){
			Log.i(getClass().getName(), "onDestroy()");
			super.onDestroy();
		}

		private void detener(){
			editor.putBoolean(getString(R.string.home), true);
			editor.commit();
		    setResult(RESULT_CANCELED);
			
			finish(); 
		}
		
		@Override
		public void onBackPressed(){
			Log.i(getClass().getName(), "onBackPressed()");

			editor.putBoolean(getString(R.string.back), true);
			editor.commit();
			
			super.onBackPressed();
		}
}