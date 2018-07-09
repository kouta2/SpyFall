package com.example.arvin.spyfall.Activities.Main;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.design.widget.TabLayout;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.example.arvin.spyfall.Activities.WatingRoom.WaitingRoom;
import com.example.arvin.spyfall.R;
import com.example.arvin.spyfall.Requests.CallBack;
import com.example.arvin.spyfall.Requests.ResponseHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

// TODO: restrict length of room name and username

public class Main extends AppCompatActivity implements CallBack, CreateRoom.OnFragmentInteractionListener, JoinRoom.OnFragmentInteractionListener {

    public TextView results;
    public TextInputEditText usernameEdit;
    private TabLayout tabs;

    public static Socket socket; // socket object used to communicate with the server
    private static String ip = "192.168.198.1"; // ip address of server

    public String resultString = ""; // String that populates results
    public String username;
    private Boolean isStartup = true;

    private static final Logger LOGGER = Logger.getLogger( Main.class.getName() );
    private ResponseHandler responseHandler;

    /**
     * Callback function that is called whenever a message is received from the server
     * @param response
     */
    public void callBack(String response) {
        LOGGER.log(Level.INFO, "Inside callback function in Main");
        results.setText("");
        if(response != null) {
            try {
                JSONObject obj = new JSONObject(response);
                if (obj.getString("status").equals("failed")) {
                    results.setTextColor(Color.rgb(255, 0, 0));
                    results.setText(obj.getString("reason"));
                } else {
                    cancelResponseThread();
                    Intent intent = new Intent(Main.this, WaitingRoom.class);
                    intent.putExtra("response", response);
                    intent.putExtra("username", username);
                    startActivityForResult(intent,0);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            results.setTextColor(Color.rgb(255, 0, 0));
            results.setText("There was error connecting to the server");
        }
    }

    /**
     * function that runs when an activity returns to this activity with a result
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        cancelResponseThread();
        super.onActivityResult(requestCode, resultCode, data);
        results.setTextColor(Color.rgb(255, 255, 255));
        results.setText(data.getStringExtra("reason"));
    }

    private void cancelResponseThread() {
        if(responseHandler != null) {
            AsyncTask.Status status = responseHandler.getStatus();
            if (status == AsyncTask.Status.RUNNING || status == AsyncTask.Status.PENDING) {
                responseHandler.cancel(true);
                LOGGER.log(Level.INFO, "Canceling current response handler thread in Main");
                responseHandler = null;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LOGGER.log(Level.INFO, "Starting Main Activity");

        results = (TextView) findViewById(R.id.results);
        tabs = (TabLayout) findViewById(R.id.tab_layout);
        usernameEdit = (TextInputEditText) findViewById(R.id.username);

        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                results.setText("");
                int position = tab.getPosition();
                if(position == 0) {
                    Fragment frag = new JoinRoom();
                    FragmentManager man = getFragmentManager();
                    FragmentTransaction ft = man.beginTransaction();
                    ft.replace(R.id.main_fragment, frag).commit();
                } else if(position == 1) {
                    Fragment frag = new CreateRoom();
                    FragmentManager man = getFragmentManager();
                    FragmentTransaction ft = man.beginTransaction();
                    ft.replace(R.id.main_fragment, frag).commit();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        usernameEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                usernameEdit.setError(null);
            }
        });
    }

    /**
     * Establishes a TCP connection with the server
     * Returns true or false on whether it was successful
     */
    public boolean createConnection() {
        Thread createConnection = new Thread(new Connection());
        createConnection.start();
        try {
            createConnection.join();
        } catch(InterruptedException e ){
            e.printStackTrace();
        }
        return !isStartup;
    }

    @Override
    public void onFragmentInteraction(Uri uri) {
        return;
    }

    /**
     * Connection class that makes connection to server. Can be Ran on its own thread.
     */
    private class Connection implements Runnable {
        @Override
        public void run() {
            LOGGER.log(Level.INFO, "Trying to establish a connection");
            try {
                socket = new Socket();
                socket.connect(new InetSocketAddress(ip, 10001), 1000);
                cancelResponseThread();
                LOGGER.log(Level.INFO, "Started response thread in Main");
                responseHandler = new ResponseHandler(socket, Main.this);
                responseHandler.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                isStartup = false;
                LOGGER.log(Level.INFO, "Established a connection");
            } catch (IOException e) {
                e.printStackTrace();
                resultString = "Error Connecting to Server";
            }
        }
    }
}
