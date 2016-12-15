package com.bignerdranch.android.photogallery;

import android.graphics.Bitmap;
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
import android.widget.ImageView;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by rana_ on 12/15/2016.
 */

public class PhotoGalleryFragment extends Fragment{

    private static final String TAG = "PhotoGalleryFragment";

    private RecyclerView mPhotoRecyclerView;
    private List<GalleryItem> mItems = new ArrayList<>();

    //This is why we use the generic in the constructor
    private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;


    //This is how we want the constructor for our fragment to be called
    public static PhotoGalleryFragment newInstance() {
        PhotoGalleryFragment fragment = new PhotoGalleryFragment();
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        updateItems();
        setRetainInstance(true);

        Handler responseHandler = new Handler();
        mThumbnailDownloader = new ThumbnailDownloader<>(responseHandler);
        mThumbnailDownloader.setThumbnailDownloadListener(
                new ThumbnailDownloader.ThumbnailDownloadListener<PhotoHolder>(){
                    @Override
                    public void onThumbnailDownloaded(PhotoHolder photoHolder, Bitmap bitmap){
                        Drawable drawable = new BitmapDrawable(getResources(), bitmap);
                        photoHolder.bindDrawable(drawable);
                    }
                }
        );

        mThumbnailDownloader.start();
        mThumbnailDownloader.getLooper();
        //This is why the Thumbnail methods don't need to be static... there is really only one instance
        //of it living in the main fragment
        Log.i(TAG, "onCreate: Background thread started");
    }

    @Override
    public void onDestroyView(){
        super.onDestroyView();
        mThumbnailDownloader.clearQueue();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        mThumbnailDownloader.quit();
        Log.d(TAG, "onDestroy: Background thread stopped!");
    }



    //This is the money-shot for our fragment
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        setHasOptionsMenu(true);

        View v = inflater.inflate(R.layout.fragment_photo_gallery, container,false);
        mPhotoRecyclerView = (RecyclerView)v.findViewById(R.id.fragment_photo_gallery_recycler_view);

        //DON'T FORGET THIS!!! -> This is a result of the No LayoutManager Attached, skipping layout message
         mPhotoRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 3));

        setupAdapter();
        return v;
    }

    private void setupAdapter(){
        if(isAdded()){
            Log.i(TAG, "setupAdapter: set the Adapter, by making a new one");
            PhotoAdapter cAdapter= new PhotoAdapter(mItems);
            mPhotoRecyclerView.setAdapter(cAdapter);

        }else{
            Log.i(TAG, "setupAdapter: didn't need to make an adapter, it already existed");
        }
    }

    private class FetchItemsTask extends AsyncTask<Void, Void, List<GalleryItem>>{

        private String mQuery;

        public FetchItemsTask(String query){
            mQuery = query;
        }

        @Override
        protected List<GalleryItem> doInBackground(Void... voids) {

            if(mQuery == null) {
                return new FlickrFetchr().fetchRecentPhotos();
            }else{
                return new FlickrFetchr().searchPhotosPhotos(mQuery);
            }

        }

        @Override
        protected void onPostExecute (List<GalleryItem> items){
            mItems = items;
            setupAdapter();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_photo_gallery,menu);

        MenuItem searchItem = menu.findItem(R.id.menu_item_search);
        final SearchView searchView = (SearchView)searchItem.getActionView();
        
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.d(TAG, "onQueryTextSubmit: Submitted query");
                QueryPreferences.setStoredQuery(getActivity(), query);
                updateItems();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Log.d(TAG, "onQueryTextChange: Text Change");
                return false;
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case R.id.menu_item_clear:
                    QueryPreferences.setStoredQuery(getActivity(),null);
                    updateItems();
                    return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }



    private void updateItems(){
        String query = QueryPreferences.getStoredQuery(getActivity());
        new FetchItemsTask(query).execute();
    }

    /************** PRIVATE CLASS PHOTOHOLDER *****************/
    private class PhotoHolder extends RecyclerView.ViewHolder{

        private ImageView mImageView;

        public PhotoHolder(View itemView){
            super(itemView);
            Log.d(TAG, "PhotoHolder: Created PhotoHolder");
            mImageView = (ImageView)itemView.findViewById(R.id.fragment_photo_gallery_image_view);
        }

        public void bindDrawable(Drawable d){
            mImageView.setImageDrawable(d);
        }

    }

    /************** PRIVATE CLASS PHOTOADAPTER *****************/
    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder> {

        private List<GalleryItem> mGalleryItems;

        public PhotoAdapter(List<GalleryItem> galleryItems){
            //I am setting the incoming GalleryItems to the Adapter's Gallery item list.
            // Wold expect to createHolders for all of them now...

            mGalleryItems = galleryItems;
        }

        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup viewGroup, int viewType){
            Log.d(TAG, "onCreateViewHolder: ");
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View photoHolderView = inflater.inflate(R.layout.gallery_item, viewGroup, false);
            return new PhotoHolder(photoHolderView);
        }

        @Override
        public void onBindViewHolder(PhotoHolder photoHolder, int position){
            Log.i(TAG, "onBindViewHolder: Going to bind item " + position);

            GalleryItem galleryItem = mGalleryItems.get(position);
            mThumbnailDownloader.queueThumbnail(photoHolder, galleryItem.getUrl());

            Drawable placeholder = getResources().getDrawable(R.drawable.bill_up_close);
            photoHolder.bindDrawable(placeholder);

        }

        @Override
        public int getItemCount(){
            return mGalleryItems.size();
        }

    }

}
