package com.bole.dragpractice;

import android.app.Activity;
import android.content.ClipData;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.DragShadowBuilder;
import android.view.View.OnDragListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class DragActivity extends Activity {
    LinearLayout bottomLayout;
    LinearLayout topLayout;
    TextView textView;
    Animation animation;
    View topView;
    View bottomView;
    LinearLayout containerBottom;
    ViewGroup viewGroupBottom;
    ViewGroup viewGroupTop;
    private Handler mUiHandler = new Handler();
    final static String REQUEST_URL = "http://www.timeapi.org/utc/now.json";


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setUp();
    }

    private void setUp() {
        textView = (TextView) findViewById(R.id.textview_clock);
        textView.getBackground().setFilterBitmap(true);
        topLayout = (LinearLayout) findViewById(R.id.topLayout);
        bottomLayout = (LinearLayout) findViewById(R.id.bottomLayout);

        bottomLayout.animate().translationY(bottomLayout.getHeight()).alpha(1.0f);
        textView.setOnTouchListener(new MyTouchListener());
        topLayout.setOnDragListener(new DragListenerTop());
        bottomLayout.setOnDragListener(new DragListenerBottom());
        animation = AnimationUtils.loadAnimation(this, R.anim.rotation);

        startRotation();
        new getTimeAsycnTask().execute(REQUEST_URL);
    }

    private void startRotation() {
        new Thread(new Runnable() {
            public void run() {
                mUiHandler.post(new Runnable() {
                    public void run() {
                        textView.startAnimation(animation);
                    }
                });
            }
        }).start();
    }

    private final class MyTouchListener implements OnTouchListener {
        public boolean onTouch(View view, MotionEvent motionEvent) {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    ClipData data = ClipData.newPlainText("", "");
                    DragShadowBuilder shadowBuilder = new DragShadowBuilder(view);
                    view.startDrag(data, shadowBuilder, view, 0);
                    break;
            }
            return true;
        }
    }

    private final class DragListenerTop implements OnDragListener {
        @Override
        public boolean onDrag(View v, DragEvent event) {
            topView = (View) event.getLocalState();
            viewGroupTop = (ViewGroup) topView.getParent();

            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    bottomLayout.animate().translationY(0).alpha(1.0f);
                    break;
                case DragEvent.ACTION_DROP:
                    bottomLayout.animate().translationY(bottomLayout.getHeight()).alpha(1.0f);
                    break;
                case DragEvent.ACTION_DRAG_EXITED:
                    break;
            }
            return true;
        }
    }

    private final class DragListenerBottom implements OnDragListener {
        Drawable enterShape = getResources().getDrawable(R.drawable.shape_droptarget);
        Drawable normalShape = getResources().getDrawable(R.drawable.shape);

        @Override
        public boolean onDrag(View v, DragEvent event) {
            bottomView = (View) event.getLocalState();
            viewGroupBottom = (ViewGroup) bottomView.getParent();
            containerBottom = (LinearLayout) v;

            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    v.clearAnimation();
                    break;
                case DragEvent.ACTION_DRAG_ENTERED:
                    bottomLayout.animate().translationY(0).alpha(1.0f);
                    v.setBackgroundDrawable(enterShape);
                    break;
                case DragEvent.ACTION_DRAG_EXITED:
                    v.setBackgroundDrawable(normalShape);
                    break;
                case DragEvent.ACTION_DROP:
                    viewGroupBottom.removeView(bottomView);
                    containerBottom.addView(bottomView);
                    bottomView.setVisibility(View.VISIBLE);
                    viewGroupTop.setVisibility(View.INVISIBLE);
                    //The object in the Drop area is not draggable
                    textView.setOnTouchListener(null);
                    Toast.makeText(getApplicationContext(), R.string.square_dropped, Toast.LENGTH_SHORT).show();
                    break;
                case DragEvent.ACTION_DRAG_ENDED:
                    v.setBackgroundDrawable(normalShape);
                    break;
                default:
            }
            return true;
        }
    }

    private class getTimeAsycnTask extends AsyncTask<String, String, String> {
        @Override
            protected String doInBackground(String... params) {
            String realtimeString = "";
            try {
                URL url = new URL(params[0]);
                HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
                httpConn.setRequestMethod("GET");
                httpConn.connect();
                if(httpConn.getResponseCode() == HttpURLConnection.HTTP_OK){
                    realtimeString = convertInputStreamIntoJSONObject(httpConn.getInputStream());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return realtimeString;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            textView.setText(result);

            new Handler().postDelayed(new Runnable() {
                public void run() {
                    new getTimeAsycnTask().execute(REQUEST_URL);
                }
            }, 700);
        }
    }

    private String convertInputStreamIntoJSONObject(InputStream inputStream) {
        BufferedReader bR = new BufferedReader(new InputStreamReader(inputStream));
        String content = "";
        StringBuilder responseStrBuilder = new StringBuilder();
        try {
            while ((content = bR.readLine()) != null) {
                responseStrBuilder.append(content);
            }
            inputStream.close();
            JSONObject result = new JSONObject(responseStrBuilder.toString());
            String temp = result.getString("dateString");
            content = temp.substring(0, temp.indexOf("+"));//Split the string, get rid of unused data
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content;
    }
}
