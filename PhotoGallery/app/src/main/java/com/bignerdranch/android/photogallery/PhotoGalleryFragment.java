package com.bignerdranch.android.photogallery;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.bignerdranch.android.photogallery.model.GalleryItem;
import com.bignerdranch.android.photogallery.model.QueryPreferences;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by michaeltan on 2017/8/16.
 */

public class PhotoGalleryFragment extends Fragment {
    private static final String TAG = "PhotoGalleryFragment";

    private RecyclerView mPhotoRecyclerView;
    private List<GalleryItem> mItems = new ArrayList<>();
    private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;

    private ProgressBar mProgressBar;

    private int mFetchedPages = 1;
    private int mColumnNumbers = 3;

    public static PhotoGalleryFragment newInstance() {
        return new PhotoGalleryFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
        updateItems();

        Handler responseHandler = new Handler();
        mThumbnailDownloader = new ThumbnailDownloader<>(responseHandler);
        mThumbnailDownloader.setThumbnailDownloadListener(new ThumbnailDownloader.ThumbnailDownloadListener<PhotoHolder>() {
            @Override
            public void onThumbnailDownloaded(PhotoHolder target, Bitmap thumbnail) {
                Drawable drawable = new BitmapDrawable(getResources(), thumbnail);
                target.bindDrawable(drawable);
            }
        });
        mThumbnailDownloader.start();
        mThumbnailDownloader.getLooper();
        Log.i(TAG, "Background thread started");
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_photo_gallery, menu);

        MenuItem searchItem = menu.findItem(R.id.menu_item_search);
        final SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.d(TAG, "onQueryTextSubmit: " + query);
                searchView.onActionViewCollapsed();
                mItems.clear();
                mPhotoRecyclerView.getAdapter().notifyDataSetChanged();
                QueryPreferences.setStoredQuery(getActivity(), query);
                mFetchedPages = 1;
                updateItems();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Log.d(TAG, "onQueryTextChange: " + newText);
                return false;
            }
        });

        searchView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String query = QueryPreferences.getStoredQuery(getActivity());
                searchView.setQuery(query, false);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_clear:
                if (QueryPreferences.getStoredQuery(getActivity()) != null) {
                    QueryPreferences.setStoredQuery(getActivity(), null);
                    updateItems();
                    return true;
                }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void updateItems() {
        String query = QueryPreferences.getStoredQuery(getActivity());
        new FetchItemsTask(query).execute(mFetchedPages);
    }

    private void showProgressBar(boolean ifShow) {
        if (ifShow) {
            mPhotoRecyclerView.setVisibility(View.INVISIBLE);
            mProgressBar.setVisibility(View.VISIBLE);
        } else {
            mProgressBar.setVisibility(View.GONE);
            mPhotoRecyclerView.setVisibility(View.VISIBLE);
        }

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_photo_gallery, container, false);

        mPhotoRecyclerView = view.findViewById(R.id.fragment_photo_gallery_recycler_view);
        mProgressBar = view.findViewById(R.id.fragment_progress_bar);
        showProgressBar(true);

        mPhotoRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), mColumnNumbers));
        mPhotoRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Point size = new Point();
                getActivity().getWindowManager().getDefaultDisplay().getSize(size);
                int newColumns = (int) Math.floor(size.x * 3 / 1440);
                if (newColumns != mColumnNumbers) {
                    GridLayoutManager layoutManager = (GridLayoutManager) mPhotoRecyclerView.getLayoutManager();
                    layoutManager.setSpanCount(newColumns);
                }
                mPhotoRecyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        mPhotoRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                //判断是否滑动到底部
                //computeVerticalScrollExtent()是当前屏幕显示的区域高度，
                //computeVerticalScrollOffset()是当前屏幕之前滑过的距离，
                //computeVerticalScrollRange()是整个View控件的高度。
                if ((recyclerView.computeVerticalScrollExtent() + recyclerView.computeVerticalScrollOffset())
                        >= recyclerView.computeVerticalScrollRange()) {
                    mFetchedPages++;
                    updateItems();
                }
                GridLayoutManager layoutManager = (GridLayoutManager) mPhotoRecyclerView.getLayoutManager();
                int lastPosition = layoutManager.findLastVisibleItemPosition();
                try {
                    preload(lastPosition);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        setAdapter();

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mThumbnailDownloader.quit();
        Log.i(TAG, "Background thread destroyed");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mThumbnailDownloader.clearQueue();
    }

    private void setAdapter() {
        if (isAdded()) {
            mPhotoRecyclerView.setAdapter(new PhotoAdapter(mItems));
        }
    }

    private void preload(int position) throws IOException {
        final int imageBufferSize = 10; //Number of images before & after position to cache

        //设置开始和截止的位置
        int startIndex = Math.max(position - imageBufferSize, 0); //Starting index must be >= 0
        int endIndex = Math.min(position + imageBufferSize, mItems.size() - 1); //Ending index must be <= number of galleryItems - 1

        //遍历并下载
        for (int i = startIndex; i <= endIndex; i++) {
            //当前的无需下载
            if (i == position) continue;

            String url = mItems.get(i).getUrl();
            mThumbnailDownloader.preloadThumbnail(url);
        }
    }

    private class FetchItemsTask extends AsyncTask<Integer, Void, List<GalleryItem>> {
        private String mQuery;

        public FetchItemsTask(String query) {
            mQuery = query;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (mPhotoRecyclerView!=null){
                showProgressBar(true);
            }
        }

        @Override
        protected List<GalleryItem> doInBackground(Integer... integers) {

            if (mQuery == null) {
                return new FlickrFetchr().fetchRecentPhotos(integers[0]);
            } else {
                return new FlickrFetchr().searchPhotos(mQuery, integers[0]);
            }
        }

        @Override
        protected void onPostExecute(List<GalleryItem> galleryItems) {
            showProgressBar(false);
            if (mFetchedPages > 1) {
                mItems.addAll(galleryItems);
                mPhotoRecyclerView.getAdapter().notifyDataSetChanged();
            } else {
                mItems.addAll(galleryItems);
                setAdapter();
            }
        }
    }

    private class PhotoHolder extends RecyclerView.ViewHolder {
        private ImageView mItemImageView;

        public PhotoHolder(View itemView) {
            super(itemView);

            mItemImageView = itemView.findViewById(R.id.fragment_photo_gallery_image_view);
        }

        public void bindDrawable(Drawable drawable) {
            mItemImageView.setImageDrawable(drawable);
        }
    }

    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder> {
        private List<GalleryItem> mGalleryItems;

        public PhotoAdapter(List<GalleryItem> galleryItems) {
            mGalleryItems = galleryItems;
        }

        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View view = inflater.inflate(R.layout.gallery_item, parent, false);
            return new PhotoHolder(view);
        }

        @Override
        public void onBindViewHolder(PhotoHolder holder, int position) {
            GalleryItem galleryItem = mGalleryItems.get(position);
            Picasso.with(getActivity()).load(galleryItem.getUrl()).placeholder(R.drawable.zeng).into(holder.mItemImageView);
            //Drawable placeholder = ContextCompat.getDrawable(getContext(), R.mipmap.ic_launcher);
            //holder.bindDrawable(placeholder);
            //mThumbnailDownloader.queueThumbnail(holder, galleryItem.getUrl());
        }

        @Override
        public int getItemCount() {
            return mGalleryItems.size();
        }
    }
}
