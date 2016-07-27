package com.ctt535.start.shambook;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

/**
 * Created by vumin_000 on 20/07/2016.
 */
public class BookOptionMenuAdapter extends FragmentPagerAdapter {
    public BookOptionMenuAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int index) {

        switch (index) {
            case 0:
                // Top Rated fragment activity
                //return new TopRatedFragment();
            case 1:
                // Games fragment activity
                //return new GamesFragment();
            case 2:
                // Movies fragment activity
               // return new MoviesFragment();
        }

        return null;
    }

    @Override
    public int getCount() {
        // get item count - equal to number of tabs
        return 3;
    }
}
