package com.bignerdranch.android.photogallery;

import android.support.v4.app.Fragment;

import com.bignerdranch.android.photogallery.base.SingleFragmentActivity;

public class PhotoGalleryActivity extends SingleFragmentActivity {

    @Override
    protected Fragment createFragment() {
        return PhotoGalleryFragment.newInstance();
    }

}
