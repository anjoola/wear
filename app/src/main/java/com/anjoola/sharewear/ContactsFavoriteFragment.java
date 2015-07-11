package com.anjoola.sharewear;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class ContactsFavoriteFragment extends Fragment {
    View mNoFavoritesView;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.contacts_favorite_fragment, container, false);

        mNoFavoritesView = v.findViewById(R.id.no_favorites_view);
        return v;
    }
}
