package de.rtcustomz.walloflight.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.rtcustomz.walloflight.R;
import de.rtcustomz.walloflight.fragments.ProcessImageFragment.Mode;

public class TabbedFragment extends Fragment {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    //private static final String ARG_CLIENT = "client";
    //private Client client;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;
    private TabLayout tabLayout;

    //private Fragment processImageFragment;

    public static Fragment newInstance() {
        Fragment fragment = new TabbedFragment();

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_tabbed, container, false);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getChildFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) rootView.findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        tabLayout = (TabLayout) getActivity().findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        if(tabLayout.getVisibility() == TabLayout.GONE) {
            tabLayout.setVisibility(TabLayout.VISIBLE);
        }

        return rootView;
    }

    @Override
    public void onDestroyView() {
        tabLayout.setVisibility(TabLayout.GONE);
        super.onDestroyView();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        String tag = makeFragmentName(mViewPager.getId(), mViewPager.getCurrentItem());
        Log.e("asdasd", tag);
        Fragment page = getChildFragmentManager().findFragmentByTag(tag);
        page.onActivityResult(requestCode, resultCode, data);
    }

    private String makeFragmentName(int viewId, int position) {
        return "android:switcher:" + viewId + ":" + position;
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            Mode mode = Mode.modes[position];
            return ProcessImageFragment.newInstance(mode);
        }

        @Override
        public int getCount() {
            // enable all modes in TabbedFragment
            return Mode.modes.length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Mode mode = Mode.modes[position];
            return mode.getTitle();
        }
    }
}
