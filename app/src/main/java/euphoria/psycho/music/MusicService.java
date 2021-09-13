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
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;


public class MusicService extends Service implements OnPreparedListener, OnBufferingUpdateListener {
    public static final String CHANNEL_ID = "YT_channel_01";
    MediaPlayer mMediaPlayer;
    NotificationManager mNotificationManager;
    String[] mMusic;
    IBinder mBinder = new LocalBinder(this);
    int mBufferedPosition;
    private WakeLock mWakeLock;

    public int bufferedPosition() {
        return mBufferedPosition;
    }

    public long duration() {
        return mMediaPlayer.getDuration();
    }

    public long position() {
        return mMediaPlayer.getCurrentPosition();
    }

    public void seekTo(int mes) {
        mMediaPlayer.seekTo(mes);
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

    private static final String BOOKMARK_CACHE_FILE = "bookmark";
    private static final int BOOKMARK_CACHE_MAX_ENTRIES = 100;
    private static final int BOOKMARK_CACHE_MAX_BYTES = 10 * 1024;
    private static final int BOOKMARK_CACHE_VERSION = 1;
    private static final int HALF_MINUTE = 30 * 1000;
    private static final int TWO_MINUTES = 4 * HALF_MINUTE;

    public Integer getBookmark(String path) {
        try {
            BlobCache cache = CacheManager.getCache(this,
                    BOOKMARK_CACHE_FILE, BOOKMARK_CACHE_MAX_ENTRIES,
                    BOOKMARK_CACHE_MAX_BYTES, BOOKMARK_CACHE_VERSION);
            byte[] data = cache.lookup(path.hashCode());
            if (data == null) return 0;
            DataInputStream dis = new DataInputStream(
                    new ByteArrayInputStream(data));
            String uriString = DataInputStream.readUTF(dis);
            int bookmark = dis.readInt();
            if (!uriString.equals(path)) {
                return 0;
            }
            return bookmark;
        } catch (Throwable t) {
        }
        return null;
    }

    public void setBookmark(String path, int bookmark/*, int duration*/) {
        try {
            BlobCache cache = CacheManager.getCache(this,
                    BOOKMARK_CACHE_FILE, BOOKMARK_CACHE_MAX_ENTRIES,
                    BOOKMARK_CACHE_MAX_BYTES, BOOKMARK_CACHE_VERSION);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);
            dos.writeUTF(path);
            dos.writeInt(bookmark);
            //dos.writeInt(duration);
            dos.flush();
            cache.insert(path.hashCode(), bos.toByteArray());
        } catch (Throwable t) {
        }
    }

    private void showNotification(Builder builder) {
        mNotificationManager.notify(hashCode(), builder.build());
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        mBufferedPosition = percent * mp.getDuration() / 100;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        final PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
        mWakeLock.setReferenceCounted(false);
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnBufferingUpdateListener(this);
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel;
            channel = new NotificationChannel(CHANNEL_ID, "YT", NotificationManager.IMPORTANCE_LOW);
            mNotificationManager.createNotificationChannel(channel);
        }
        startForeground(hashCode(), createNotification()
                .setContentTitle("准备播放音乐")
                .build());
    }

    @Override
    public void onDestroy() {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        super.onDestroy();
        mWakeLock.release();
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mMediaPlayer.start();
        int value = getBookmark(mMusic[0]);
        if (value > 0) {
            mMediaPlayer.seekTo(value);
        }
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
            if (mMusic != null && mMediaPlayer != null) {
                setBookmark(mMusic[0], mMediaPlayer.getCurrentPosition());
            }
            mMediaPlayer.reset();
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

        public int bufferedPosition() {
            return mService.get().bufferedPosition();
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
    }
}
