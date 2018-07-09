package com.example.arvin.spyfall.Activities.Main;

import android.app.Fragment;
import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.design.widget.TextInputEditText;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.example.arvin.spyfall.R;
import com.example.arvin.spyfall.Requests.RequestHandler;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link CreateRoom.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link CreateRoom#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CreateRoom extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private TextView results;
    private TextInputEditText usernameEdit;
    private TabLayout tabs;
    private Button createBtn;

    private Main act;

    private OnFragmentInteractionListener mListener;

    public CreateRoom() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment CreateRoom.
     */
    // TODO: Rename and change types and number of parameters
    public static CreateRoom newInstance(String param1, String param2) {
        CreateRoom fragment = new CreateRoom();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_main_create_room, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        act = (Main) getActivity();

        createBtn = (Button) view.findViewById(R.id.create_btn);

        // set up on Click listener for create Button
        createBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean success = act.createConnection();
                if(success) {
                    act.results.setTextColor(Color.rgb(255, 255, 255));
                    act.username = act.usernameEdit.getText().toString().trim();
                    boolean error = false; // error = true if username is empty
                    if (act.username.length() == 0) { // username needs to be non-empty
                        act.usernameEdit.setError("Username is required!");
                        error = true;
                    }
                    if (!error) {
                        String message = "Create:" + act.username;
                        RequestHandler handler = new RequestHandler(Main.socket, message);
                        handler.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    }
                } else {
                    act.results.setText(act.resultString);
                    act.results.setTextColor(Color.rgb(255, 0, 0));
                    act.resultString = "";
                }
            }
        });

    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
