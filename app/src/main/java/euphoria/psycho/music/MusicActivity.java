package euphoria.psycho.music;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Formatter;

import euphoria.psycho.music.MusicService.LocalBinder;
import euphoria.psycho.music.TimeBar.OnScrubListener;


public class MusicActivity extends Activity {
    LocalBinder mLocalBinder;
    private final StringBuilder mStringBuilder = new StringBuilder();
    private final Formatter mFormatter =new Formatter(mStringBuilder);
    private ProgressBar mProgressBar;
    private LinearLayout mController;
    private TextView mExoPosition;
    private TextView mExoDuration;
    private DefaultTimeBar mExoProgress;
    private Handler mHandler = new Handler();

    ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mLocalBinder = (LocalBinder) service;
            mExoProgress.setDuration(mLocalBinder.duration());
            mExoPosition.setText(getStringForTime(mStringBuilder, mFormatter, mLocalBinder.position()));
            Log.e("TAG",getStringForTime(mStringBuilder, mFormatter, mLocalBinder.duration()));
            mExoDuration.setText(getStringForTime(mStringBuilder, mFormatter, mLocalBinder.duration()));
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mLocalBinder = null;
        }
    };

    public static String getStringForTime(StringBuilder builder, Formatter formatter, long timeMs) {
        if (timeMs == Long.MIN_VALUE + 1) {
            timeMs = 0;
        }
        long totalSeconds = (timeMs + 500) / 1000;
        long seconds = totalSeconds % 60;
        long minutes = (totalSeconds / 60) % 60;
        long hours = totalSeconds / 3600;
        builder.setLength(0);
        Log.e("TAG",String.format("%s %s %s",hours,minutes,seconds));
        return hours > 0 ? formatter.format("%d:%02d:%02d", hours, minutes, seconds).toString()
                : formatter.format("%02d:%02d", minutes, seconds).toString();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music);
        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, mServiceConnection, BIND_AUTO_CREATE);
        mProgressBar = findViewById(R.id.progress_bar);
        mController = findViewById(R.id.controller);
        mExoPosition = findViewById(R.id.exo_position);
        mExoProgress = findViewById(R.id.exo_progress);
        mExoDuration = findViewById(R.id.exo_duration);
        mExoProgress.addListener(new OnScrubListener() {
            @Override
            public void onScrubMove(TimeBar timeBar, long position) {
                mLocalBinder.seekTo((int) position);
            }

            @Override
            public void onScrubStart(TimeBar timeBar, long position) {
            }

            @Override
            public void onScrubStop(TimeBar timeBar, long position, boolean canceled) {
                mLocalBinder.seekTo((int) position);
            }
        });
        mHandler.post(() -> updateProgress());
    }

    private void updateProgress() {
        if (mLocalBinder != null) {
            mExoPosition.setText(getStringForTime(mStringBuilder, mFormatter, mLocalBinder.position()));
            mExoProgress.setBufferedPosition(mLocalBinder.bufferedPosition());
        }
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateProgress();
            }
        }, 1000);
    }

    @Override
    protected void onDestroy() {
        unbindService(mServiceConnection);
        super.onDestroy();
    }
}
