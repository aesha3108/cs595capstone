package com.example.manmay.whereismyphone;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import android.util.Log;
import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;

import static android.widget.Toast.makeText;
import static edu.cmu.pocketsphinx.SpeechRecognizerSetup.defaultSetup;
public class MainActivity extends AppCompatActivity
        implements
        RecognitionListener {
    private static final int REQ_CODE_PICK_SOUNDFILE = 0;
    MediaPlayer mp;
    private Ringtone mCurrentRingtone;
    private static String KWS_SEARCH;
   // public String KEYPHRASE2;
  private static String KEYPHRASE2;
    private final int REQ_CODE_SPEECH_INPUT = 100;
    /* hotword we are looking for to activate ringtone */
    private static String KEYPHRASE = "hey android";
    final Context context = this;
    private SpeechRecognizer recognizer;
    private static String txtSpeechInput;
    private Button button;
    private HashMap<String, Integer> captions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SharedPreferences sharedPreferences=getSharedPreferences("Commands", MODE_PRIVATE);

            KEYPHRASE2=sharedPreferences.getString("input","where is my phone");
            KWS_SEARCH =sharedPreferences.getString("input","where is my phone");



        //to set uri for default ringtone on device
        Uri ringtoneUri = RingtoneManager
                .getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        mp=MediaPlayer.create(this,ringtoneUri);
        // Prepare the data for UI
        captions = new HashMap<String, Integer>();
        captions.put(KWS_SEARCH, R.string.kws_caption);

        setContentView(R.layout.activity_main);
        ((TextView) findViewById(R.id.caption_text))
                .setText("Preparing the recognizer");

        // Recognizer initialization is a time-consuming and it involves IO,
        // so we execute it in async task

        new AsyncTask<Void, Void, Exception>() {
            @Override
            protected Exception doInBackground(Void... params) {
                try {
                    Assets assets = new Assets(MainActivity.this);
                    File assetDir = assets.syncAssets();
                    setupRecognizer(assetDir);
                } catch (IOException e) {
                    return e;
                }
                return null;
            }

            @Override
            protected void onPostExecute(Exception result) {
                if (result != null) {
                    ((TextView) findViewById(R.id.caption_text))
                            .setText("Failed to init recognizer " + result);
                } else {
                    switchSearch(KWS_SEARCH);
                }
            }
        }.execute();
    }
    public void change_cmd(View view) {

        AlertDialog.Builder alert = new AlertDialog.Builder(context);
        alert.setTitle("Change Default Command"); //Set Alert dialog title here
        alert.setMessage("New Command"); //Message here

        // Set an EditText view to get user input
        final EditText input = new EditText(context);
            input.setText(KEYPHRASE2);
        alert.setView(input);

        alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                //You will get as string input data in this variable.
                // here we convert the input to a string and show in a toast.
                String srt = input.getEditableText().toString();
                SharedPreferences sharedPreferences = getSharedPreferences("Commands", MODE_PRIVATE);

                SharedPreferences.Editor editor = sharedPreferences.edit();
                clearPreferences();
                editor.putString("input", input.getText().toString());
                editor.commit();
                Intent intent = getIntent();
                finish();
                startActivity(intent);

                //recognizer.addKeyphraseSearch(KEYPHRASE, KEYPHRASE);
                //Intent intent = getIntent();
                //  finish();
                // startActivity(intent);
                // Toast.makeText(context, srt, Toast.LENGTH_LONG).show();
            } // End of onClick(DialogInterface dialog, int whichButton)
        }); //End of alert.setPositiveButton
        alert.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
                dialog.cancel();
            }
        }); //End of alert.setNegativeButton
        AlertDialog alertDialog = alert.create();
        alertDialog.show();


    }
    private void clearPreferences() {
        try {
            // clearing app data
            Runtime runtime = Runtime.getRuntime();
            runtime.exec("pm clear YOUR_APP_PACKAGE_GOES HERE");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        recognizer.cancel();
        recognizer.shutdown();
    }

    /**
     * In partial result we get quick updates about current hypothesis. In
     * keyword spotting mode we can react here, in other modes we need to wait
     * for final result in onResult.
     */
    @Override
    public void onPartialResult(Hypothesis hypothesis) {
        if (hypothesis == null)
            return;

        String text = hypothesis.getHypstr();
        if (text.equals(KEYPHRASE)) {
            recognizer.cancel();
            recognizer.addKeyphraseSearch(KWS_SEARCH, KEYPHRASE2);
            recognizer.startListening(KEYPHRASE2, 1000);
        }
            if(text.equals(KEYPHRASE2)){
                mp.start();
            }


        else
            ((TextView) findViewById(R.id.result_text)).setText(text);
    }

    /**
     * Showing google speech input dialog
     * */
    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                getString(R.string.speech_prompt));
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.speech_not_supported),
                    Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    public void onResult(Hypothesis hypothesis) {
        ((TextView) findViewById(R.id.result_text)).setText("");
        if (hypothesis != null) {
            String text = hypothesis.getHypstr();
            makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBeginningOfSpeech() {
    }

    /**
     * We stop recognizer here to get a final result
     */
    @Override
    public void onEndOfSpeech() {
        if (!recognizer.getSearchName().equals(KWS_SEARCH))
            switchSearch(KWS_SEARCH);
    }

    /**
     * Receiving speech input
     * */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != data) {

                    ArrayList<String> result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    txtSpeechInput=(result.get(0)).toString();
                }
                break;


        }
    }}

    /**
     * This callback is called when we stop the recognizer.
     */


    private void switchSearch(String hotword) {
        recognizer.stop();

        // If we are not spotting, start listening with timeout (3seconds).
        if (hotword.equals(KWS_SEARCH))
            recognizer.startListening(hotword);
        else
            recognizer.startListening(hotword,10000);



        String caption = getResources().getString(captions.get(hotword));
        ((TextView) findViewById(R.id.caption_text)).setText(caption);
    }

    private void setupRecognizer(File assetsDir) throws IOException {
        // The recognizer can be configured to perform multiple searches
        // of different kind and switch between them

        recognizer = defaultSetup()
                .setAcousticModel(new File(assetsDir, "en-us-ptm"))
                .setDictionary(new File(assetsDir, "cmudict-en-us.dict"))

                        // To disable logging of raw audio comment out this call (takes a lot of space on the device)
                .setRawLogDir(assetsDir)

                        // Threshold to tune for keyphrase to balance between false alarms and misses
                //.setKeywordThreshold((float) (1e-20))
                .setKeywordThreshold((float) (1e-1-30f))
                        // Use context-independent phonetic search, context-dependent is too slow for mobile
                .setBoolean("-allphone_ci", true)

                .getRecognizer();
        recognizer.addListener(this);


        // Create hotword-activation search.
        recognizer.addKeyphraseSearch(KWS_SEARCH, KEYPHRASE);


    }

    @Override
    public void onError(Exception error) {
        ((TextView) findViewById(R.id.caption_text)).setText(error.getMessage());
    }

    @Override
    public void onTimeout() {
        switchSearch(KWS_SEARCH);
        Intent intent = getIntent();
        finish();
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }


    private void playRingtone(Ringtone newRingtone) {

        if (null != mCurrentRingtone && mCurrentRingtone.isPlaying())
            mCurrentRingtone.stop();

        mCurrentRingtone = newRingtone;

        if (null != newRingtone) {
            mCurrentRingtone.play();
        }
    }
    public void stopMusic(View view) {
      mp.stop();

       Intent intent = getIntent();
       finish();
       startActivity(intent);
    }
    @Override
    public void onBackPressed() {
        Log.d("CDA", "onBackPressed Called");
        Intent setIntent = new Intent(Intent.ACTION_MAIN);
        setIntent.addCategory(Intent.CATEGORY_HOME);
        setIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(setIntent);
    }

   }
