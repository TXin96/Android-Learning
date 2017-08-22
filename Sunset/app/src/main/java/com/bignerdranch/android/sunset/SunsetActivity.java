package com.bignerdranch.android.sunset;

import android.support.v4.app.Fragment;

import com.bignerdranch.android.sunset.base.SingleFragmentActivity;

public class SunsetActivity extends SingleFragmentActivity {

    @Override
    protected Fragment createFragment() {
        return SunsetFragment.newInstance();
    }

}
