package com.example.arvin.spyfall.Requests;

import android.os.AsyncTask;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by arvin on 11/13/2017.
 */

public class RequestHandler extends AsyncTask<String, String, String> {

    private Socket socket; // socket used to communicate with server
    private String message; // message to be sent to server
    private static String ip = "192.168.198.1"; // ip address of server
    private static final Logger LOGGER = Logger.getLogger( RequestHandler.class.getName() );

    /**
     *  Constructor for RequestHandler class
     * @param s - the socket to be sued to communicate with server
     * @param m - message to be sent to server
     */
    public RequestHandler(Socket s, String m) {
        socket = s;
        if(m.length() >= 100) {
            m = m.substring(0, 100);
            StringBuilder builder = new StringBuilder(m);
            builder.setCharAt(99, '\n');
            message = builder.toString();
        } else {
            m += '\n';
            message = String.format("%-100s", m);
        }
        LOGGER.log(Level.INFO, "Messaging being sent is: " + message);
    }


    /**
     * Makes a tcp request to the server
     * @param strings
     * @return
     */
    @Override
    protected String doInBackground(String... strings) {
        LOGGER.log(Level.INFO, "trying to send a message to server");
        StringBuilder result = new StringBuilder();
        try {
            PrintWriter printWriter = new PrintWriter(socket.getOutputStream());
            printWriter.write(message);
            printWriter.flush();
            LOGGER.log(Level.INFO, "Message sent to server");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
