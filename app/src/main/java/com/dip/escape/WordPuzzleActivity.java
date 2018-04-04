package com.dip.escape;

import android.app.FragmentTransaction;
import android.os.Bundle;
import android.app.Fragment;
import android.support.v7.app.AppCompatActivity;

import android.widget.FrameLayout;

public class WordPuzzleActivity extends AppCompatActivity {
    private static final int FRAGMENT_ONE_ID = 001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_word_puzzle);
        FrameLayout frame = new FrameLayout(this);
        frame.setId(FRAGMENT_ONE_ID);
        setContentView(frame, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        PhotonConnect.authenticate(this);
        if (savedInstanceState == null) {
            Fragment newFragment = new WordPuzzleFragment();
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.add(FRAGMENT_ONE_ID, newFragment).commit();
        }
    }

}
