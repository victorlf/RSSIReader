package com.example.rssireader.ui;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.rssireader.R;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.w3c.dom.Document;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GraphViewActivity extends AppCompatActivity {

    private static final String TAG = "GraphView";
    private final Handler mHandler = new Handler();
    LineGraphSeries<DataPoint> series;
    private Runnable mTimer;
    private double graphLastXValue = 5d;
    //private GraphView graph = (GraphView) findViewById(R.id.graph);
    private int count = 0;
    private Context context;

    final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1234;

    // List for RSSI values to calculate its mode
    List<Integer> rssi_values = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph_view);

        /*GraphView graph = findViewById(R.id.graph);
        series = new LineGraphSeries<>();
        graph.addSeries(series);
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(40);*/

        /*GraphView graph = (GraphView) findViewById(R.id.graph);
        LineGraphSeries<DataPoint> series = new LineGraphSeries<>(new DataPoint[] {
                new DataPoint(0, 1),
                new DataPoint(1, 5),
                new DataPoint(2, 3),
                new DataPoint(3, 2),
                new DataPoint(4, 6)
        });
        graph.addSeries(series);

        graph.setBackgroundColor(Color.WHITE);*/


        // Plot graph from DB
        SQLiteDatabase bancoDados = openOrCreateDatabase("app", MODE_PRIVATE, null);
        // Recover data
        Cursor cursor = bancoDados.rawQuery("SELECT id, medida FROM rssi", null);

        int indiceId = cursor.getColumnIndex("id");
        int indiceMedida = cursor.getColumnIndex("medida");

        // The cursor is at the last position so we move to next
        cursor.moveToFirst();
        series = new LineGraphSeries<>();
        while (!cursor.isLast()) {

            int id = cursor.getInt(indiceId);
            int medida = cursor.getInt(indiceMedida);

            series.appendData(new DataPoint(id, medida), true, 40);

            // Add to the list of RSSI values to calculate its mode
            rssi_values.add(medida);

            cursor.moveToNext();
        }

        GraphView graph = (GraphView) findViewById(R.id.graph);
        // Necessary to make graph scrollable
        graph.getViewport().setScalable(true);  // activate horizontal zooming and scrolling
        graph.getViewport().setScrollable(true);  // activate horizontal scrolling
        graph.getViewport().setScalableY(true);  // activate horizontal and vertical zooming and scrolling
        graph.getViewport().setScrollableY(true);  // activate vertical scrolling

        graph.setBackgroundColor(Color.WHITE);

        graph.addSeries(series);

        // Mode of the RSSI values
        int x  = mode(rssi_values, rssi_values.size());

        // Calculate distance
        // Total Path Loss = Tx-power less RSSI
        int l_total = -21 - x;
        // Depends on the frequency equals 2400MHz
        double log = Math.log10(2400);
        double l_d0 = 20*log - 28;
        // Operations
        double l_final = l_total - l_d0;
        // Depends on the same frequency and the environment
        int n_for_Office = 30;
        // Operations
        l_final = l_final/n_for_Office;
        // Final Operation for distance
        double distance = Math.pow(10, l_final);

        // Print Total Path Loss and Distance
        TextView textRssiMode = findViewById(R.id.textRssiMode);
        textRssiMode.setText("RSSI = " + l_total + " e Distância = " + distance);

    }

    /*@Override
    public void onResume() {
        super.onResume();
        mTimer = new Runnable() {
            @Override
            public void run() {
                graphLastXValue += 1d;
                series.appendData(new DataPoint(graphLastXValue, getRandom()), true, 40);
                mHandler.postDelayed(this, 200);
            }
        };
        mHandler.postDelayed(mTimer, 1000);
    }*/

    private double getRandom() {
        return mLastRandom += mRand.nextDouble()*0.5 - 0.25;
    }

    double mLastRandom = 2;
    Random mRand = new Random();

    public void snapshot(View view) {
        GraphView graph = findViewById(R.id.graph);
        Bitmap bitmap = graph.takeSnapshot();


        // Saves the snapshot at internal storage in the folder MyDirName
        //File path= new File(getApplicationContext().getFilesDir(), "MyAppName" + File.separator + "Images");
        File path = new File(Environment.getExternalStorageDirectory(), "MyDirName");
        if(!path.exists()){
            path.mkdirs();
        }
        File outFile = new File(path, "graphWhite" + ".jpeg");

        try {
            FileOutputStream outputStream = new FileOutputStream(outFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.close();
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Saving received message failed with", e);
        } catch (IOException e) {
            Log.e(TAG, "Saving received message failed with", e);
        }


        // Shares the snapshot on social media
        /*if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (shouldShowRequestPermissionRationale(
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
                // Explain to the user why we need to read the contacts
            }

            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);

            // MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE is an
            // app-defined int constant that should be quite unique

            return;
        }


        graph.takeSnapshotAndShare(this, "exampleGraph", "GraphViewSnapshot");
        */
    }

    // Mode for the RSSI values
    static int mode(List<Integer> a, int size) {
        int maxValue = 0, maxCount = 0, i, j;

        for (i = 0; i < size; ++i) {
            int count = 0;
            for (j = 0; j < size; ++j) {
                if (a.get(j) == a.get(i))
                    ++count;
            }

            if (count > maxCount) {
                maxCount = count;
                maxValue = a.get(i);
            }
        }
        return maxValue;
    }

}



