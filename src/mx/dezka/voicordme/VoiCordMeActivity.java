package mx.dezka.voicordme;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;



import org.json.JSONException;
import org.json.JSONObject;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxStatus;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

//Implementamos OnInitListener para detectar los diferentes estados del speech
public class VoiCordMeActivity extends Activity implements OnClickListener, OnInitListener {

	private Button registroButton;
	private String nombre;
	private String asunto;
	protected static final int RESULT_SPEECH = 1;
	private TextToSpeech voz; //Con esta variable haremos que android hable
	private UtteranceProgressListener progress; //Con esta sabremos el estado en que se encuentra la voz
	public HashMap<String, String> voiceEntries; //Sirve para indicar que texto se esta hablando
	private AQuery aq;//Biblioteca para manejo de red
	private boolean saludo;//Indica si se estan preguntando datos al usuario
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.voicecordme_layout);
		registroButton = (Button) findViewById(R.id.registroButton);
		registroButton.setOnClickListener(this);
		voz = new TextToSpeech(this, this); //Inicializamos la voz del dispositivo
		verificaProgreso(); //Inializa la variable progress la cual Permitira gestionar las diferentes etapas de la voz del dispositivo : Cuando comienza a hablar, cuando hay un error y cuando termina
		voz.setOnUtteranceProgressListener(progress);//Se asigna progress a la voz que se usara para poder identificar sus estados
		saludo = false;
		
	}
	
	private void verificaProgreso(){
		progress = new UtteranceProgressListener() {
			//El parametro string de cada uno de los metodos identifica a una cadena de texto que el dispositivo dira
			@Override
			public void onStart(String arg0) {
				
			}
			
			@Override
			public void onError(String arg0) {
				
			}
			
			@Override
			public void onDone(String arg0) {
				
				
				if(arg0.equals("termino"))
					return;
				//Lanzamos el reconocimiento de voz
					Intent intent = new Intent(
							RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

					intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, "es-MX");

					try {
						startActivityForResult(intent, RESULT_SPEECH);
					} catch (ActivityNotFoundException a) {
						Toast t = Toast.makeText(getApplicationContext(),
								"Ops! Your device doesn't support Speech to Text",
								Toast.LENGTH_SHORT);
						t.show();
					}
	
				
			}
		};
		
	}

	@Override
	public void onClick(View v) {
		if(v.getId() == R.id.registroButton){
			if(saludo)
				return;
			//Nuevo hashmap que tiene un id y el valor de nombre, el cual identidicara a la cadena de texto que el dispositivo dira
			voiceEntries = new HashMap<String, String>();
			voiceEntries.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "nombre");
			voz.speak("Hola dime tu nombre", TextToSpeech.QUEUE_FLUSH,voiceEntries);
		}
		
	}

	@Override
	public void onInit(int status) {
		//Se verifica que el dispositivo haya podido hablar
		if(status != TextToSpeech.ERROR){
            voz.setLanguage(Locale.getDefault());
        }else{
       	 
       	 Log.d("Hablar", "No se pudo");
        }	
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode) {
		case RESULT_SPEECH: {
			ArrayList<String> text = data
					.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

			if (resultCode == RESULT_OK && null != data) {
				if(!saludo){
					voiceEntries = new HashMap<String, String>();
					voiceEntries.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "asunto");
					nombre = text.get(0);
					Log.d("Nombre", nombre);
					voz.speak("Hola "+text.get(0)+" Bienvenido a mejorandola cual es el motivo de tu visita", TextToSpeech.QUEUE_FLUSH,voiceEntries);
					saludo = true;
				}else{
					asunto = text.get(0);
					Log.d("Asunto", asunto);
					saludo = false;
					//Almacena la información en esta spreadsheet https://docs.google.com/spreadsheets/d/1oY7Ib5DJL9kyxfa_J2g1PjTn6xeEUC5552gEKxtS3aU/edit
					subirInfo();
				}
				
				
				
			}
			break;
		}

		}
	}
	
	public void jsonCallback(String url, JSONObject json, AjaxStatus status) {
		if (json != null) {
			try {
				
				String response = json.getString("realizado");
				if (response.equals("1")){
					voiceEntries = new HashMap<String, String>();
					voiceEntries.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "termino");
					voz.speak("Gracias por registrarte "+nombre+" en un momento te atenderemos", TextToSpeech.QUEUE_FLUSH, voiceEntries);
					
				}else{
					voiceEntries = new HashMap<String, String>();
					voiceEntries.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "termino");
					voz.speak("Ocurrio un error al registrarte "+nombre+" trata de nuevo", TextToSpeech.QUEUE_FLUSH,voiceEntries);
				}
				Log.d("Respuesta", response);
				
			} catch (JSONException e) {
				
				Toast.makeText(aq.getContext(), "Hay un problema con registro", Toast.LENGTH_LONG).show();
			} catch (Exception e) {
				Toast.makeText(aq.getContext(), "Tuvimos un problema al subir la informacion", Toast.LENGTH_LONG).show();
			}
		} 
		else {
		
			if(status.getCode() == 500){
				Toast.makeText(aq.getContext(),"Problemas en el servidor",Toast.LENGTH_SHORT).show();
			}
			else if(status.getCode() == 404){
				Toast.makeText(aq.getContext(),"Parece que hubo problemas",Toast.LENGTH_SHORT).show();
			}
			else{
				Toast.makeText(aq.getContext(),status.getCode(),Toast.LENGTH_LONG).show();
			}
		}
	}
	
	public void subirInfo(){
		aq = new AQuery(getApplicationContext());
		//Url del servicio en Google Apps Script
		String url = "https://script.google.com/macros/s/AKfycbxr2xFPdVJcnAuIz3S5jJRNzdT0jj9fGnbk07JXfzkKPcJEAnDY/exec?nombre="+nombre+"&asunto="+asunto;
		aq.ajax(url, JSONObject.class, this,"jsonCallback");
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public String getAsunto() {
		return asunto;
	}

	public void setAsunto(String asunto) {
		this.asunto = asunto;
	}
}
