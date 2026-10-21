package com.youpi.observer;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;


public class MainActivity extends Activity {
    private static final int REQ_SCAN_QR = 2002;

    private ApiClient apiClient;

    private TextView tvStatusBadge;
    private TextView tvDeviceDetails;
    private Button btnScanQr;
    private EditText etBaseUrl;
    private EditText etEnrollmentToken;
    private EditText etDeviceName;
    private Button btnPairDevice;
    private Button btnUnpairDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        apiClient = new ApiClient(this);

        // Build premium, responsive modern UI programmatically
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(Color.parseColor("#090d16"));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(40, 48, 40, 48);

        // Header Title
        TextView tvTitle = new TextView(this);
        tvTitle.setText("YOUPI Gateway Connector");
        tvTitle.setTextColor(Color.WHITE);
        tvTitle.setTextSize(22f);
        tvTitle.setTypeface(null, Typeface.BOLD);
        root.addView(tvTitle);

        TextView tvSubtitle = new TextView(this);
        tvSubtitle.setText("Connect this device to your YOUPI gateway");
        tvSubtitle.setTextColor(Color.parseColor("#94a3b8"));
        tvSubtitle.setTextSize(13f);
        tvSubtitle.setPadding(0, 4, 0, 32);
        root.addView(tvSubtitle);

        // Status Card
        LinearLayout cardStatus = createCard();
        tvStatusBadge = new TextView(this);
        tvStatusBadge.setText("STATUS: CHECKING...");
        tvStatusBadge.setTextSize(15f);
        tvStatusBadge.setTypeface(null, Typeface.BOLD);
        cardStatus.addView(tvStatusBadge);

        tvDeviceDetails = new TextView(this);
        tvDeviceDetails.setText("Device ID: Checking...\nModel: " + Build.MANUFACTURER + " " + Build.MODEL);
        tvDeviceDetails.setTextColor(Color.parseColor("#cbd5e1"));
        tvDeviceDetails.setTextSize(12f);
        tvDeviceDetails.setPadding(0, 12, 0, 0);
        cardStatus.addView(tvDeviceDetails);
        root.addView(cardStatus);

        LinearLayout privacyCard = createCard();
        TextView privacyText = new TextView(this);
        privacyText.setText("Privacy: this app only connects to the gateway you pair it with. It does not read SMS or payment notifications. Automatic payment detection is disabled.");
        privacyText.setTextColor(Color.parseColor("#cbd5e1"));
        privacyText.setTextSize(13f);
        privacyCard.addView(privacyText);
        root.addView(privacyCard);

        // Pairing Form Card
        LinearLayout cardPairing = createCard();
        TextView tvPairTitle = new TextView(this);
        tvPairTitle.setText("Pair with Gateway");
        tvPairTitle.setTextColor(Color.WHITE);
        tvPairTitle.setTextSize(16f);
        tvPairTitle.setTypeface(null, Typeface.BOLD);
        cardPairing.addView(tvPairTitle);

        TextView tvPairDesc = new TextView(this);
        tvPairDesc.setText("Point camera at the Pairing QR code in the Merchant Admin panel to automatically link this device. Zero manual typing required.");
        tvPairDesc.setTextColor(Color.parseColor("#94a3b8"));
        tvPairDesc.setTextSize(13f);
        tvPairDesc.setPadding(0, 4, 0, 16);
        cardPairing.addView(tvPairDesc);

        // Primary: SCAN PAIRING QR
        btnScanQr = createPrimaryButton("📷 SCAN PAIRING QR");
        btnScanQr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startQrScanner();
            }
        });
        cardPairing.addView(btnScanQr);

        // Optional / Manual fallback controls
        TextView lblManual = createLabel("Manual Pairing Credentials (Optional):");
        lblManual.setPadding(0, 16, 0, 4);
        cardPairing.addView(lblManual);

        etBaseUrl = createEditText(apiClient.getBaseUrl(), "http://your-server-ip:6100");
        cardPairing.addView(etBaseUrl);

        etEnrollmentToken = createEditText("", "Enrollment Secret Token");
        cardPairing.addView(etEnrollmentToken);

        etDeviceName = createEditText(Build.MODEL, "Custom Device Name");
        cardPairing.addView(etDeviceName);

        btnPairDevice = createAccentButton("Manual Connect");
        btnPairDevice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performPairing();
            }
        });
        cardPairing.addView(btnPairDevice);

        btnUnpairDevice = createDangerButton("Unpair & Reset Device");
        btnUnpairDevice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                apiClient.unpair();
                Toast.makeText(MainActivity.this, "Device unpaired.", Toast.LENGTH_SHORT).show();
                refreshUI();
            }
        });
        cardPairing.addView(btnUnpairDevice);

        root.addView(cardPairing);

        scrollView.addView(root);
        setContentView(scrollView);

    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshUI();
    }

    private void refreshUI() {
        boolean paired = apiClient.isPaired();
        if (paired) {
            tvStatusBadge.setText("● STATUS: PAIRED & ACTIVE");
            tvStatusBadge.setTextColor(Color.parseColor("#10b981"));
            tvDeviceDetails.setText("Device ID: " + apiClient.getDeviceId() + "\nGateway: " + apiClient.getBaseUrl() + "\nModel: " + Build.MANUFACTURER + " " + Build.MODEL);
            if (btnScanQr != null) btnScanQr.setText("🔄 RE-SCAN PAIRING QR");
            btnPairDevice.setText("Update Configuration");
            btnUnpairDevice.setVisibility(View.VISIBLE);
        } else {
            tvStatusBadge.setText("○ STATUS: UNPAIRED (Ready to scan)");
            tvStatusBadge.setTextColor(Color.parseColor("#f59e0b"));
            tvDeviceDetails.setText("Tap [SCAN PAIRING QR] below to link with Merchant Admin.\nNo manual typing required.");
            if (btnScanQr != null) btnScanQr.setText("📷 SCAN PAIRING QR");
            btnPairDevice.setText("Manual Connect");
            btnUnpairDevice.setVisibility(View.GONE);
        }

    }

    private void startQrScanner() {
        if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.CAMERA}, REQ_SCAN_QR);
            return;
        }

        Intent intent = new Intent(this, QrScannerActivity.class);
        startActivityForResult(intent, REQ_SCAN_QR);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_SCAN_QR && resultCode == RESULT_OK && data != null) {
            String qrPayload = data.getStringExtra(QrScannerActivity.EXTRA_QR_PAYLOAD);
            handleScannedQrPayload(qrPayload);
        }
    }

    private void handleScannedQrPayload(String rawPayload) {
        if (rawPayload == null || rawPayload.trim().isEmpty()) {
            Toast.makeText(this, "INVALID YOUPI PAIRING QR", Toast.LENGTH_LONG).show();
            return;
        }

        try {
            org.json.JSONObject json = new org.json.JSONObject(rawPayload.trim());

            String type = json.optString("type", "");
            if (!"YOUPI_SMS_GATEWAY_PAIR".equals(type)) {
                Toast.makeText(this, "INVALID YOUPI PAIRING QR", Toast.LENGTH_LONG).show();
                return;
            }

            String version = json.optString("version", "v1");
            if (!"v1".equals(version)) {
                Toast.makeText(this, "INVALID YOUPI PAIRING QR", Toast.LENGTH_LONG).show();
                return;
            }

            long expiresAt = json.optLong("expiresAt", 0);
            if (expiresAt > 0 && System.currentTimeMillis() > expiresAt) {
                Toast.makeText(this, "PAIRING QR EXPIRED", Toast.LENGTH_LONG).show();
                return;
            }

            final String serverUrl = json.optString("serverUrl", json.optString("gatewayUrl", json.optString("apiBaseUrl", "")));
            if (!serverUrl.startsWith("http://") && !serverUrl.startsWith("https://")) {
                Toast.makeText(this, "INVALID YOUPI PAIRING QR", Toast.LENGTH_LONG).show();
                return;
            }

            final String token = json.optString("enrollmentToken", json.optString("token", ""));
            if (token.isEmpty()) {
                Toast.makeText(this, "INVALID YOUPI PAIRING QR", Toast.LENGTH_LONG).show();
                return;
            }

            final String merchantId = json.optString("merchantId", "00000000-0000-0000-0000-000000000001");
            final String deviceName = Build.MODEL;

            if (etBaseUrl != null) etBaseUrl.setText(serverUrl);
            if (etEnrollmentToken != null) etEnrollmentToken.setText(token);

            Toast.makeText(this, "Connecting to " + serverUrl + "...", Toast.LENGTH_SHORT).show();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    final boolean success = apiClient.pair(serverUrl, token, deviceName, merchantId, version, expiresAt);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (success) {
                                Toast.makeText(MainActivity.this, "Gateway paired successfully.", Toast.LENGTH_LONG).show();
                                refreshUI();
                            } else {
                                Toast.makeText(MainActivity.this, "Pairing Failed. Check network connection.", Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                }
            }).start();

        } catch (Exception e) {
            Log.e("MainActivity", "Error parsing QR: " + e.getMessage());
            Toast.makeText(this, "INVALID YOUPI PAIRING QR", Toast.LENGTH_LONG).show();
        }
    }

    private void performPairing() {
        final String baseUrl = etBaseUrl.getText().toString().trim();
        final String token = etEnrollmentToken.getText().toString().trim();
        final String deviceName = etDeviceName.getText().toString().trim();

        if (baseUrl.isEmpty() || token.isEmpty()) {
            Toast.makeText(this, "Please provide Gateway URL and Secret Token", Toast.LENGTH_SHORT).show();
            return;
        }

        btnPairDevice.setEnabled(false);
        btnPairDevice.setText("Pairing with Gateway...");

        new Thread(new Runnable() {
            @Override
            public void run() {
                final boolean success = apiClient.pair(baseUrl, token, deviceName);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        btnPairDevice.setEnabled(true);
                        if (success) {
                            Toast.makeText(MainActivity.this, "Device Paired Successfully!", Toast.LENGTH_LONG).show();
                            refreshUI();
                        } else {
                            btnPairDevice.setText("Manual Connect");
                            Toast.makeText(MainActivity.this, "Pairing Failed. Check URL and Enrollment Token.", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        }).start();
    }

    // UI Helper factories
    private LinearLayout createCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(32, 32, 32, 32);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#131b2e"));
        bg.setCornerRadius(24f);
        bg.setStroke(2, Color.parseColor("#1e293b"));
        card.setBackground(bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 32);
        card.setLayoutParams(lp);
        return card;
    }

    private TextView createCheckItem(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.parseColor("#cbd5e1"));
        tv.setTextSize(13f);
        tv.setPadding(0, 8, 0, 8);
        return tv;
    }

    private TextView createLabel(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.parseColor("#94a3b8"));
        tv.setTextSize(12f);
        tv.setPadding(0, 12, 0, 6);
        return tv;
    }

    private EditText createEditText(String text, String hint) {
        EditText et = new EditText(this);
        et.setText(text);
        et.setHint(hint);
        et.setHintTextColor(Color.parseColor("#64748b"));
        et.setTextColor(Color.WHITE);
        et.setTextSize(14f);
        et.setPadding(24, 20, 24, 20);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#0f172a"));
        bg.setCornerRadius(16f);
        bg.setStroke(2, Color.parseColor("#334155"));
        et.setBackground(bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 16);
        et.setLayoutParams(lp);
        return et;
    }

    private Button createPrimaryButton(String text) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(14f);
        btn.setTypeface(null, Typeface.BOLD);
        btn.setPadding(32, 24, 32, 24);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#2563eb"));
        bg.setCornerRadius(16f);
        btn.setBackground(bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 16, 0, 12);
        btn.setLayoutParams(lp);
        return btn;
    }

    private Button createAccentButton(String text) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(Color.parseColor("#38bdf8"));
        btn.setTextSize(12f);
        btn.setPadding(24, 16, 24, 16);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#1e293b"));
        bg.setCornerRadius(14f);
        bg.setStroke(1, Color.parseColor("#0284c7"));
        btn.setBackground(bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 8, 0, 12);
        btn.setLayoutParams(lp);
        return btn;
    }

    private Button createDangerButton(String text) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(Color.parseColor("#f87171"));
        btn.setTextSize(13f);
        btn.setPadding(24, 16, 24, 16);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#450a0a"));
        bg.setCornerRadius(14f);
        bg.setStroke(1, Color.parseColor("#dc2626"));
        btn.setBackground(bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 8, 0, 8);
        btn.setLayoutParams(lp);
        return btn;
    }
}
