package com.dip.escape;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * A placeholder fragment containing a simple view.
 */
public class WordPuzzleFragment extends Fragment {

    private TextView mPrompt;
    private Button mSubmitButton;
    private EditText mAnswer;
    private Boolean firstLevel;
    private ImageView mPuzzleImage;

    private static String FIRST_ANSWER = "dip";
    private static String SECOND_ANSWER = "eee";

    public WordPuzzleFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_word_puzzle, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mSubmitButton = view.findViewById(R.id.fragment_first_puzzle_submit_btn);
        mAnswer = view.findViewById(R.id.fragment_first_puzzle_answer_text);
        firstLevel = true;
        mPuzzleImage = view.findViewById(R.id.puzzle_image);
        mPrompt = view.findViewById(R.id.fragment_first_puzzle_prompt);
        mSubmitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String currentText = mAnswer.getText().toString().toLowerCase();
                verifyResult(currentText);
            }
        });
    }

    private void verifyResult(String text) {
        if (firstLevel) {
            if (text.equals(FIRST_ANSWER)) {
                PhotonConnect.toggleServo(1);
                changePuzzle();
            } else {
                Toast.makeText(getActivity(), "Wrong Answer",
                        Toast.LENGTH_LONG).show();
            }
        } else {
            if (text.equals(SECOND_ANSWER)) {
                PhotonConnect.toggleServo(2);
                Intent myIntent = new Intent(getActivity(), ColorPuzzleActivity.class);
                getActivity().startActivity(myIntent);
            } else {
                Toast.makeText(getActivity(), "Wrong Answer",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void changePuzzle() {
        String newString = getString(R.string.second_puzzle_text);
        mPuzzleImage.setVisibility(View.VISIBLE);
        firstLevel = false;
        mPrompt.setText(newString);
        View view = getActivity().getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(getActivity().INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
        mAnswer.setText("");
        mAnswer.setHint("Answer");
    }
}
