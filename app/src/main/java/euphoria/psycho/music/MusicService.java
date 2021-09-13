package euphoria.psycho.music;

import android.app.Notification.Builder;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.io.IOException;
import java.lang.ref.WeakReference;


public class MusicService extends Service implements OnPreparedListener, OnBufferingUpdateListener {
    public static final String CHANNEL_ID = "YT_channel_01";
    MediaPlayer mMediaPlayer;
    NotificationManager mNotificationManager;
    String[] mMusic;
    IBinder mBinder = new LocalBinder(this);
    int mBufferedPosition;

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        mBufferedPosition = percent * mp.getDuration() / 100;
    }

    private Builder createNotification() {
        Builder builder;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            builder = new Builder(this, CHANNEL_ID);
        } else {
            builder = new Builder(this);
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, new Intent(this, MusicActivity
                        .class),
                PendingIntent.FLAG_UPDATE_CURRENT
        );
        builder.setSmallIcon(R.drawable.ic_stat_yt)
                .addAction(R.drawable.ic_action_play_arrow, "", null)
                .setContentIntent(pendingIntent);
        return builder;
    }

    private void showNotification(Builder builder) {
        mNotificationManager.notify(hashCode(), builder.build());
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e("TAG", "onCreate");
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnBufferingUpdateListener(this);
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel;
            channel = new NotificationChannel(CHANNEL_ID, "YT", NotificationManager.IMPORTANCE_LOW);
            mNotificationManager.createNotificationChannel(channel);
        }
        showNotification(createNotification()
                .setContentTitle("准备播放音乐"));
    }

    @Override
    public void onDestroy() {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        super.onDestroy();
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mMediaPlayer.start();
        Builder builder = createNotification();
        builder.setContentTitle(mMusic[0]);
        showNotification(builder);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;
        String[] musicUri = intent.getStringArrayExtra("music");
        if (musicUri == null) {
            return START_NOT_STICKY;
        }
        try {
            mMediaPlayer.setDataSource(this, Uri.parse(musicUri[1]));
            mMusic = musicUri;
        } catch (IOException e) {
            e.printStackTrace();
        }
        mMediaPlayer.prepareAsync();
        return super.onStartCommand(intent, flags, startId);
    }

    public static final class LocalBinder extends Binder {

        private final WeakReference<MusicService> mService;

        private LocalBinder(final MusicService service) {
            mService = new WeakReference<>(service);
        }

        public long duration() {
            return mService.get().duration();
        }

        public long position() {
            return mService.get().position();
        }

        public void seekTo(int mes) {
            mService.get().seekTo(mes);
        }

        public int bufferedPosition() {
            return mService.get().bufferedPosition();
        }
    }

    public void seekTo(int mes) {
        mMediaPlayer.seekTo(mes);
    }

    public long duration() {
        return mMediaPlayer.getDuration();
    }

    public long position() {
        return mMediaPlayer.getCurrentPosition();
    }

    public int bufferedPosition() {
        return mBufferedPosition;
    }
}
