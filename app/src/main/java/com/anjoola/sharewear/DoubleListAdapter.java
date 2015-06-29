package com.anjoola.sharewear;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;

/**
 * Allows for two different adapters at once, each with a section header.
 * Courtesy of http://jsharkey.org/blog/2008/08/18/separating-lists-with-headers-in-android-09/.
 */
public class DoubleListAdapter extends BaseAdapter {
    // Section headers and associated adapters.
    private String section1, section2;
    private Adapter adapter1, adapter2;
    public final ArrayAdapter<String> headers;

    public final static int TYPE_SECTION_HEADER = 0;

    public DoubleListAdapter(Context context, String section1, Adapter adapter1,
                             String section2, Adapter adapter2) {
        // Initialize header layout.
        headers = new ArrayAdapter<String>(context,
                R.layout.contacts_list_view_header,
                R.id.section_header);

        this.section1 = section1;
        this.section2 = section2;
        this.adapter1 = adapter1;
        this.adapter2 = adapter2;
        headers.add(section1);
        headers.add(section2);
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public int getCount() {
        // Add 2 for the headers.
        return adapter1.getCount() + adapter2.getCount() + 2;
    }

    @Override
    public Object getItem(int position) {
        // First section.
        if (position == 0)
            return section1;

        position -= 1;
        if (position < adapter1.getCount())
            return adapter1.getItem(position);

        // Second section.
        position -= adapter1.getCount();
        if (position == 0)
            return section2;

        position -= 1;
        if (position < adapter2.getCount())
            return adapter2.getItem(position);

        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        int type = 1;

        // First section.
        if (position == 0)
            return TYPE_SECTION_HEADER;

        position -= 1;
        if (position < adapter1.getCount())
            return type + adapter1.getItemViewType(position);

        // Second section.
        position -= adapter1.getCount();
        type += adapter1.getViewTypeCount();
        if (position == 0)
            return TYPE_SECTION_HEADER;

        position -= 1;
        if (position < adapter2.getCount())
            return type + adapter2.getItemViewType(position);

        return -1;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // First section.
        if (position == 0)
            return headers.getView(0, convertView, parent);


        position -= 1;
        if (position < adapter1.getCount())
            return adapter1.getView(position, convertView, parent);


        // Second section.
        position -= adapter1.getCount();
        if (position == 0)
            return headers.getView(1, convertView, parent);


        position -= 1;
        if (position < adapter2.getCount())
            return adapter2.getView(position, convertView, parent);

        return convertView;
    }

    @Override
    public int getViewTypeCount() {
        return 1 + adapter1.getViewTypeCount() + adapter2.getViewTypeCount();
    }

    @Override
    public boolean isEnabled(int position) {
        return getItemViewType(position) != TYPE_SECTION_HEADER;
    }
}
