package com.nerodns.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.VpnService;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import okhttp3.*;
import org.json.JSONObject;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private static final int VPN_REQUEST_CODE = 0x0F;
    private EditText etLicense;
    private Button btnConnect;
    private TextView tvStatus;
    private View indicatorDot;
    private boolean isConnected = false;
    private final OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etLicense = findViewById(R.id.etLicense);
        btnConnect = findViewById(R.id.btnConnect);
        tvStatus = findViewById(R.id.tvStatus);
        indicatorDot = findViewById(R.id.indicatorDot);

        btnConnect.setOnClickListener(v -> {
            if (isConnected) {
                disconnectVpn();
            } else {
                verifyAndConnect();
            }
        });
    }

    private void verifyAndConnect() {
        String license = etLicense.getText().toString().trim().toUpperCase();
        if (license.isEmpty()) {
            Toast.makeText(this, "PLEASE ENTER LICENSE KEY", Toast.LENGTH_SHORT).show();
            return;
        }

        tvStatus.setText("STATUS: AUTHENTICATING...");
        indicatorDot.setBackgroundColor(Color.parseColor("#FFD166"));

        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        JSONObject json = new JSONObject();
        try { json.put("token", license); } catch (Exception ignored) {}

        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
                .url("http://82.22.175.205:5000/api/activate")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    tvStatus.setText("STATUS: CONNECTION FAILED");
                    indicatorDot.setBackgroundColor(Color.parseColor("#FF3366"));
                    Toast.makeText(MainActivity.this, "Authentication server unreachable", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String resStr = response.body().string();
                runOnUiThread(() -> {
                    try {
                        JSONObject res = new JSONObject(resStr);
                        if (res.optBoolean("success")) {
                            startVpnPermission();
                        } else {
                            tvStatus.setText("STATUS: " + res.optString("message", "INVALID TOKEN").toUpperCase());
                            indicatorDot.setBackgroundColor(Color.parseColor("#FF3366"));
                        }
                    } catch (Exception e) {
                        tvStatus.setText("STATUS: PARSE ERROR");
                        indicatorDot.setBackgroundColor(Color.parseColor("#FF3366"));
                    }
                });
            }
        });
    }

    private void startVpnPermission() {
        Intent intent = VpnService.prepare(this);
        if (intent != null) {
            startActivityForResult(intent, VPN_REQUEST_CODE);
        } else {
            onActivityResult(VPN_REQUEST_CODE, Activity.RESULT_OK, null);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VPN_REQUEST_CODE && resultCode == RESULT_OK) {
            Intent intent = new Intent(this, DnsVpnService.class);
            startService(intent);
            isConnected = true;
            btnConnect.setText("DISCONNECT");
            btnConnect.setBackgroundColor(Color.parseColor("#FF0055"));
            tvStatus.setText("STATUS: ACTIVE & PROTECTED");
            indicatorDot.setBackgroundColor(Color.parseColor("#00F5D4"));
        }
    }

    private void disconnectVpn() {
        Intent intent = new Intent(this, DnsVpnService.class);
        intent.setAction("STOP");
        startService(intent);
        isConnected = false;
        btnConnect.setText("CONNECT NETWORK");
        btnConnect.setBackgroundResource(R.drawable.btn_neon);
        tvStatus.setText("STATUS: DISCONNECTED");
        indicatorDot.setBackgroundColor(Color.parseColor("#FF3366"));
    }
}
