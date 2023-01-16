package upsilon.mobile;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.http.SslError;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.webkit.SslErrorHandler;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import androidx.appcompat.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import java.util.List;
import java.util.Locale;

import upsilon.mobile.upsilon.mobile.backend.AmqpHandler;

public class MainActivity extends AppCompatActivity implements AmqpHandler.Listener {

	protected SharedPreferences getPrefs() {
		return PreferenceManager.getDefaultSharedPreferences(this);
	}

	public void onConnected() {
		runOnUiThread(() -> {
			MainActivity.this.setStatusText(getResources().getString(R.string.conn_connected));
			MainActivity.this.getConnStatus().setText(getResources().getString(R.string.conn_connected));
			MainActivity.this.getConnStatus().setBackgroundColor(MainActivity.this.getColor(R.color.color_connected));
		});
	}

	public void onDisconnected() {
		runOnUiThread(() -> {
			setStatusText(getResources().getString(R.string.conn_disconnected));
			getConnStatus().setText(getResources().getString(R.string.conn_disconnected));
			getConnStatus().setBackgroundColor(MainActivity.this.getColor(R.color.color_disconnected));
		});
	}

	public void onError(final String message) {
		runOnUiThread(() -> {
			MainActivity.this.setStatusText(getResources().getString(R.string.conn_error));
			MainActivity.this.getConnStatus().setText(getResources().getString(R.string.conn_error));
			MainActivity.this.getConnStatus().setBackgroundColor(MainActivity.this.getColor(R.color.color_error));

			MainActivity.this.alert("Exception", message);
		});
	}

	private TextView getConnStatus() {
		return (TextView) this.findViewById(R.id.action_connstatus);
	}

	private TextToSpeech engine;
	private AmqpHandler messageListener;

	private static final int SPEECH_REQUEST_CODE = 0;

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
			List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

			StringBuilder sentence = new StringBuilder();

			for (String word : results) {
				sentence.append(word);
				sentence.append(" ");
			}

			this.speak("You said; " + sentence);
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		supportRequestWindowFeature(Window.FEATURE_NO_TITLE);

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
			tb.setNavigationIcon(R.mipmap.ic_launcher);
			//tb.setLogo(R.drawable.ic_launcher);
			setSupportActionBar(tb);

			//getSupportActionBar().setIcon(R.drawable.ic_launcher);
			//getSupportActionBar().setDisplayShowHomeEnabled(true);

			this.web = (WebView) findViewById(R.id.webView1);
			//this.web.getSettings().setAppCacheEnabled(false);
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
		setNavOpen(false);
	}

	public void onMniNodesClicked(MenuItem mniNodes) {
		web.loadUrl(getUrl() + "/listNodes.php");
		setNavOpen(false);
	}

	public void onMniSettingsClicked(MenuItem mniSettings) {
		Intent settings = new Intent(this, SettingsActivity.class);

		startActivity(settings);
	}

	public void refresh() {
		Log.w("refreshing", "url: " + getUrl());
		setStatusText("Waiting...");

		web.loadUrl(getUrl());
	}

	private void setNavOpen(boolean isOpen) { 
		DrawerLayout o = (DrawerLayout) findViewById(R.id.drawer);

		if (isOpen) { 
			o.openDrawer(findViewById(R.id.nav_view));
		} else {
			o.closeDrawer(findViewById(R.id.nav_view));
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		setNavOpen(true);

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
			this.engine = new TextToSpeech(getApplicationContext(), status -> {

			});
			this.engine.setLanguage(Locale.UK);
		}

		engine.speak(msg, TextToSpeech.QUEUE_FLUSH, null, msg);
	}

	@Override
	public void onNotification(String message) {
		//AudioManager am = (AudioManager)getSystemService(Context.AUDIO_SERVICE);

		final boolean isHeadset = true;

		if (isHeadset) {
			this.speak(message);
		} else {
			NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "channelId");
			builder.setSmallIcon(R.mipmap.ic_launcher);
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
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public void setStatusText(String result) {
		TextView et = (TextView) findViewById(R.id.TextView1);
		et.setText(result);
	}

	public void setText(String result) {
		WebView web = (WebView) findViewById(R.id.webView1);
		web.loadData("st: "+result, "text/html", null);
	}

}
