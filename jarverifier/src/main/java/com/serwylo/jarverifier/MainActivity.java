package com.serwylo.jarverifier;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.*;
import org.spongycastle.jce.provider.BouncyCastleProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.security.Security;
import java.security.cert.Certificate;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Toy program which is used to reproduce a bug on Android 5.0
 * whereby signature verification fails when using a different
 * security provider to that bundled with Android.
 */
public class MainActivity extends Activity {

    private BouncyCastleProvider provider = new BouncyCastleProvider();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Button btnDownload = (Button)findViewById(R.id.btn_verify);
        final CheckBox check     = (CheckBox)findViewById(R.id.check_bouncy_castle);
        final CheckBox prefer    = (CheckBox)findViewById(R.id.check_bouncy_castle_first);
        final EditText inputUri  = (EditText)findViewById(R.id.input_uri);

        btnDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadAndVerify(inputUri.getText().toString());
            }
        });

        check.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if ( isChecked ) {
                    Security.addProvider(provider);
                    prefer.setEnabled( true );
                } else {
                    Security.removeProvider( provider.getName() );
                    prefer.setEnabled( false );
                    prefer.setChecked( false );
                }
            }
        });

        prefer.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if ( isChecked ) {
                    Security.removeProvider( provider.getName() );
                    Security.insertProviderAt( provider, 1 );
                } else {
                    Security.removeProvider( provider.getName() );
                    Security.addProvider( provider );
                }
            }
        });

    }

    private void log(final String message) {
        final String TAG = "com.serwylo.jarverifier.MainActivity";
        Log.d(TAG, message);
        ((TextView)findViewById(R.id.text_log)).append(message + "\n");
        ((ScrollView)findViewById(R.id.scroller)).fullScroll(View.FOCUS_DOWN);
    }

    private void logError(final String message) {
        log( "ERROR: " + message );
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void verifyJar(File jar) {

        log("Beginning verification...");

        try {
            JarFile jarFile = new JarFile(jar, true);
            Manifest manifest = jarFile.getManifest();
            if (manifest == null) {
                logError("Could not find manifest");
                return;
            }

            verifyJarEntry(jarFile, "META-INF/MANIFEST.MF");

            Map<String, Attributes> entries = manifest.getEntries();
            if ( entries.size() == 0 ) {
                logError( "Manifest doesn't have any entries" );
                return;
            }

            for ( String entryName : entries.keySet() ) {
                verifyJarEntry( jarFile, entryName );
            }

        } catch ( SecurityException e ) {
            logError( e.getMessage() );
        } catch ( IOException e ) {
            logError( e.getMessage() );
        }

    }

    private void verifyJarEntry( JarFile file, String entryName ) throws SecurityException, IOException {

        log( "  Verifying " + entryName + "..." );

        JarEntry entry = file.getJarEntry( entryName );

        /*
         * JarFile.getInputStream() provides the signature check, even
         * though the Android docs do not mention this, the Java docs do
         * and Android seems to implement it the same:
         * http://docs.oracle.com/javase/6/docs/api/java/util/jar/JarFile.html#getInputStream(java.util.zip.ZipEntry)
         * https://developer.android.com/reference/java/util/jar/JarFile.html#getInputStream(java.util.zip.ZipEntry)
         */
        InputStream tmpInput = file.getInputStream(entry);
        StreamUtils.consume(tmpInput);
        StreamUtils.closeQuietly(tmpInput);


        Certificate[] certs = entry.getCertificates();
        if ( certs == null ) {
            log("  * No certs found");
        } else {
            log( "  * Found " + certs.length + " cert(s)" );
        }
    }

    private void downloadAndVerify(final String url) {
        new AsyncTask<Void, String, Void>() {

            private String error;
            private File file;

            @Override
            protected void onPostExecute(Void aVoid) {

                if (error != null) {
                    logError( error );
                    Toast.makeText( MainActivity.this, error, Toast.LENGTH_LONG ).show();
                } else {
                    verifyJar( file );
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
                    publishProgress("\nDownloading " + url + " to " + file.getName() + "...");
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