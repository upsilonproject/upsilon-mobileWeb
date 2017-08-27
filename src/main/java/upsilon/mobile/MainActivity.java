package upsilon.mobile;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.NotificationCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.webkit.SslErrorHandler;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;

import java.util.List;
import java.util.Locale;

import upsilon.mobile.upsilon.mobile.backend.AmqpHandler;

public class MainActivity extends AppCompatActivity implements AmqpHandler.Listener {
	/**
	 * ATTENTION: This was auto-generated to implement the App Indexing API.
	 * See https://g.co/AppIndexing/AndroidStudio for more information.
	 */
	private GoogleApiClient client;

	protected SharedPreferences getPrefs() {
		return PreferenceManager.getDefaultSharedPreferences(this);
	}

	public void onConnected() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				MainActivity.this.setStatusText("Connected");
				MainActivity.this.getConnStatus().setText("Connected");
				MainActivity.this.getConnStatus().setBackgroundColor(MainActivity.this.getResources().getColor(R.color.color_connected));
			}
		});
	}

	public void onDisconnected() {
		runOnUiThread(new Runnable() {
			public void run() {
				setStatusText("Disconnected");
				getConnStatus().setText("Disconnected");
				getConnStatus().setBackgroundColor(getResources().getColor(R.color.color_disconnected));
			}
		});
	}

	public void onError(final String message) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				MainActivity.this.setStatusText("Error");
				MainActivity.this.getConnStatus().setText("Error");
				MainActivity.this.getConnStatus().setBackgroundColor(MainActivity.this.getResources().getColor(R.color.color_error));

				MainActivity.this.alert("Exception", message);
			}
		});
	}

	private TextView getConnStatus() {
		return (TextView) this.findViewById(R.id.action_connstatus);
	}

	private TextToSpeech engine;
	private AmqpHandler messageListener;

	private static final int SPEECH_REQUEST_CODE = 0;

	public void setupSpeech() {
		Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

		startActivityForResult(intent, SPEECH_REQUEST_CODE);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
			List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

			String sentenace = "";

			for (String word : results) {
				sentenace += word + " ";
			}

			this.speak("You said; " + sentenace);
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		if (getIntent().getAction() != null && getIntent().getAction().equals("com.google.android.gms.actions.SEARCH_ACTION")) {
			String query = getIntent().getStringExtra(SearchManager.QUERY);

			speak("You said: " +  query);
			return;
		}

		this.messageListener = AmqpHandler.getInstance();
		this.messageListener.setHostname(getPrefs().getString("hostname_amqp", "localhost"));
		this.messageListener.addListener(this);

		setContentView(R.layout.activity_main);

		try {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);

			if (getUrl().isEmpty()) {
				this.setText("please set upsilon-web URL in settings");
			}

			Toolbar tb = (Toolbar) findViewById(R.id.toolbar);
			tb.setNavigationIcon(R.drawable.ic_launcher);
			//tb.setLogo(R.drawable.ic_launcher);
			setSupportActionBar(tb);
			//getSupportActionBar().setIcon(R.drawable.ic_launcher);
			//getSupportActionBar().setDisplayShowHomeEnabled(true);

			this.web = (WebView) findViewById(R.id.webView1);
			this.web.getSettings().setAppCacheEnabled(false);
			this.web.getSettings().setJavaScriptEnabled(true);
			this.web.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
			this.web.setWebViewClient(new WebViewClient() {
				@Override
				public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
					Log.v("sslError", error.toString());
					handler.proceed();
				}
			});

			refresh();
		} catch (Exception e) {
			alert("global exception", e.toString());
		}

		// ATTENTION: This was auto-generated to implement the App Indexing API.
		// See https://g.co/AppIndexing/AndroidStudio for more information.
		client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
	}

	public String getUrl() {
		return getPrefs().getString("hostname_web", "about:blank");
	}

	private WebView web;

	public void onClearCache(MenuItem mniClear) {
		this.web.clearCache(true);

		AlertDialog alertClear = new AlertDialog.Builder(this).create();
		// alertClear.setTitle("Cache Cleared");
		alertClear.setMessage("Cache Cleared!");
		alertClear.show();

		refresh();
	}

	public void onMniHomeClicked(MenuItem mniNodes) {
		web.loadUrl(getUrl() + "/index.php");
	}

	public void onMniNodesClicked(MenuItem mniNodes) {
		web.loadUrl(getUrl() + "/listNodes.php");
	}

	public void onMniSettingsClicked(MenuItem mniSettings) {
		Intent isettings = new Intent(this, SettingsActivity.class);

		startActivity(isettings);
	}

	public void refresh() {
		Log.w("refreshing", "url: " + getUrl());
		setStatusText("Waiting...");

		web.loadUrl(getUrl());
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		DrawerLayout o = (DrawerLayout) findViewById(R.id.drawer);
		o.openDrawer(findViewById(R.id.nav_view));

		return true;
	}

	public void onMniAboutClicked(MenuItem about) {
		String version;
		try {
			version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
		} catch (Exception e) {
			version = "???";
		}

		alert("About", "Version: " + version);

		speak("Hello World");
	}

	public void alert(String title, String content) {
		AlertDialog alertAbout = new AlertDialog.Builder(this).create();
		alertAbout.setTitle(title);
		alertAbout.setMessage(content);
		alertAbout.show();
	}

	private void speak(String msg) {
		if (engine == null) {
			this.engine = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
				public void onInit(int status) {

				}
			});
			this.engine.setLanguage(Locale.UK);
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			engine.speak(msg, TextToSpeech.QUEUE_FLUSH, null, msg);
		} else {
			engine.speak(msg, TextToSpeech.QUEUE_FLUSH, null);
		}
	}

	@Override
	public void onNotification(String message) {
		AudioManager am = (AudioManager)getSystemService(Context.AUDIO_SERVICE);

		if (am.isWiredHeadsetOn()) {
			this.speak(message);
		} else {
			NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
			builder.setSmallIcon(R.drawable.ic_launcher);
			builder.setContentTitle("Upsilon");
			builder.setContentText(message);

			NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			manager.notify(message.hashCode(), builder.build());
		}
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
	}

	public void onClickRefresh(MenuItem mnu) {
		refresh();
	}

	public void onClickConn(MenuItem mni) {
		if (this.messageListener.isConnected()) {
			this.messageListener.sendHeartbeat();
		} else {
			this.messageListener.reconnect();
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public void setStatusText(String result) {
		TextView et = (TextView) findViewById(R.id.TextView1);
		et.setText("sst: " + result);
	}

	public void setText(String result) {
		WebView web = (WebView) findViewById(R.id.webView1);
		web.loadData("st: "+result, "text/html", null);
	}

	/**
	 * ATTENTION: This was auto-generated to implement the App Indexing API.
	 * See https://g.co/AppIndexing/AndroidStudio for more information.
	 */
	public Action getIndexApiAction() {
		Thing object = new Thing.Builder()
				.setName("Main Page") // TODO: Define a title for the content shown.
				// TODO: Make sure this auto-generated URL is correct.
				.setUrl(Uri.parse("http://upsilon-project.co.uk"))
				.build();
		return new Action.Builder(Action.TYPE_VIEW)
				.setObject(object)
				.setActionStatus(Action.STATUS_TYPE_COMPLETED)
				.build();
	}

	@Override
	public void onStart() {
		super.onStart();

		// ATTENTION: This was auto-generated to implement the App Indexing API.
		// See https://g.co/AppIndexing/AndroidStudio for more information.
		client.connect();
		AppIndex.AppIndexApi.start(client, getIndexApiAction());
	}

	@Override
	public void onStop() {
		super.onStop();

		// ATTENTION: This was auto-generated to implement the App Indexing API.
		// See https://g.co/AppIndexing/AndroidStudio for more information.
		AppIndex.AppIndexApi.end(client, getIndexApiAction());
		client.disconnect();
	}
}
