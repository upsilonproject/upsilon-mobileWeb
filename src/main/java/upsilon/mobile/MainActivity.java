package upsilon.mobile;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
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

import java.util.Locale;
import java.util.Random;

public class MainActivity extends FragmentActivity implements ActionBar.OnNavigationListener, MessageListener.Listener {
	/**
	 * ATTENTION: This was auto-generated to implement the App Indexing API.
	 * See https://g.co/AppIndexing/AndroidStudio for more information.
	 */
	private GoogleApiClient client;

	protected SharedPreferences getPrefs() {
		return getSharedPreferences("upsilon", 0);
	}

	protected String getUrl() {
		return getPrefs().getString("url", "about:blank");
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
	private MessageListener messageListener;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.messageListener = new MessageListener();
		this.messageListener.addListener(this);

		setContentView(R.layout.activity_main);

		this.engine = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
			public void onInit(int status) {

			}
		});
		this.engine.setLanguage(Locale.UK);

		try {

			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);

			final ActionBar actionBar = getActionBar();
			actionBar.setTitle(R.string.app_name);
			actionBar.setDisplayShowTitleEnabled(true);

			this.url = getPrefs().getString("url", "");

			if (url.isEmpty()) {
				promptForUrl("URL");
			}

			this.web = (WebView) findViewById(R.id.webView1);
			this.web.getSettings().setAppCacheEnabled(false);
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

	private void promptForUrl(String q) {
		final EditText input = new EditText(this);
		input.setMaxLines(1);
		input.setText(getPrefs().getString("url", ""));

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("upsilon-web URL?");
		builder.setView(input);

		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String res = input.getText().toString();
				urlChanged(res);
			}
		});

		builder.show();
	}

	private String url;

	private void urlChanged(String url) {
		this.url = url;

		SharedPreferences.Editor editor = getPrefs().edit();
		editor.putString("url", url);
		editor.commit();

		alert("", "URL set!\n\nPlease click the refresh button. ");
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

	public void refresh() {
		setStatusText("Waiting...");

		web.loadUrl(this.url);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		setText(item.toString());
		return super.onOptionsItemSelected(item);
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

	public void onMniSetUrlClicked(MenuItem mni) {
		promptForUrl("Set URL");
	}

	public void alert(String title, String content) {
		AlertDialog alertAbout = new AlertDialog.Builder(this).create();
		alertAbout.setTitle(title);
		alertAbout.setMessage(content);
		alertAbout.show();
	}

	private void speak(String msg) {
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
		et.setText("sst" + result);
	}

	public void setText(String result) {
		WebView web = (WebView) findViewById(R.id.webView1);
		web.loadData("st"+result, "text/html", null);
	}

	@Override
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		return false;
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
