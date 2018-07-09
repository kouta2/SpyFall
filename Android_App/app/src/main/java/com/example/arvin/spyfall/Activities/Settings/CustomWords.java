package com.example.arvin.spyfall.Activities.Settings;

import android.app.Fragment;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.text.TextUtils;

import com.example.arvin.spyfall.R;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link CustomWords.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link CustomWords#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CustomWords extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    public CustomWords() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment CustomWords.
     */
    // TODO: Rename and change types and number of parameters
    public static CustomWords newInstance(String param1, String param2) {
        CustomWords fragment = new CustomWords();
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
        return inflater.inflate(R.layout.fragment_custom_words, container, false);
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
     * determines if the text inside customWords is valid.
     * returns true if there are 20 comma seperated words
     * @return
     */
    public boolean isWordValid() {
        String listOfWords = customWords.getText().toString().trim();
        String[] split = listOfWords.split(",");
        return split.length == 20;
    }

    /**
     * sets error message for customWords EditText
     */
    public void setWordError() {
        customWords.setError("Exactly 20 words is required and every word should be comma seperated!");
    }

    /**
     * returns an ArrayList<String> of the custom words that the user wants to use in the game
     * If the EditText is invalid, returns null
     * @return
     */
    public ArrayList<String> getWords() {
        if(isWordValid()) {
            String listOfWords = customWords.getText().toString().trim();
            String[] split = listOfWords.split(",");
            ArrayList<String> ret = new ArrayList<>();
            for(int i = 0; i < split.length; i++) {
                ret.add(split[i].trim());
            }
            return ret;
        }
        return null;
    }

    private TextInputEditText customWords;

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        customWords = (TextInputEditText) view.findViewById(R.id.custom_words);
        Bundle bundle = this.getArguments();
        ArrayList<String> words = bundle.getStringArrayList("custom_words");
        if(words != null) {
            String text = TextUtils.join(", ", words);
            customWords.setText(text);
        }


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
