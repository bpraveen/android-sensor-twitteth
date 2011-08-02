package com.example;

import java.util.ArrayList;

import winterwell.jtwitter.Twitter.Status;
import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class CustomArrayAdapter extends ArrayAdapter<Status> {
	private ArrayList<Status> results;
	
	public CustomArrayAdapter(Context context, int textViewResourceId, ArrayList<Status> results) {
        super(context, textViewResourceId, results);
        this.results = results;
}
	
	@Override
    public View getView(int position, View convertView, ViewGroup parent) {
                View v = convertView;                      
                if (v != null) {
                	return v;
                	}                    
                LayoutInflater vi = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.row, null);                
                Status s = results.get(position);
                if (s != null) {
           
                    TextView textUser = (TextView) v.findViewById(R.id.textUser);
                    TextView textCreatedAt = (TextView) v.findViewById(R.id.textCreatedAt);
                    TextView textText = (TextView) v.findViewById(R.id.textText);
                    
                    textUser.setText(s.getUser().getScreenName());                    
                    textText.setText(s.text);                                              
                    textCreatedAt.setText(DateUtils.getRelativeTimeSpanString(s.getCreatedAt().getTime()));                      
                }
            return v;
    }		
	
}