package com.example.fragments;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private ArrayList<NewsItem> news;
    RecyclerView recView;
    RecyclerViewAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        recView = findViewById(R.id.recView);
        adapter = new RecyclerViewAdapter(this);
        recView.setAdapter(adapter);
        recView.setLayoutManager(new LinearLayoutManager(this));
        news = new ArrayList<>();
        getDataAsyncTask getDataAsyncTask = new getDataAsyncTask();
        getDataAsyncTask.execute();

    }

    private class getDataAsyncTask extends AsyncTask<Void, Void, Void>{
        @Override
        protected Void doInBackground(Void... voids) {
            InputStream inputStream = getInputStream();
            try {
                initXMLPullParser(inputStream);
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            adapter.setNews(news);
            super.onPostExecute(aVoid);
        }
    }

    private String getContent(XmlPullParser parser, String tagName ){
        Log.d(TAG, "getContent: started for tag: "+ tagName);
        try {
            parser.require(XmlPullParser.START_TAG,null,tagName);
            String content = "";
            if(parser.next() == XmlPullParser.TEXT){
                content = parser.getText();
                parser.next();
            }
            return content;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        }
        return null;
    }

    private InputStream getInputStream(){
        Log.d(TAG, "getInputStream: started!");
        try {
            URL url = new URL("https://www.autosport.com/rss/feed/f1");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setDoInput(true);
            connection.connect();
            return connection.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void skipTag(XmlPullParser parser) throws XmlPullParserException, IOException {
        Log.d(TAG, "skipTag: skipping " + parser.getName());
        if(parser.getEventType() != XmlPullParser.START_TAG){
            throw new IllegalStateException();
        }
        int number = 1;
        while (number != 0){
            switch (parser.next()){
                case XmlPullParser.START_TAG:
                    number++;
                    break;
                case XmlPullParser.END_TAG:
                    number--;
                    break;
                default:
                    break;
            }
        }
    }

    private void initXMLPullParser(InputStream inputStream) throws XmlPullParserException, IOException {
        Log.d(TAG, "initXMLPullParser: called");
        XmlPullParser parser = Xml.newPullParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES,false);
        parser.setInput(inputStream,null);
        parser.nextTag();

        parser.require(XmlPullParser.START_TAG,null,"rss");
        while(parser.next() != XmlPullParser.END_TAG){
            if(parser.getEventType() != XmlPullParser.START_TAG){
                continue;
            }
            parser.require(XmlPullParser.START_TAG,null,"channel");
            while(parser.next() != XmlPullParser.END_TAG){
                if(parser.getEventType() != XmlPullParser.START_TAG){
                    continue;
                }
                if(parser.getName().equals("item")){
                    parser.require(XmlPullParser.START_TAG,null,"item");

                    String title = "";
                    String desc = "";
                    String link = "";
                    String date = "";
                    while(parser.next()!= XmlPullParser.END_TAG){
                        if(parser.getEventType() != XmlPullParser.START_TAG){
                            continue;
                        }
                        String tagName = parser.getName();
                        switch(tagName){
                            case "title":
                                title = getContent(parser,"title");
                                break;
                            case "description":
                                desc = getContent(parser,"description");
                                break;
                            case "link":
                                link = getContent(parser,"link");
                                break;
                            case "pubDate":
                                String fullDate = getContent(parser,"pubDate");
                                date = fullDate.substring(0, fullDate.length()-9);
                                break;
                            default:
                                skipTag(parser);
                                break;
                        }
                    }
                    NewsItem item = new NewsItem(title,desc,link,date);
                    news.add(item);
                    Log.d(TAG, "initXMLPullParser: item "+ item.toString() + " added");
                } else{
                    skipTag(parser);
                }
            }

        }
    }
}
