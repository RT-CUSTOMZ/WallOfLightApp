package de.rtcustomz.walloflight.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;

public class AlertDialogFragment extends DialogFragment {
    private static final String ARG_INITIATOR = "initiator";
    private static final String ARG_TITLE = "title";
    private static final String ARG_MESSAGE = "message";
    private static final String ARG_POSITIVE = "positive";
    private static final String ARG_NEGATIVE = "negative";

    private int initiator;
    private String title;
    private String message;
    private String positive;
    private String negative;

    private OnClickListener mListener;

    public AlertDialogFragment() {
        // Required empty public constructor
    }

    public static AlertDialogFragment newInstance(int initiator, String title, String message, String positive, String negative) {
        AlertDialogFragment alertDialogFragment = new AlertDialogFragment();

        Bundle args = new Bundle();
        args.putInt(ARG_INITIATOR, initiator);
        args.putString(ARG_TITLE, title);
        args.putString(ARG_MESSAGE, message);
        args.putString(ARG_POSITIVE, positive);
        args.putString(ARG_NEGATIVE, negative);
        alertDialogFragment.setArguments(args);

        return alertDialogFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Fragment context = getTargetFragment();
        if (context instanceof OnClickListener) {
            mListener = (OnClickListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnClickListener");
        }

        if (getArguments() != null) {
            initiator = getArguments().getInt(ARG_INITIATOR);
            title = getArguments().getString(ARG_TITLE);
            message = getArguments().getString(ARG_MESSAGE);
            positive = getArguments().getString(ARG_POSITIVE);
            negative = getArguments().getString(ARG_NEGATIVE);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton(positive, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                mListener.onDialogPositiveClick(initiator, AlertDialogFragment.this);
            }
        });
        builder.setNegativeButton(negative, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                mListener.onDialogNegativeClick(initiator, AlertDialogFragment.this);
            }
        });

        return builder.create();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mListener = null;
    }

    public interface OnClickListener {
        void onDialogPositiveClick(int initiator, DialogFragment dialog);
        void onDialogNegativeClick(int initiator, DialogFragment dialog);
    }
}
