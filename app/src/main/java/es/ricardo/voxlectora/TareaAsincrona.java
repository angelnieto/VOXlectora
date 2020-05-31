package es.ricardo.voxlectora;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import android.content.Context;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.util.Log;

import es.ricardo.voxlectora.utils.Base64;

/**
 * Clase encargada de llamar al servicio web POST de Google Drive
 */
public class TareaAsincrona extends AsyncTask<String, String, Boolean> {

	/** application context. */
	private final Context context;
	private final ResultadoActivity activity;
	int error=0;
	String excepcion="";
	String texto=null;
	int veces=0;

	//private GoogleAccountCredential credential;
	//private String URLServidor="https://googledrive.com/host/0B4O65dFE5SPsNGZTR0puWjREWTQ/";

    /**
     * Constructor con parámetro de la activity llamante
     *
     * @param activity
     */
	public TareaAsincrona(ResultadoActivity activity) {
		this.context = activity.getApplicationContext();
		this.activity=activity;
	}

	protected void onPreExecute() {
	   /* try {
			 //selectAccount();
			 	selectServiceAccount();
			 	
		} catch (Exception e) {
			excepcion+=" "+e.getMessage();
			error=R.raw.reloj;			
		}	*/
	}

	protected void onPostExecute(Boolean result) {
		activity.updateResults(texto,veces,error,excepcion);
	}

	@Override
	protected Boolean doInBackground(String... args) {
		boolean responseObtainned = false;

		activity.mp=MediaPlayer.create(context, R.raw.procesando);
		activity.mp.start();
		
		try {
			HttpClient httpClient = new DefaultHttpClient();
			HttpParams httpParameters = httpClient.getParams();
			HttpConnectionParams.setConnectionTimeout(httpParameters, 50000);
			 
		    HttpPost post = new HttpPost(context.getString(R.string.host));
		    post.setHeader(context.getString(R.string.type), context.getString(R.string.json));
		    JSONObject dato = new JSONObject();
		    dato.put(context.getString(R.string.parametroEntrada1), context.getString(R.string.esSAD));
		    dato.put(context.getString(R.string.parametroEntrada2), getIdCacharro());
		    dato.put(context.getString(R.string.parametroEntrada3), getImagenBase64());
		    StringEntity entity = new StringEntity(dato.toString());
	        post.setEntity(entity);
	        org.apache.http.HttpResponse resp = httpClient.execute(post);
	        
	        if(!isCancelled()){
		        String jsonRespuesta = EntityUtils.toString(resp.getEntity());
		        if(jsonRespuesta != null){
		        	JsonParser parser=new JsonParser();
					JsonObject jsonObject=parser.parse(jsonRespuesta.trim()).getAsJsonObject();

					Log.i(getClass().getName(), "WS response : " + jsonObject);

					if(jsonObject.get(context.getString(R.string.parametroSalida1)).getAsInt()==1){
						excepcion+=" "+jsonObject.get(context.getString(R.string.parametroSalida2)).getAsString();
						if("reloj".equals(jsonObject.get(context.getString(R.string.parametroSalida2)).getAsString()))
							error=R.raw.reloj;
						else
							error=R.raw.error_drive;
					}else{
						texto=jsonObject.get(context.getString(R.string.parametroSalida3)).getAsString();
						veces=jsonObject.get(context.getString(R.string.parametroSalida4)).getAsInt();
					}
					responseObtainned = true;
				}
	        }
		} catch (Exception e) {
			error=R.raw.error_drive;
			excepcion+=" "+e.getMessage();

            Log.e(getClass().getName(),e.getMessage(),e);
			responseObtainned = false;
         }
		 return responseObtainned;
     }

	@Override
	protected void onProgressUpdate(String... values) {
        //Método no necesario
    }

    /**
     * Mostrador de mensajes emergentes
     *
     * @param toast
     */
	 public void showToast(final String toast) {
	        activity.showToast(toast);
	  }
	 
		private String getIdCacharro(){
			return android.provider.Settings.Secure.getString(context.getContentResolver(),android.provider.Settings.Secure.ANDROID_ID);
		}

		@Override
		protected void onCancelled(){
			return;
		}

		private String getImagenBase64() throws IOException{
			File mediaStorageDir = new File(context.getFilesDir(), context.getString(R.string.raizMovil));
			java.io.File imagen = new java.io.File(mediaStorageDir.getPath() + File.separator + context.getString(R.string.nombre_imagen));
			FileInputStream fin = new FileInputStream(imagen);
			byte[] fileContent = new byte[(int)imagen.length()];

			int offset = 0;
			while ( offset < fileContent.length ) {
				int count = fin.read(fileContent, offset, fileContent.length - offset);
				offset += count;
			}

			fin.close();

			byte[] encoded = Base64.encodeBytesToBytes(fileContent);
			return new String(encoded);
		}
		
}