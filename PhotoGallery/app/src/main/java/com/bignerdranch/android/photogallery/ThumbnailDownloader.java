package com.bignerdranch.android.photogallery;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by michaeltan on 2017/8/17.
 */

public class ThumbnailDownloader<T> extends HandlerThread {
    public static final int MESSAGE_DOWNLOAD = 0;
    public static final int MESSAGE_PRELOAD = 1;
    private static final String TAG = "ThumbnailDownloader";
    private Boolean mHasQuit = false;
    private Handler mRequestHandler;
    private ConcurrentMap<T, String> mRequestMap = new ConcurrentHashMap<>();
    private Handler mResponseHandler;
    private ThumbnailDownloadListener<T> mThumbnailDownloaderListener;

    private GalleryCache mGalleryCache;

    public ThumbnailDownloader(Handler responseHandler) {
        super(TAG);
        mResponseHandler = responseHandler;
        mGalleryCache = new GalleryCache();
    }

    public void setThumbnailDownloadListener(ThumbnailDownloadListener<T> listener) {
        mThumbnailDownloaderListener = listener;
    }

    @Override
    public boolean quit() {
        mHasQuit = true;
        return super.quit();
    }

    public void queueThumbnail(T target, String url) {
        Log.i(TAG, "Got a URL: " + url);

        if (url == null) {
            mRequestMap.remove(target);
        } else {
            mRequestMap.put(target, url);
            mRequestHandler.obtainMessage(MESSAGE_DOWNLOAD, target).sendToTarget();
        }
    }

    public void preloadThumbnail(String url) throws IOException {
        mRequestHandler.obtainMessage(MESSAGE_PRELOAD, url).sendToTarget();
    }

    public void clearQueue() {
        mRequestHandler.removeMessages(MESSAGE_DOWNLOAD);
    }

    private void handleRequest(final T target) {
        final String url = mRequestMap.get(target);
        final Bitmap bitmap = downloadThumbnail(url);
        if (bitmap == null) {
            return;
        }
        Log.i(TAG, "Bitmap created");

        mResponseHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mRequestMap.get(target) != url || mHasQuit) {
                    return;
                }

                mRequestMap.remove(target);
                mThumbnailDownloaderListener.onThumbnailDownloaded(target, bitmap);
            }
        });

    }

    private Bitmap downloadThumbnail(String url) {
        if (url == null) {
            return null;
        }
        //先从缓存中获取，若没有则下载
        Bitmap bitmap = mGalleryCache.getThumbnail(url);
        if (bitmap != null) {
            return bitmap;
        }

        try {
            byte[] bitmapBytes = new byte[0];
            bitmapBytes = new FlickrFetchr().getUrlBytes(url);
            bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
            //下载后添加到缓存中
            mGalleryCache.addThumbnail(url, bitmap);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return bitmap;
    }

    @Override
    protected void onLooperPrepared() {
        mRequestHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MESSAGE_DOWNLOAD) {
                    T target = (T) msg.obj;
                    Log.i(TAG, "Got a request for URL: " + mRequestMap.get(target));
                    handleRequest(target);
                } else if (msg.what == MESSAGE_PRELOAD) {
                    String url = (String) msg.obj;
                    Log.i(TAG, "Got a request for URL: " + url);
                    downloadThumbnail(url);
                }
            }
        };
    }

    public interface ThumbnailDownloadListener<T> {
        void onThumbnailDownloaded(T target, Bitmap thumbnail);
    }


}
