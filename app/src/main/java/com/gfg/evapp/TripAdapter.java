package com.gfg.evapp;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

public class TripAdapter extends RecyclerView.Adapter<TripAdapter.ViewHolder> {

    Context context;
    Cursor cursor;

    public TripAdapter(Context context, Cursor cursor) {
        this.context = context;
        this.cursor = cursor;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(android.R.layout.simple_list_item_2, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        if (!cursor.moveToPosition(position)) return;

        String source = cursor.getString(
                cursor.getColumnIndexOrThrow("source"));

        String destination = cursor.getString(
                cursor.getColumnIndexOrThrow("destination"));

        String result = cursor.getString(
                cursor.getColumnIndexOrThrow("result"));

        String eta = cursor.getString(
                cursor.getColumnIndexOrThrow("eta"));

        holder.title.setText(source + " → " + destination);
        holder.subtitle.setText(result + " | ETA: " + eta);
    }

    @Override
    public int getItemCount() {
        return cursor == null ? 0 : cursor.getCount();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView title, subtitle;

        public ViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(android.R.id.text1);
            subtitle = itemView.findViewById(android.R.id.text2);
        }
    }
}
