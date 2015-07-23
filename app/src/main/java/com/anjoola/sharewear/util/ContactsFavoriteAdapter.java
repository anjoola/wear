package com.anjoola.sharewear.util;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.anjoola.sharewear.R;

import java.util.ArrayList;

/**
 * Adapter for displaying favorited contacts.
 */
public class ContactsFavoriteAdapter extends BaseAdapter {
    // Activity that contains this adapter.
    private Activity activity;

    // List of all contacts.
    private ArrayList<ContactDetails> mContactsList;

    public ContactsFavoriteAdapter(Activity activity,
                                   ArrayList<ContactDetails> contactsList) {
        this.activity = activity;
        this.mContactsList = contactsList;
    }

    @Override
    public int getCount() {
        return mContactsList.size();
    }

    @Override
    public Object getItem(int i) {
        return mContactsList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        // References children views to avoid unnecessary calls.
        final ViewHolder holder;
        final ContactDetails user = (ContactDetails) getItem(position);

        if (view == null) {
            LayoutInflater layoutInflater = (LayoutInflater)
                    activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = layoutInflater.inflate(R.layout.contacts_list_view_favorite,
                    parent, false);
            holder = new ViewHolder();
            holder.photo = (ImageView) view.findViewById(R.id.photo);
            holder.name = (TextView) view.findViewById(R.id.name);
            view.setTag(holder);
        }
        // Get view holder back.
        else {
            holder = (ViewHolder) view.getTag();
        }

        // Set photo and name.
        holder.photo.setImageURI(Uri.parse(user.photoUri));
        holder.name.setText(user.name);
        return view;
    }

    /**
     * Used for keeping references to children views to avoid unnecessary calls.
     */
    static class ViewHolder {
        ImageView photo;
        TextView name;
    }
}
