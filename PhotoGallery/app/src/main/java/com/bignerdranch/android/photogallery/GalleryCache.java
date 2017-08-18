package com.bignerdranch.android.photogallery;


import android.graphics.Bitmap;
import android.support.v4.util.LruCache;

/**
 * Created by michaeltan on 2017/8/18.
 */

public class GalleryCache {
    private LruCache<String, Bitmap> mCache;

    public GalleryCache() {
        //返回Java虚拟机将尝试使用的最大内存
        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        //指定缓存大小
        int cacheSize = maxMemory / 8;
        mCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                //Bitmap的实际大小 注意单位与maxMemory一致
                return value.getByteCount();
            }
        };
    }

    public Bitmap getThumbnail(String url) {
        return mCache.get(url);
    }

    public void addThumbnail(String url, Bitmap bitmap) {
        if (mCache.get(url) == null) {
            //保存到缓存中
            mCache.put(url, bitmap);
        }
    }
}
