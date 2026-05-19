package de.daniel.hearstream;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.IBinder;

import java.util.concurrent.atomic.AtomicBoolean;

public class MicStreamService extends Service {
    public static final String ACTION_START = "de.daniel.hearstream.START";
    public static final String ACTION_STOP = "de.daniel.hearstream.STOP";

    private static final String CHANNEL_ID = "hearstream_mic";
    private static final int NOTIFICATION_ID = 42;
    private static final int SAMPLE_RATE = 16000;

    private Thread workerThread;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : ACTION_START;
        if (ACTION_STOP.equals(action)) {
            stopStreaming();
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }

        startForeground(NOTIFICATION_ID, buildNotification());
        startStreaming();
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        stopStreaming();
        super.onDestroy();
    }

    private void startStreaming() {
        if (running.getAndSet(true)) {
            return;
        }

        workerThread = new Thread(this::audioLoop, "HearStream-AudioLoop");
        workerThread.start();
    }

    private void stopStreaming() {
        running.set(false);
        if (workerThread != null) {
            workerThread.interrupt();
            workerThread = null;
        }
    }

    private void audioLoop() {
        int channelIn = AudioFormat.CHANNEL_IN_MONO;
        int channelOut = AudioFormat.CHANNEL_OUT_MONO;
        int encoding = AudioFormat.ENCODING_PCM_16BIT;
        int minRecord = AudioRecord.getMinBufferSize(SAMPLE_RATE, channelIn, encoding);
        int minPlay = AudioTrack.getMinBufferSize(SAMPLE_RATE, channelOut, encoding);
        int bufferSize = Math.max(Math.max(minRecord, minPlay), SAMPLE_RATE / 2);

        AudioRecord record = null;
        AudioTrack track = null;

        try {
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            audioManager.setMode(AudioManager.MODE_NORMAL);
            audioManager.setSpeakerphoneOn(false);

            record = new AudioRecord(
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    SAMPLE_RATE,
                    channelIn,
                    encoding,
                    bufferSize
            );

            AudioAttributes attributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build();

            AudioFormat format = new AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(encoding)
                    .setChannelMask(channelOut)
                    .build();

            track = new AudioTrack.Builder()
                    .setAudioAttributes(attributes)
                    .setAudioFormat(format)
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build();

            byte[] buffer = new byte[bufferSize];
            record.startRecording();
            track.play();

            while (running.get() && !Thread.currentThread().isInterrupted()) {
                int read = record.read(buffer, 0, buffer.length);
                if (read > 0) {
                    track.write(buffer, 0, read);
                }
            }
        } catch (SecurityException ignored) {
            running.set(false);
        } finally {
            if (record != null) {
                try { record.stop(); } catch (Exception ignored) {}
                record.release();
            }
            if (track != null) {
                try { track.stop(); } catch (Exception ignored) {}
                track.release();
            }
        }
    }

    private Notification buildNotification() {
        Intent stopIntent = new Intent(this, MicStreamService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this,
                2,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent openIntent = new Intent(this, MainActivity.class);
        PendingIntent openPendingIntent = PendingIntent.getActivity(
                this,
                1,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);

        return builder
                .setContentTitle("HearStream läuft")
                .setContentText("Mikrofon wird live auf Bluetooth/Medienausgabe gestreamt")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentIntent(openPendingIntent)
                .addAction(android.R.drawable.ic_media_pause, "Stop", stopPendingIntent)
                .setOngoing(true)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "HearStream Mikrofon",
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription("Benachrichtigung für laufendes Mikrofon-Streaming");
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(channel);
    }
}
