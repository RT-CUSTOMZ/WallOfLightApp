package de.rtcustomz.walloflight.fragments;

import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.UUID;

import de.rtcustomz.walloflight.util.Client;
import de.rtcustomz.walloflight.R;
import de.rtcustomz.walloflight.dialogs.AlertDialogFragment;
import de.rtcustomz.walloflight.dialogs.BrushChooser;
import de.rtcustomz.walloflight.views.DrawingView;

/**
 * This is demo code to accompany the Mobiletuts+ tutorial series:
 * - Android SDK: Create a Drawing App
 * 
 * Sue Smith
 * August 2013
 *
 */
public class DrawingFragment extends Fragment implements OnClickListener, BrushChooser.OnBrushClickListener, AlertDialogFragment.OnClickListener {

	//custom drawing view
	private DrawingView drawView;
	//buttons
	private ImageButton currPaint, drawBtn, eraseBtn, newBtn, saveBtn;

	private static final String ARG_CLIENT = "client";
	//private Client client;

	public static Fragment newInstance(/*Client client*/) {
		Fragment fragment = new DrawingFragment();

		/*Bundle args = new Bundle();
		args.putParcelable(ARG_CLIENT, client);
		fragment.setArguments(args);*/

		return fragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_drawing, container,false);

		//client = getArguments().getParcelable(ARG_CLIENT);

		//get drawing view
		drawView = (DrawingView)rootView.findViewById(R.id.drawing);
		//drawView.setClient(client);

		//get the palette and first color button
		LinearLayout paintColorsTop = (LinearLayout)rootView.findViewById(R.id.paint_colors_top);
		LinearLayout paintColorsBottom = (LinearLayout)rootView.findViewById(R.id.paint_colors_bottom);

		OnClickListener switchColorListener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				paintClicked(v);
			}
		};

		// set onClickListener for every color button on the top palette
		for(int i=0; i<paintColorsTop.getChildCount(); i++) {
			paintColorsTop.getChildAt(i).setOnClickListener(switchColorListener);
		}

		// set onClickListener for every color button on the bottom palette
		for(int i=0; i<paintColorsBottom.getChildCount(); i++) {
			paintColorsBottom.getChildAt(i).setOnClickListener(switchColorListener);
		}

		//get the palette and first color button
		currPaint = (ImageButton)paintColorsTop.getChildAt(0);
		currPaint.setImageDrawable(getResources().getDrawable(R.drawable.paint_pressed));

		//draw button
		drawBtn = (ImageButton)rootView.findViewById(R.id.draw_btn);
		drawBtn.setOnClickListener(this);

		//set initial size
		drawView.setBrushSize(getResources().getInteger(R.integer.medium_size));

		//erase button
		eraseBtn = (ImageButton)rootView.findViewById(R.id.erase_btn);
		eraseBtn.setOnClickListener(this);

		//new button
		newBtn = (ImageButton)rootView.findViewById(R.id.new_btn);
		newBtn.setOnClickListener(this);

		//save button
		saveBtn = (ImageButton)rootView.findViewById(R.id.save_btn);
		saveBtn.setOnClickListener(this);

		return rootView;
	}

	//user clicked paint
	public void paintClicked(View view) {
		//use chosen color

		//set erase false
		drawView.setErase(false);
		drawView.setBrushSize(drawView.getLastBrushSize());

		if(view!=currPaint) {
			ImageButton imgView = (ImageButton)view;
			String color = view.getTag().toString();
			drawView.setColor(color);
			//update ui
			imgView.setImageDrawable(getResources().getDrawable(R.drawable.paint_pressed));
			// TODO: int paint_pressed = getResources().getIdentifier("paint_pressed", "drawable", getActivity().getPackageName());
			// TODO: imgView.setImageResource(paint_pressed);
			currPaint.setImageDrawable(getResources().getDrawable(R.drawable.paint));
			currPaint=(ImageButton)view;
		}
	}

	@Override
	public void onClick(View view) {
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		Fragment prev = getFragmentManager().findFragmentByTag("dialog");

		if (prev != null) {
			ft.remove(prev);
		}

		ft.addToBackStack(null);

		DialogFragment dialog;

		if(view.getId()== R.id.draw_btn) {
			dialog = BrushChooser.newInstance("Brush size:", false);
		}
		else if(view.getId()==R.id.erase_btn) {
			dialog = BrushChooser.newInstance("Eraser size:", true);
		}
		else if(view.getId()==R.id.new_btn) {
			dialog = AlertDialogFragment.newInstance(R.id.new_btn, "New drawing", "Start new drawing (you will lose the current drawing)?", "Yes", "Cancel");
		}
		else if(view.getId()==R.id.save_btn) {
			dialog = AlertDialogFragment.newInstance(R.id.save_btn, "Save drawing", "Save drawing to device Gallery?", "Yes", "Cancel");
		} else {
			return;
		}

		dialog.setTargetFragment(this, 0);
		dialog.show(ft, "dialog");
	}

	@Override
	public void onBrushClick(int brushSize, boolean erase) {
		drawView.setErase(erase);
		drawView.setBrushSize(brushSize);
		if(!erase) {
			drawView.setLastBrushSize(brushSize);
		}
	}

	@Override
	public void onDialogPositiveClick(int initiator, DialogFragment dialog) {
		switch(initiator) {
			case R.id.new_btn:
				drawView.startNew();
				break;
			case R.id.save_btn:
				saveBitmap();
				break;
		}
	}

	@Override
	public void onDialogNegativeClick(int initiator, DialogFragment dialog) {
		dialog.getDialog().cancel();
	}

	private void saveBitmap() {
		drawView.setDrawingCacheEnabled(true);

		//attempt to save
		String imgSaved = MediaStore.Images.Media.insertImage(
				getActivity().getContentResolver(), drawView.getDrawingCache(),
				UUID.randomUUID().toString()+".png", "drawing");

		//feedback
		if(imgSaved!=null) {
			Toast savedToast = Toast.makeText(getActivity(), "Drawing saved to Gallery!", Toast.LENGTH_SHORT);
			savedToast.show();
		} else {
			Toast unsavedToast = Toast.makeText(getActivity(), "Oops! Image could not be saved.", Toast.LENGTH_SHORT);
			unsavedToast.show();
		}

		drawView.destroyDrawingCache();
	}
}
