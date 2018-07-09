package com.example.arvin.spyfall.Activities.Game;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;

import com.example.arvin.spyfall.Activities.Main.Main;
import com.example.arvin.spyfall.Activities.Util.GridViewAdapter;
import com.example.arvin.spyfall.Activities.WatingRoom.WaitingRoom;
import com.example.arvin.spyfall.R;
import com.example.arvin.spyfall.Requests.CallBack;
import com.example.arvin.spyfall.Requests.RequestHandler;
import com.example.arvin.spyfall.Requests.ResponseHandler;
import com.github.clans.fab.FloatingActionButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Game extends AppCompatActivity implements CallBack {

    private TextView timer;
    private TextView role;
    private TextView word;
    private GridView listOfWords;
    private FloatingActionButton leaveBtn;
    private FloatingActionButton endBtn;

    private String roomName;
    private String username;
    private ArrayList<String> players;
    private long timerLength = 1; // 1 minute
    private CountDownTimer countDown;
    private boolean gameEndedNaturally = true;

    private ResponseHandler responseHandler;

    private static final Logger LOGGER = Logger.getLogger( WaitingRoom.class.getName() );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();

        roomName = (String) bundle.get("roomName");
        username = (String) bundle.get("username");
        String timerStr = (String) bundle.get("timer");
        timerLength = Long.parseLong(timerStr);
        players = (ArrayList<String>) bundle.get("players");
        timer = (TextView) findViewById(R.id.timer);
        role = (TextView) findViewById(R.id.role);
        String playerRole = (String) bundle.get("role");
        role.setText(" Role: " + playerRole);

        word = (TextView) findViewById(R.id.specific_word);
        String specfic_word = (String) bundle.get("specificWord");
        word.setText(" Word: " + specfic_word);

        listOfWords = (GridView) findViewById(R.id.words_list_view);
        listOfWords.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                System.out.println("In gridview click");
                Drawable background = view.getBackground();
                if(background instanceof ColorDrawable) {
                    int color = ((ColorDrawable) background).getColor();
                    if(color == getResources().getColor(R.color.WordsColor)) { // was teal make red
                        view.setBackgroundColor(getResources().getColor(R.color.MyRed));
                    } else if(color == getResources().getColor(R.color.MyRed)) { // was red make green
                        view.setBackgroundColor(getResources().getColor(R.color.MyGreen));
                    } else { // was green make teal
                        view.setBackgroundColor(getResources().getColor(R.color.WordsColor));
                    }
                }
            }
        });
        ArrayList<String> words = (ArrayList<String>) bundle.get("listOfWords");
        int color = getResources().getColor(R.color.WordsColor);
        int wordColor = 0xffffffff;
        GridViewAdapter adapter = new GridViewAdapter(Game.this, words, color, wordColor, 16);
        listOfWords.setAdapter(adapter);

        leaveBtn = (FloatingActionButton) findViewById(R.id.leave_game_btn);
        leaveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = "Leave_Session:Room Name Not Given";
                RequestHandler requestHandler = new RequestHandler(Main.socket, message);
                requestHandler.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                leaveRoom("You left the room");
            }
        });

        endBtn = (FloatingActionButton) findViewById(R.id.end_game_btn);
        // set up onClick for endGameBtn
        endBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gameEndedNaturally = false;
                String message = "End_Game:" + roomName;
                RequestHandler requestHandler = new RequestHandler(Main.socket, message);
                requestHandler.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        });

        // CountDown object that will execute onTick every second
        countDown = new CountDownTimer(60*1000*timerLength, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60;
                long minutes = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished);
                LOGGER.log(Level.INFO, "seconds = " + seconds + " and minutes = " + minutes);
                timer.setText("Time Remaining: " + String.format("%02d", minutes) + ":" + String.format("%02d", seconds));
            }

            @Override
            public void onFinish() {
                timer.setText("Time Remaining: 00:00");
            }
        };
        countDown.start();
        setupResponseThread();
    }

    /**
     * leave the room completely
     * @param value
     */
    private void leaveRoom(String value) {
        try {
            Main.socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        cancelResponseThread();
        countDown.cancel();
        Intent intent = new Intent();
        intent.putExtra("reason", value);
        intent.putExtra("leaving", true);
        setResult(RESULT_OK, intent);
        finish();
    }

    /**
     * Callback function called when a message is received from server
     * @param resp
     */
    @Override
    public void callBack(String resp) {
        LOGGER.log(Level.INFO, "Received callback response in Game of " + resp);
        if(resp != null) {
            try {
                JSONObject obj = new JSONObject(resp);
                String type = obj.getString("type");
                if (obj.getString("status").equals("failed")) { // if message from server was a failed message
                    setupResponseThread();
                }
                else if(type.equals("Ended_Game")) { // if message from server was to end game
                    endGameActivity(obj);
                } else if(type.equals("Leave_Session")) {
                    String userLeft = obj.getJSONArray("usernames").getString(0);
                    if(userLeft.equals(username)) {
                        leaveRoom(obj.getString("reason"));
                    } else {
                        players.remove(userLeft);
                        if(obj.getBoolean("end") == true) {
                            endGameActivity(obj);
                        }
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
                setupResponseThread();
            }
        } else {
            setupResponseThread();
        }
    }

    /**
     * returns to WaitingRoom Activity and passes results to that activity
     * @param obj
     * @throws JSONException
     */
    private void endGameActivity(JSONObject obj) throws JSONException {
        cancelResponseThread();
        countDown.cancel();
        Intent intent = new Intent();
        intent.putExtra("reason", obj.getString("reason"));
        intent.putExtra("players", players);
        setResult(RESULT_OK, intent);
        finish();
    }

    /**
     * cancels Response thread because it is not longer needed
     */
    private void cancelResponseThread() {
        if(responseHandler != null) {
            AsyncTask.Status status = responseHandler.getStatus();
            if (status == AsyncTask.Status.RUNNING || status == AsyncTask.Status.PENDING) {
                responseHandler.cancel(true);
                LOGGER.log(Level.INFO, "Canceling current response handler thread in Game");
                responseHandler = null;
            }
        }
    }

    /**
     * sets up thread to listen to messages from server
     */
    private void setupResponseThread() {
        cancelResponseThread();
        LOGGER.log(Level.INFO, "Started response thread in Game");
        responseHandler = new ResponseHandler(Main.socket, Game.this);
        responseHandler.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
}
