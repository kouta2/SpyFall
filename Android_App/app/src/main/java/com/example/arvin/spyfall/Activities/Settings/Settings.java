package com.example.arvin.spyfall.Activities.Settings;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.graphics.Color;
import android.media.Image;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.example.arvin.spyfall.Activities.Game.Game;
import com.example.arvin.spyfall.Activities.Main.Main;
import com.example.arvin.spyfall.Activities.Util.GridViewAdapter;
import com.example.arvin.spyfall.Activities.WatingRoom.WaitingRoom;
import com.example.arvin.spyfall.R;
import com.example.arvin.spyfall.Requests.CallBack;
import com.example.arvin.spyfall.Requests.ResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Settings extends AppCompatActivity implements CallBack, CustomWords.OnFragmentInteractionListener, RandomWords.OnFragmentInteractionListener {

    private TextInputEditText timer;
    private RadioGroup radioGroup;
    private ImageButton saveBtn;
    private ImageButton cancelBtn;

    private Fragment randomFrag;
    private Fragment customFrag;

    public ArrayList<String> customWords;
    private ArrayList<String> players;
    private String username;
    private String roomName;
    private int numOfPlayers;
    private ResponseHandler responseHandler;

    private static final Logger LOGGER = Logger.getLogger( Settings.class.getName() );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();

        customWords = bundle.getStringArrayList("words");
        // Set the proper fragment up in settings
        if (customWords != null) {
            customFrag = new CustomWords();
            Bundle b = new Bundle();
            b.putStringArrayList("custom_words", customWords);
            customFrag.setArguments(b);
            FragmentManager man = getFragmentManager();
            FragmentTransaction ft = man.beginTransaction();
            ft.replace(R.id.words_fragment, customFrag).commit();
        } else {
            randomFrag = new RandomWords();
            FragmentManager man = getFragmentManager();
            FragmentTransaction ft = man.beginTransaction();
            ft.replace(R.id.words_fragment, randomFrag).commit();
        }
        players = bundle.getStringArrayList("players");
        username = bundle.getString("username");
        roomName = bundle.getString("roomName");

        timer = (TextInputEditText) findViewById(R.id.timer);
        timer.setText(bundle.getString("timer"));

        radioGroup = (RadioGroup) findViewById(R.id.radio_group);

        // Set up Radio Group
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                if(i == R.id.random_radio_btn) {
                    if(customFrag.isVisible()) {
                        customWords = ((CustomWords) customFrag).getWords();
                    }
                    randomFrag = new RandomWords();
                    FragmentManager man = getFragmentManager();
                    FragmentTransaction ft = man.beginTransaction();
                    ft.replace(R.id.words_fragment, randomFrag).commit();
                } else if(i == R.id.custom_radio_btn) {
                    customFrag = new CustomWords();
                    Bundle bundle = new Bundle();
                    bundle.putStringArrayList("custom_words", customWords);
                    customFrag.setArguments(bundle);
                    FragmentManager man = getFragmentManager();
                    FragmentTransaction ft = man.beginTransaction();
                    ft.replace(R.id.words_fragment, customFrag).commit();
                }
            }
        });

        saveBtn = (ImageButton) findViewById(R.id.save_btn);

        // Set up on click listener for save Button
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean error = false;
                if(!isTimerValid()) {
                    timer.setError("Timer is required and must be an integer minutes!");
                    error = true;
                }
                if(isWordValid()) {
                    customWords = ((CustomWords) customFrag).getWords();
                } else if(customFrag != null && customFrag.isVisible()) {
                    ((CustomWords) customFrag).setWordError();
                    error = true;
                } else {
                    customWords = null;
                }
                if(!error) {
                    cancelResponseThread();
                    Intent intent = new Intent();
                    intent.putExtra("settings", true);
                    intent.putExtra("custom_words", customWords);
                    intent.putExtra("timer", timer.getText().toString().trim());
                    intent.putExtra("players", players);
                    setResult(RESULT_OK, intent);
                    finish();
                }
            }
        });


        cancelBtn = (ImageButton) findViewById(R.id.cancel_btn);

        // Sets up cancel Button on Click Listener
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                leaveSettingsNoUpdate("");
            }
        });
        setupResponseThread();
    }

    /**
     * checks to make sure format of words in customWord is valid
     * @return
     */
    private boolean isWordValid() {
        return customFrag != null && customFrag.isVisible() && ((CustomWords) customFrag).isWordValid();
    }

    /**
     * makes sure timer editText is valid
     * @return
     */
    private boolean isTimerValid() {
        try {
            String text = timer.getText().toString().trim();
            int minutes = Integer.parseInt(text);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    @Override
    /**
     * Function that is called whenever a message is received from the server
     * @param resp
     */
    public void callBack(String resp) {
        LOGGER.log(Level.INFO, "Received callback response in Settings of " + resp);
        if(resp != null) {
            try {
                JSONObject obj = new JSONObject(resp);
                String type = obj.getString("type");
                if (obj.getString("status").equals("failed")) { // message from server was a failure message
                    setupResponseThread();
                } else if (type.equals("Ended_Session")) { // message from server to end room
                    leaveSettingsAndWaitRoom(obj.getString("reason"));
                } else if (type.equals("Joined")) { // message from server notifying you someone joined room
                    JSONArray new_players = obj.getJSONArray("usernames");
                    for(int i = 0; i < new_players.length(); i++) {
                        players.add(new_players.getString(i));
                    }
                    setupResponseThread();
                } else if (type.equals("Started")) { // message from server to start a game
                    cancelResponseThread();
                    Intent intent = new Intent();
                    intent.putExtra("game_started", true);
                    intent.putExtra("obj", obj.toString());
                    intent.putExtra("players", players);
                    setResult(RESULT_OK, intent);
                    finish();
                } else if (type.equals("Leave_Session")) { // message from server saying someone left room
                    JSONArray new_players = obj.getJSONArray("usernames");
                    if(new_players.length() == 1 && new_players.getString(0).equals(username)) {
                        leaveSettingsAndWaitRoom(obj.getString("reason"));
                    } else {
                        for (int i = 0; i < new_players.length(); i++) {
                            players.remove(new_players.getString(i));
                        }
                        setupResponseThread();
                    }
                } else if (type.equals("Ended_Game")) { // message notifying user that a game in this room just ended
                    leaveSettingsNoUpdate(obj.getString("reason"));
                }
            } catch (JSONException e) {
                e.printStackTrace();
                setupResponseThread();
            }
        } else {
            // In general set up another thread to listen once this thread is complete
            setupResponseThread();
        }
    }

    /**
     * Return to Main Activity
     * @param reason
     */
    private void leaveSettingsAndWaitRoom(String reason) {
        cancelResponseThread();
        Intent intent = new Intent();
        intent.putExtra("reason", reason);
        intent.putExtra("leaving", true);
        setResult(RESULT_OK, intent);
        finish();
    }

    /**
     * Return to WaitingRoom with nothing changed
     * @param reason
     */
    private void leaveSettingsNoUpdate(String reason) {
        cancelResponseThread();
        Intent intent = new Intent();
        intent.putExtra("reason", reason);
        intent.putExtra("players", players);
        setResult(RESULT_OK, intent);
        finish();
    }

    /**
     * helper function that sets up a thread to listen to server messages
     */
    private void setupResponseThread() {
        cancelResponseThread();
        LOGGER.log(Level.INFO, "Started response thread in Settings");
        responseHandler = new ResponseHandler(Main.socket, Settings.this);
        responseHandler.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * helper to end async tasks that aren't needed anymore
     */
    private void cancelResponseThread() {
        if(responseHandler != null) {
            AsyncTask.Status status = responseHandler.getStatus();
            if (status == AsyncTask.Status.RUNNING || status == AsyncTask.Status.PENDING) {
                responseHandler.cancel(true);
                LOGGER.log(Level.INFO, "Canceling current response handler thread in Settings");
                responseHandler = null;
            }
        }
    }
}
