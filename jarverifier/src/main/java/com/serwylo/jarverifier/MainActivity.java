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
import java.security.cert.Certificate;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;


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

    private void log(final String message) {
        final String TAG = "com.serwylo.jarverifier.MainActivity";
        Log.d(TAG, message);
        ((TextView)findViewById(R.id.text_log)).append(message + "\n");
    }

    private void verify(File jar) {

        log("Beginning verification...");

        try {
            JarFile jarFile = new JarFile(jar, true);
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                log( "  Verifying " + entry.getName() + "..." );


                /*
                 * JarFile.getInputStream() provides the signature check, even
                 * though the Android docs do not mention this, the Java docs do
                 * and Android seems to implement it the same:
                 * http://docs.oracle.com/javase/6/docs/api/java/util/jar/JarFile.html#getInputStream(java.util.zip.ZipEntry)
                 * https://developer.android.com/reference/java/util/jar/JarFile.html#getInputStream(java.util.zip.ZipEntry)
                 */
                InputStream tmpInput = jarFile.getInputStream(entry);
                StreamUtils.closeQuietly(tmpInput);


                Certificate[] certs = entry.getCertificates();
                if ( certs == null ) {
                    log( "  * NO CERTS FOUND!" );
                    Toast.makeText(this, "No certificates found in " + jar.getName() + "!", Toast.LENGTH_LONG ).show();
                } else {
                    log( "    * Found " + certs.length + " certs" );
                }
            }
        } catch (IOException e) {
            log( "Error verifying: " + e);
        }


    }

    private void downloadAndVerify(final String url) {
        new AsyncTask<Void, String, Void>() {

            private String error;
            private File file;

            @Override
            protected void onPostExecute(Void aVoid) {

                if (error != null) {
                    log("ERROR: " + error);
                    Toast.makeText(MainActivity.this, error, Toast.LENGTH_LONG).show();
                } else {
                    verify(file);
                }

            }

            @Override
            protected void onProgressUpdate(String... values) {
                log(values[0]);
            }

            private File prepareFile(URL url) {
                return prepareFile(new File(url.getFile()).getName());
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
                    file = prepareFile(urlToDownload);
                    publishProgress("Downloading " + url + " to " + file.getName() + "...");
                    URLConnection conn = urlToDownload.openConnection();
                    input = conn.getInputStream();
                    output = openFileOutput(file.getName(), MODE_PRIVATE);
                    StreamUtils.copy(input, output);
                    publishProgress("Download complete!");
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