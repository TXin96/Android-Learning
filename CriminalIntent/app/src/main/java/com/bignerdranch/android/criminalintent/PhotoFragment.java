package com.bignerdranch.android.criminalintent;

import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;


/**
 * Created by michaeltan on 2017/8/8.
 */

public class PhotoFragment extends DialogFragment {
    private static final String ARG_PHOTO = "date";

    private ImageView mPhotoView;

    public static PhotoFragment newInstance(String path) {
        Bundle args = new Bundle();
        args.putSerializable(ARG_PHOTO, path);

        PhotoFragment photoFragment = new PhotoFragment();
        photoFragment.setArguments(args);

        return photoFragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final String path = (String) getArguments().getSerializable(ARG_PHOTO);
        View v = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_photo, null);
        Bitmap bitmap = BitmapFactory.decodeFile(path);
        mPhotoView = (ImageView) v.findViewById(R.id.dialog_photo_image_view);
        mPhotoView.setImageBitmap(bitmap);
        return new AlertDialog.Builder(getActivity()).setView(mPhotoView).create();
    }


}
