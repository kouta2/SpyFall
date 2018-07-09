package com.example.arvin.spyfall.Activities.WatingRoom;

import android.content.Intent;
import android.graphics.Color;
import android.icu.text.IDNA;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.GridView;
import android.widget.TextView;

import com.example.arvin.spyfall.Activities.Game.Game;
import com.example.arvin.spyfall.Activities.Main.Main;
import com.example.arvin.spyfall.Activities.Settings.Settings;
import com.example.arvin.spyfall.Activities.Util.GridViewAdapter;
import com.example.arvin.spyfall.R;
import com.example.arvin.spyfall.Requests.CallBack;
import com.example.arvin.spyfall.Requests.RequestHandler;
import com.example.arvin.spyfall.Requests.ResponseHandler;
import com.github.clans.fab.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WaitingRoom extends AppCompatActivity implements CallBack {

    private String response;
    private String username;
    private String roomName;
    private ArrayList<String> players;

    private TextView waiting;
    private TextView roomLabel;
    private GridView people;
    private FloatingActionButton settingsBtn;
    private FloatingActionButton startBtn;
    private FloatingActionButton leaveBtn;
    private FloatingActionButton endRoomBtn;
    private TextView results;

    private static final Logger LOGGER = Logger.getLogger( WaitingRoom.class.getName() );
    private int count = 1;
    private final int delay = 1000; //milliseconds
    private int numOfPlayers = 0;

    private ResponseHandler responseHandler;
    private String timer = "1";
    private ArrayList<String> words;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waiting_room);

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        response = (String) bundle.get("response");
        username = (String) bundle.get("username");
        roomName = getRoomName(response);

        waiting = (TextView) findViewById(R.id.waiting);

        roomLabel = (TextView) findViewById(R.id.room_label);
        roomLabel.setText("Room Name: " + roomName);

        people = (GridView) findViewById(R.id.people_list_view);
        players = getPlayers(response);
        updatePlayers();
        people.setClickable(false);

        results = (TextView) findViewById(R.id.info_message);

        settingsBtn = (FloatingActionButton) findViewById(R.id.settings_btn);
        // Set up onClick for startBtn to start a game
        settingsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                cancelResponseThread();
                Intent intent = new Intent(WaitingRoom.this, Settings.class);
                intent.putExtra("players", players);
                intent.putExtra("words", words);
                intent.putExtra("timer", timer);
                intent.putExtra("roomName", roomName);
                intent.putExtra("username", username);
                startActivityForResult(intent, 0);
            }
        });

        startBtn = (FloatingActionButton) findViewById(R.id.start_game_btn);
        // Set up onClick for startBtn to start a game
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = "Start:" + roomName + ":" + timer;
                if(words != null) {
                    message += ":" + TextUtils.join(",", words);
                }
                RequestHandler handler = new RequestHandler(Main.socket, message);
                handler.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        });

        leaveBtn = (FloatingActionButton) findViewById(R.id.leave_game_btn);
        // Set up onClick for leaveBtn to leave Room
        leaveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = "Leave_Session:" + roomName;
                RequestHandler handler = new RequestHandler(Main.socket, message);
                handler.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        });

        endRoomBtn = (FloatingActionButton) findViewById(R.id.end_game_btn);
        // Set up onClick for endRoomBtn to end the room
        endRoomBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = "End_Session:" + roomName;
                RequestHandler handler = new RequestHandler(Main.socket, message);
                handler.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        });

        // Set up a thread to listen to responses from the server
        LOGGER.log(Level.INFO, "Line 39");
        setupResponseThread();
    }

    /**
     * this function runs when an activity returns to this activity with a result.
     * Sets up responseHandler thread
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data.getBooleanExtra("leaving", false)) {
            try {
                closeSocket();
                endActivity(data.getStringExtra("reason"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else if (data.getBooleanExtra("settings", false)) {
            LOGGER.log(Level.INFO, "Line 161");
            timer = data.getStringExtra("timer");
            words = data.getStringArrayListExtra("custom_words");
            players = data.getStringArrayListExtra("players");
            updatePlayers();
        } else if (data.getBooleanExtra("game_started", false)) {
            try {
                JSONObject obj = new JSONObject(data.getStringExtra("obj"));
                players = data.getStringArrayListExtra("players");
                updatePlayers();
                startGame(obj);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            players = data.getStringArrayListExtra("players");
            updatePlayers();
            try {
                LOGGER.log(Level.INFO, "Line 181");
                callbackFailed(Color.rgb(255, 255, 255), data.getStringExtra("reason"), Main.socket);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Function that is called whenever a message is received from the server
     * @param resp
     */
    public void callBack(String resp) {
        LOGGER.log(Level.INFO, "Received callback response in WaitingRoom of " + resp);
        results.setText("");
        if(resp != null) {
            try {
                JSONObject obj = new JSONObject(resp);
                String type = obj.getString("type");
                if (obj.getString("status").equals("failed")) { // message from server was a failure message
                    LOGGER.log(Level.INFO, "Line 201");
                    callbackFailed(Color.rgb(255, 0, 0), obj.getString("reason"), Main.socket);
                } else if (type.equals("Ended_Session")) { // message from server to end room
                    closeSocket();
                    endActivity(obj.getString("reason"));
                } else if (type.equals("Joined")) { // message from server notifying you someone joined room
                    JSONArray new_players = obj.getJSONArray("usernames");
                    for(int i = 0; i < new_players.length(); i++) {
                        players.add(new_players.getString(i));
                    }
                    updatePlayers();
                    LOGGER.log(Level.INFO, "Line 209");
                    setupResponseThread();
                } else if (type.equals("Started")) { // message from server to start a game
                    startGame(obj);
                } else if (type.equals("Leave_Session")) { // message from server saying someone left room
                    JSONArray new_players = obj.getJSONArray("usernames");
                    if(new_players.length() == 1 && new_players.getString(0).equals(username)) {
                        closeSocket();
                        endActivity(obj.getString("reason"));
                    } else {
                        for (int i = 0; i < new_players.length(); i++) {
                            players.remove(new_players.getString(i));
                        }
                        updatePlayers();
                        LOGGER.log(Level.INFO, "Line 223");
                        setupResponseThread();
                    }
                } else if (type.equals("Ended_Game")) { // message notifying user that a game in this room just ended
                    results.setTextColor(Color.rgb(255, 255, 255));
                    results.setText(obj.getString("reason"));
                }
            } catch (JSONException e) {
                e.printStackTrace();
                LOGGER.log(Level.INFO, "Line 232");
                setupResponseThread();
            }
        } else {
            // In general set up another thread to listen once this thread is complete
            LOGGER.log(Level.INFO, "Line 237");
            setupResponseThread();
        }
    }

    /**
     * Helper function to start a game and go to the Game Activity
     * @param obj
     * @throws JSONException
     */
    private void startGame(JSONObject obj) throws JSONException {
        Intent intent = new Intent(WaitingRoom.this, Game.class);
        String role = "Samaritan";
        if (obj.getBoolean("spy")) {
            role = "Spy";
        }
        ArrayList<String> listOfWords = new ArrayList<>();
        JSONArray arr = obj.getJSONArray("list_of_words");
        for(int i = 0; i < arr.length(); i++) {
            listOfWords.add(arr.getString(i));
        }
        String specificWord = obj.getString("specific_word");
        if (specificWord == "null") {
            specificWord = "????";
        }
        cancelResponseThread();
        String timer = obj.getString("timer");
        intent.putExtra("role", role);
        intent.putExtra("listOfWords", listOfWords);
        intent.putExtra("specificWord", specificWord);
        intent.putExtra("roomName", roomName);
        intent.putExtra("username", username);
        intent.putExtra("players", players);
        intent.putExtra("timer", timer);
        startActivityForResult(intent,0);
    }

    /**
     * helper function that sets up a thread to listen to server messages
     */
    private void setupResponseThread() {
        cancelResponseThread();
        LOGGER.log(Level.INFO, "Started response thread in Waiting Room");
        responseHandler = new ResponseHandler(Main.socket, WaitingRoom.this);
        responseHandler.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * cancels an async task that is no longer needed
     */
    private void cancelResponseThread() {
        if(responseHandler != null) {
            AsyncTask.Status status = responseHandler.getStatus();
            if (status == AsyncTask.Status.RUNNING || status == AsyncTask.Status.PENDING) {
                responseHandler.cancel(true);
                LOGGER.log(Level.INFO, "Canceling current response handler thread in WaitingRoom");
                responseHandler = null;
            }
        }
    }

    /**
     * updates the people ListView with players ArrayList<String>>
     */
    private void updatePlayers() {
        int color = getResources().getColor(R.color.WordsColor);
        int wordColor = 0xffffffff;
        GridViewAdapter adapter = new GridViewAdapter(WaitingRoom.this, players, color, wordColor, 24);
        numOfPlayers = players.size();
        final Handler handler = new Handler();

        handler.postDelayed(new Runnable(){
            public void run(){
                //do something
                if(numOfPlayers >= 3) {
                    waiting.setText("Ready to Start Game");
                } else {
                    String periods = new String(new char[count]).replace("\0", " .");
                    String spaces = new String(new char[3 - count]).replace("\0", "  ");
                    String text = "Waiting for Players" + periods + spaces;
                    waiting.setText(text);
                    count++;
                    if (count == 4) {
                        count = 1;
                    }
                    handler.postDelayed(this, delay);
                }
            }
        }, delay);

        people.setAdapter(adapter);
    }

    /**
     * ends the WaitingRoom Activty and returns the proper info to the Main
     * @param reason
     * @throws JSONException
     */
    private void endActivity(String reason) throws JSONException {
        cancelResponseThread();
        Intent intent = new Intent();
        intent.putExtra("reason", reason);
        setResult(RESULT_OK, intent);
        finish();
    }

    /**
     * Closes connection to Server
     */
    private void closeSocket() {
        try {
            Main.socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Helper function for when message from server was a failed message
     * @param rgb
     * @param reason
     * @param socket
     * @throws JSONException
     */
    private void callbackFailed(int rgb, String reason, Socket socket) throws JSONException {
        results.setTextColor(rgb);
        results.setText(reason);
        LOGGER.log(Level.INFO, "Line 356");
        setupResponseThread();
    }

    /**
     * Given response string passed from MainActiviy, parse the string for the room name
     * @param response
     * @return
     */
    private String getRoomName(String response) {
        try {
            JSONObject obj = new JSONObject(response);
            return obj.getString("room_name");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Given response string from MainActivty, parse the string for the list of players in the game
     * @param response
     * @return
     */
    private ArrayList<String> getPlayers(String response) {
        HashSet<String> ret = new HashSet<>();
        ret.add(username);
        try {
            JSONObject obj = new JSONObject(response);
            JSONArray players = obj.getJSONArray("usernames");
            for(int i = 0; i < players.length(); i++) {
                ret.add(players.getString(i));
            }
            return new ArrayList<String>(ret);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
