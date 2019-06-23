package de.rtcustomz.walloflight.dialogs;

import android.os.Bundle;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import de.rtcustomz.walloflight.R;

public class BrushChooser extends DialogFragment {
    private static final String ARG_ERASE = "erase";
    private static final String ARG_TITLE = "title";

    private boolean erase;
    private String title;

    private OnBrushClickListener mListener;

    private int smallBrush;
    private int mediumBrush;
    private int largeBrush;


    public BrushChooser() {

    }

    public static BrushChooser newInstance(String title, boolean erase) {
        BrushChooser brushChooser = new BrushChooser();

        Bundle args = new Bundle();
        args.putBoolean(BrushChooser.ARG_ERASE, erase);
        args.putString(BrushChooser.ARG_TITLE, title);
        brushChooser.setArguments(args);

        return brushChooser;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Fragment context = getTargetFragment();
        if (context instanceof OnBrushClickListener) {
            mListener = (OnBrushClickListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnBrushClickListener");
        }

        if (getArguments() != null) {
            erase = getArguments().getBoolean(BrushChooser.ARG_ERASE);
            title = getArguments().getString(BrushChooser.ARG_TITLE);
        }

        smallBrush = getActivity().getResources().getInteger(R.integer.small_size);
        mediumBrush = getActivity().getResources().getInteger(R.integer.medium_size);
        largeBrush = getActivity().getResources().getInteger(R.integer.large_size);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().setTitle(title);
        View v = inflater.inflate(R.layout.brush_chooser, container, false);
        ImageButton smallBtn = (ImageButton)v.findViewById(R.id.small_brush);
        smallBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onBrushClick(smallBrush, erase);
                getDialog().dismiss();
            }
        });
        ImageButton mediumBtn = (ImageButton)v.findViewById(R.id.medium_brush);
        mediumBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onBrushClick(mediumBrush, erase);
                getDialog().dismiss();
            }
        });
        ImageButton largeBtn = (ImageButton)v.findViewById(R.id.large_brush);
        largeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onBrushClick(largeBrush, erase);
                getDialog().dismiss();
            }
        });
        return v;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mListener = null;
    }

    public interface OnBrushClickListener {
        void onBrushClick(int brushSize, boolean erase);
    }
}
