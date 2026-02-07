package com.gfg.evapp;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;


public class TripAdapter extends RecyclerView.Adapter<TripAdapter.TripViewHolder> {

    Context context;
    Cursor cursor;

    public TripAdapter(Context context, Cursor cursor) {
        this.context = context;
        this.cursor = cursor;
    }

    @Override
    public TripViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context)
                .inflate(R.layout.item_trip, parent, false);
        return new TripViewHolder(v);
    }

    @Override
    public void onBindViewHolder(TripViewHolder holder, int position) {
        if (!cursor.moveToPosition(position)) return;

        holder.tvSource.setText("From: " + cursor.getString(4));
        holder.tvDestination.setText("To: " + cursor.getString(5));
        holder.tvResult.setText(cursor.getString(6));
        holder.tvCharger.setText("Charger: " + cursor.getString(7));
        holder.tvTime.setText(cursor.getString(8));
    }

    @Override
    public int getItemCount() {
        return cursor.getCount();
    }

    static class TripViewHolder extends RecyclerView.ViewHolder {

        TextView tvSource, tvDestination, tvResult, tvCharger, tvTime;

        public TripViewHolder(View itemView) {
            super(itemView);
            tvSource = itemView.findViewById(R.id.tvSource);
            tvDestination = itemView.findViewById(R.id.tvDestination);
            tvResult = itemView.findViewById(R.id.tvResult);
            tvCharger = itemView.findViewById(R.id.tvCharger);
            tvTime = itemView.findViewById(R.id.tvTime);
        }
    }
}
