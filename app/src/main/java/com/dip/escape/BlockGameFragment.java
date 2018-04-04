package com.dip.escape;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by irynashvydchenko on 2018-02-19.
 */

public class BlockGameFragment extends Fragment implements ColorPuzzleActivity.NewColorReadListener, BlockGameView.DoneGame{

    BlockGameView gameView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {

        gameView = new BlockGameView(getContext(), this);
        return gameView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

    }

    @Override
    public void onStop() {
        super.onStop();
        gameView.pause();
    }

    @Override
    public void onStart() {
        super.onStart();
        gameView.resume();
    }

    @Override
    public void onColorRead(ColorPuzzleActivity.DetectedColor color) {
        gameView.moveBlock(color);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((ColorPuzzleActivity) activity).registerNewColorReadListener(this);
    }

    @Override
    public void finishGame() {
        PhotonConnect.toggleServo(3);
        Intent myIntent = new Intent(getActivity(), GameFinishedActivity.class);
        getActivity().startActivity(myIntent);
    }
}
