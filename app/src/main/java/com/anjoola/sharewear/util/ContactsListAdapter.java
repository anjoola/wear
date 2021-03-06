package com.anjoola.sharewear.util;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import com.anjoola.sharewear.R;

import java.util.ArrayList;

/**
 * Adapter used for filtering contacts for searches.
 * Courtesy of https://coderwall.com/p/zpwrsg/add-search-function-to-list-view-in-android.
 *
 * Smooth scrolling courtesy of
 * http://www.javacodegeeks.com/2012/06/android-smooth-list-scrolling.html.
 */
public class ContactsListAdapter extends BaseAdapter implements Filterable {
    // Used for filtering out contacts.
    private ContactsFilter mFilter;

    // List of contacts (all, and filtered).
    private ArrayList<ContactDetails> mContactsList;
    private ArrayList<ContactDetails> filteredList;

    public boolean isScrolling;

    // Used for layouts.
    LayoutInflater mLayoutInflater;

    public ContactsListAdapter(Activity activity,
                               ArrayList<ContactDetails> contactsList) {
        mLayoutInflater = (LayoutInflater)
                activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.mContactsList = contactsList;
        this.filteredList = contactsList;
        getFilter();

        isScrolling = false;
    }

    @Override
    public int getCount() {
        return filteredList.size();
    }

    @Override
    public Object getItem(int i) {
        return filteredList.get(i);
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
            view = mLayoutInflater.inflate(R.layout.contacts_list_view_all,
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

        // Set photo and name. Only show real photo if not scrolling.
        holder.name.setText(user.name);
        if (!isScrolling)
            holder.photo.setImageURI(Uri.parse(user.photoUri));
        else
            holder.photo.setImageResource(R.mipmap.ic_loading_picture);

        return view;
    }

    @Override
    public Filter getFilter() {
        if (mFilter == null) {
            mFilter = new ContactsFilter();
        }
        return mFilter;
    }

    /**
     * Used for keeping references to children views to avoid unnecessary calls.
     */
    static class ViewHolder {
        ImageView photo;
        TextView name;
    }

    /**
     * Used for filtering contacts such that only the ones satisfying the
     * search request are displayed.
     */
    private class ContactsFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults filterResults = new FilterResults();
            if (constraint != null && constraint.length() > 0) {
                ArrayList<ContactDetails> tempList = new ArrayList<ContactDetails>();

                // Find contact with the searched name.
                for (ContactDetails user : mContactsList) {
                    if (user.name.toLowerCase().contains(
                            constraint.toString().toLowerCase())) {
                        tempList.add(user);
                    }
                }
                filterResults.count = tempList.size();
                filterResults.values = tempList;
            }

            // No filtering.
            else {
                filterResults.count = mContactsList.size();
                filterResults.values = mContactsList;
            }

            return filterResults;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint,
                                      FilterResults results) {
            filteredList = (ArrayList<ContactDetails>) results.values;
            notifyDataSetChanged();
        }
    }
}
