package com.serwylo.jarverifier;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;


public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Button btnDownload = (Button)findViewById(R.id.btn_verify);
        final EditText inputUri  = (EditText)findViewById(R.id.input_uri);

        btnDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadAndVerify(inputUri.getText().toString());
            }
        });
    }

    private void log(String message) {
        final String TAG = "com.serwylo.jarverifier.MainActivity";
        Log.d(TAG, message);
        ((TextView)findViewById(R.id.text_log)).append(message + "\n");
    }

    private void verify() {

        log("Beginning verification...");
    }

    private void downloadAndVerify(final String url) {
        new AsyncTask<Void, Void, Void>() {

            private String error;

            @Override
            protected void onPostExecute(Void aVoid) {

                if (error != null) {
                    log("ERROR: " + error);
                    Toast.makeText(MainActivity.this, error, Toast.LENGTH_LONG).show();
                } else {
                    verify();
                }

            }

            private File prepareFile(URL url) {
                return prepareFile(url.getFile());
            }

            private File prepareFile(String fileName) {
                if (TextUtils.isEmpty(fileName)) {
                    fileName = "toVerify.jar";
                }

                File file = new File(getFilesDir(), fileName);

                if (file.exists()) {
                    if (!file.delete()) {
                        return prepareFile( "_" + fileName );
                    }
                }

                return file;
            }

            @Override
            protected Void doInBackground(Void... params) {

                InputStream input = null;
                FileOutputStream output = null;

                try {
                    URL urlToDownload = new URL(url);
                    File file = prepareFile(urlToDownload);
                    URLConnection conn = urlToDownload.openConnection();
                    input = conn.getInputStream();
                    output = new FileOutputStream(file);
                    log("Downloading " + url + " to " + file.getName() + "...");
                    StreamUtils.copy(input, output);
                    log("Download complete!");
                } catch (MalformedURLException e) {
                    error = url + " is not a valid URL.";
                } catch (IOException e) {
                    error = "Error downloading file: " + e.getMessage();
                } finally {
                    StreamUtils.closeQuietly(input);
                    StreamUtils.closeQuietly(output);
                }

                return null;

            }

        }.execute();
    }


}