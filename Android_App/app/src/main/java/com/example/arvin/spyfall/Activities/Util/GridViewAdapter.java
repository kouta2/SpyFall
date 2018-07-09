package com.example.arvin.spyfall.Activities.Util;

import android.content.Context;
import android.database.DataSetObserver;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridLayout;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by arvin on 12/6/2017.
 */

/**
 * Adapter for Gridview used in WaitingRoom.java and Game.java
 */
public class GridViewAdapter extends BaseAdapter {
    private Context ctx;
    private ArrayList<String> listOfData;
    private int backgroundColor;
    private int wordColor;
    private float size;

    public GridViewAdapter(Context context, ArrayList<String> data, int color, int wColor, float s) {
        ctx = context;
        listOfData = new ArrayList<>(data);
        backgroundColor = color;
        wordColor = wColor;
        size = s;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        TextView ret;
        if(view != null) {
            ret = (TextView) view;
        } else {
            ret = new TextView(ctx);
        }
        ret.setPadding(0, 10, 0, 10);
        ret.setTextColor(wordColor);
        ret.setText(listOfData.get(i));
        ret.setTextSize(size);
        ret.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        ret.setBackgroundColor(backgroundColor);
        ret.setLayoutParams(new ViewGroup.LayoutParams(GridLayout.LayoutParams.MATCH_PARENT, GridLayout.LayoutParams.MATCH_PARENT));
        return ret;
    }

    @Nullable
    @Override
    public CharSequence[] getAutofillOptions() {
        return new CharSequence[0];
    }

    @Override
    public int getCount() {
        return listOfData.size();
    }

    @Override
    public Object getItem(int i) {
        return listOfData.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }
}
