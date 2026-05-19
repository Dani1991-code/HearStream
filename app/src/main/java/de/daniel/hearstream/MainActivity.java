package de.daniel.hearstream;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;

public class MainActivity extends Activity {
    private TextView statusText;
    private Button startButton;
    private Button stopButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
        requestNeededPermissions();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(36, 64, 36, 36);

        TextView title = new TextView(this);
        title.setText("HearStream");
        title.setTextSize(30);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(-1, -2));

        statusText = new TextView(this);
        statusText.setText("Bereit. Koppel deine Hörgeräte als Bluetooth-Medienausgabe und drücke Start.");
        statusText.setTextSize(17);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(0, 28, 0, 28);
        root.addView(statusText, new LinearLayout.LayoutParams(-1, -2));

        startButton = new Button(this);
        startButton.setText("START – Mikrofon streamen");
        startButton.setTextSize(18);
        startButton.setOnClickListener(v -> startStreaming());
        root.addView(startButton, new LinearLayout.LayoutParams(-1, -2));

        stopButton = new Button(this);
        stopButton.setText("STOP");
        stopButton.setTextSize(18);
        stopButton.setOnClickListener(v -> stopStreaming());
        LinearLayout.LayoutParams stopParams = new LinearLayout.LayoutParams(-1, -2);
        stopParams.setMargins(0, 20, 0, 0);
        root.addView(stopButton, stopParams);

        TextView hint = new TextView(this);
        hint.setText("Hinweis: Android zeigt währenddessen eine Benachrichtigung an. Das ist Absicht, damit Mikrofonzugriff bei ausgeschaltetem Bildschirm erlaubt bleibt.");
        hint.setTextSize(14);
        hint.setGravity(Gravity.CENTER);
        hint.setPadding(0, 28, 0, 0);
        root.addView(hint, new LinearLayout.LayoutParams(-1, -2));

        setContentView(root);
    }

    private void startStreaming() {
        if (!hasAllRuntimePermissions()) {
            requestNeededPermissions();
            statusText.setText("Bitte Mikrofon- und Benachrichtigungsrechte erlauben.");
            return;
        }

        Intent intent = new Intent(this, MicStreamService.class);
        intent.setAction(MicStreamService.ACTION_START);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        statusText.setText("Streaming läuft. Handy-Mikrofon wird auf die aktuelle Medienausgabe gesendet.");
    }

    private void stopStreaming() {
        Intent intent = new Intent(this, MicStreamService.class);
        intent.setAction(MicStreamService.ACTION_STOP);
        startService(intent);
        statusText.setText("Streaming gestoppt.");
    }

    private boolean hasAllRuntimePermissions() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return true;
    }

    private void requestNeededPermissions() {
        if (Build.VERSION.SDK_INT < 23) {
            return;
        }

        List<String> permissions = new ArrayList<>();
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECORD_AUDIO);
        }
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        if (Build.VERSION.SDK_INT >= 31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
        }
        if (!permissions.isEmpty()) {
            requestPermissions(permissions.toArray(new String[0]), 1001);
        }
    }
}
