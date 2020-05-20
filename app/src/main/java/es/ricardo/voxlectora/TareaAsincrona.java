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
    static Logger logger = Logger.getLogger("VOXlectora");
	
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
		        if(jsonRespuesta!=null){
		        	JsonParser parser=new JsonParser();
					JsonObject jsonObject=parser.parse(jsonRespuesta.trim()).getAsJsonObject();
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
				}
	        }
		} catch (Exception e) {
			error=R.raw.error_drive;
			excepcion+=" "+e.getMessage();

            logger.log(Level.SEVERE,e.getMessage(),e);

             return false;
         }
		 return true;
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
	 
/*    private void selectAccount(){
    	
    	credential = GoogleAccountCredential.usingOAuth2(activity, DriveScopes.DRIVE);
    	
    	  AccountManager am = AccountManager.get(activity); 

    	  Account[] accounts = am.getAccountsByType("com.google");
    	  
    	  if(accounts.length>0){
    		  if ( accounts[0].name != null) {
    	          credential.setSelectedAccountName(accounts[0].name);
    	          service = getDriveService(credential);

    	        }
    	  }
      }
	    
	    private Drive getDriveService(GoogleAccountCredential credential) {
	        return new Drive.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), credential).build();
	    }	*/
	   
/*	    private void selectServiceAccount() throws GeneralSecurityException, IOException, URISyntaxException{
	    	service=getDriveService1();
	    }	
	   
	    public Drive getDriveService1() throws GeneralSecurityException,IOException, URISyntaxException {
	      
	    List<String> scopes=new ArrayList<String>();
	    scopes.add(DriveScopes.DRIVE);
	    	
      HttpTransport httpTransport = new NetHttpTransport();
      JacksonFactory jsonFactory = new JacksonFactory();
      GoogleCredential credential = new GoogleCredential.Builder()
          .setTransport(httpTransport)
          .setJsonFactory(jsonFactory)
          .setServiceAccountId(context.getString(R.string.correo))
          .setServiceAccountScopes(scopes)
          .setServiceAccountPrivateKeyFromP12File(getTempPkc12File())
          .build();
	      
	      Drive service = new Drive.Builder(httpTransport, jsonFactory,null).setHttpRequestInitializer(credential).build();
	           
	      return service;

	    }
	    
	    private java.io.File getTempPkc12File() throws IOException {
	        InputStream pkc12Stream = context.getAssets().open(context.getString(R.string.certificado));
	        java.io.File tempPkc12File = java.io.File.createTempFile(context.getString(R.string.nombre_certificado), context.getString(R.string.extension_certificado));
	        OutputStream tempFileStream = new FileOutputStream(tempPkc12File);

	        int read = 0;
	        byte[] bytes = new byte[1024];
	        while ((read = pkc12Stream.read(bytes)) != -1) {
	            tempFileStream.write(bytes, 0, read);
	        }
	        return tempPkc12File;
	    }
	    */

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