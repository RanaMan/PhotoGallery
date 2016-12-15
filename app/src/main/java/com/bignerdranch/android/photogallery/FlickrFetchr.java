package com.bignerdranch.android.photogallery;

import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by rana_ on 12/15/2016.
 */

public class FlickrFetchr {

    private static final String TAG="FlickerFetcher";
    private static final String API_KEY ="a4e7a39c49de7a3e5465f3c95be10261";
    private static final String FETCH_RECENT_METHOD="flickr.photos.getRecent";
    private static final String SEARCH_METHOD="flickr.photos.search";
    private static final Uri ENDPOINT = Uri
            .parse("https://api.flickr.com/services/rest/")
            .buildUpon()
            .appendQueryParameter("api_key", API_KEY)
            .appendQueryParameter("format","json")
            .appendQueryParameter("nojsoncallback" , "1")
            .appendQueryParameter("extras","url_s")
            .build();



    List<GalleryItem> items = new ArrayList<>();

    public byte[] getURLBytes(String urlSpec) throws IOException{

        // Create the URL & the HTTP Connection
        URL url = new URL(urlSpec);
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();

        try{
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = connection.getInputStream();

            if(connection.getResponseCode() != HttpURLConnection.HTTP_OK){
                throw new IOException(connection.getResponseMessage() + " with " + urlSpec);
            }

            int bytesRead = 0;
            byte[] buffer = new byte[1024];
            while ((bytesRead = in.read(buffer)) > 0 ){
                out.write(buffer, 0, bytesRead);
            }
            out.close();
            return out.toByteArray();
        }finally {
            connection.disconnect();
        }

    }

    public String getURLString (String urlSpec) throws  IOException{
        return new String (getURLBytes(urlSpec));
    }

    public List<GalleryItem> downloadGalleryItems(String url){

        try{
            String jsonString = getURLString(url);
            Log.i(TAG, "fetchItems: Recieved JSON" + jsonString);

            JSONObject jsonBody = new JSONObject(jsonString);
            Log.i(TAG, "fetchItems: Parsed JSON");
            parseItems(items, jsonBody);
            Log.d(TAG, "fetchItems: we have ["+ items.size()+"] items");
        }catch (Exception E){
            E.printStackTrace();
        }
        return items;
    }

    private String buildURL(String method, String query){
        Uri.Builder uriBuilder = ENDPOINT.buildUpon()
                .appendQueryParameter("method" , method);

        if(method.equals(SEARCH_METHOD)){
            uriBuilder.appendQueryParameter("text", query);
        }
        return uriBuilder.build().toString();
    }

    public List<GalleryItem> fetchRecentPhotos(){
        String url = buildURL(FETCH_RECENT_METHOD, null);
        return downloadGalleryItems(url);
    }

    public List<GalleryItem> searchPhotosPhotos( String query ){
        String url = buildURL(SEARCH_METHOD, query);
        return downloadGalleryItems(url);
    }


    private void parseItems(List<GalleryItem> items, JSONObject jsonBody)
        throws IOException, JSONException{

        JSONObject photosJsonObject = jsonBody.getJSONObject("photos");
        JSONArray photoJsonArray = photosJsonObject.getJSONArray("photo");

        for(int i=0; i<photoJsonArray.length();i++){
            JSONObject photoObject = photoJsonArray.getJSONObject(i);

            GalleryItem item = new GalleryItem();
            item.setId(photoObject.getString("id"));
            item.setCaption(photoObject.getString("title"));

            if(!photoObject.has("url_s")){
                continue;
            }
            item.setUrl(photoObject.getString("url_s"));
            items.add(item);


        }

    }
}
