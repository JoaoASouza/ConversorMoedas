package com.example.conversormoedas;

import androidx.appcompat.app.AppCompatActivity;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    private String selectedCurrency;
    private final OkHttpClient client = new OkHttpClient();
    private SensorManager sensorManager;
    private Sensor proximitySensor;
    private SensorEventListener proximitySensorListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Spinner spinner = findViewById(R.id.spinner1);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.currencies, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        if (proximitySensor == null) {
            Toast.makeText(this, "Sensor de proximidade não disponível", Toast.LENGTH_LONG).show();
            finish();
        }

        proximitySensorListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (event.values[0] < proximitySensor.getMaximumRange()) {
                    makeRequest();
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };

        sensorManager.registerListener(proximitySensorListener, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL);

    }

    @Override
    protected void onPause() {
        super.onPause();

        sensorManager.unregisterListener(proximitySensorListener);
    }

    public void onButtonClick(View view) {
        makeRequest();
    }

    public void makeRequest() {

        Request request = new Request.Builder()
                .url("https://economia.awesomeapi.com.br/last/" + selectedCurrency + "-BRL")
                .build();


        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {

                    if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

                    String responseBodyString = responseBody.string();

                    Gson gson = new Gson();
                    Currency currencyObj = gson.fromJson(responseBodyString.substring(10, responseBodyString.length()-1), Currency.class);

                    final String name = currencyObj.getName();
                    final String bid = currencyObj.getBid();
                    final String low = currencyObj.getLow();
                    final String high = currencyObj.getHigh();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TextView nameTextView = findViewById(R.id.nameTextView);
                            TextView bidTextView = findViewById(R.id.bidTextView);
                            TextView highTextView = findViewById(R.id.highTextView);
                            TextView lowTextView = findViewById(R.id.lowTextView);

                            nameTextView.setText(name);
                            bidTextView.setText("Cotação atual: R$ " + bid);
                            highTextView.setText("Máxima do dia: R$ " + high);
                            lowTextView.setText("Mínima do dia: R$ " + low);
                        }
                    });
                }
            }
        });


    }


    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        String text = parent.getItemAtPosition(position).toString();
        this.selectedCurrency = text;
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}