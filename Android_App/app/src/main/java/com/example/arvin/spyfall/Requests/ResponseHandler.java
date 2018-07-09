package com.example.arvin.spyfall.Requests;

import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by arvin on 11/13/2017.
 */

public class ResponseHandler extends AsyncTask<String, String, String> {

    private Socket socket; // socket used to communicate with server
    private static String ip = "192.168.198.1"; // ip address of server
    private CallBack callBack;

    private static final Logger LOGGER = Logger.getLogger( ResponseHandler.class.getName() );

    /**
     *  Constructor for RequestHandler class
     * @param s - the socket to be sued to communicate with server
     * @param c - class that is used to call it's callback function
     */
    public ResponseHandler(Socket s, CallBack c) {
        socket = s;
        callBack = c;
        LOGGER.log(Level.INFO, "Started Response Handler");
    }

    /**
     * Waits for tcp response from server and returns that value to a callback function
     * @param strings
     * @return
     */
    @Override
    protected String doInBackground(String... strings) {
        LOGGER.log(Level.INFO, "Inside doInBackground");
        final StringBuilder result = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String response = br.readLine();
            LOGGER.log(Level.INFO, "Message received from server is: " + response);
            if(response == null) {
                return null;
            } else {
                return response.trim();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        LOGGER.log(Level.INFO, "In onPostExecute of responseHandler and the string is: " + s);
        callBack.callBack(s);
    }
}
