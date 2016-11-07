package com.dji.sdk.sample.mrl;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.dji.sdk.sample.R;
import com.dji.sdk.sample.mrl.network.model.Episode;

import java.util.ArrayList;
import java.util.Collection;

public class EpisodeAdapter extends BaseAdapter {

    private ArrayList<Episode> mEpisodes = new ArrayList<>();

    public EpisodeAdapter() {}

    @Override
    public int getCount() {
        return mEpisodes.size();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final Context context = parent.getContext();

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.listview_item_episode, parent, false);
        }

        TextView mEpisodeId = (TextView) convertView.findViewById(R.id.episode_id);
        TextView mEpisodeCreatedAt = (TextView) convertView.findViewById(R.id.episode_created_at);
        TextView mEpisodeName = (TextView) convertView.findViewById(R.id.episode_name);

        Episode episode = mEpisodes.get(position);

        mEpisodeId.setText(String.format("#%d", episode.id));
        mEpisodeCreatedAt.setText(episode.created_at);
        mEpisodeName.setText(episode.name);

        return convertView;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public Object getItem(int position) {
        return mEpisodes.get(position);
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
    }

    public void addAll(Collection<Episode> episodes) {
        mEpisodes.addAll(episodes);
    }

    public void clear() {
        mEpisodes.clear();
    }
}
