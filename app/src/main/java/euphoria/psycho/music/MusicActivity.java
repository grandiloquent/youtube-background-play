package euphoria.psycho.music;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Formatter;

import euphoria.psycho.music.MusicService.LocalBinder;
import euphoria.psycho.music.TimeBar.OnScrubListener;

import static euphoria.psycho.music.DefaultTimeBar.getStringForTime;

public class MusicActivity extends Activity {
    LocalBinder mServiceStub;
    private StringBuilder mStringBuilder;
    private Formatter mFormatter;
    private ProgressBar mProgressBar;
    private LinearLayout mController;
    private TextView mExoPosition;
    private TextView mExoDuration;
    private DefaultTimeBar mExoProgress;
    ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mServiceStub = (LocalBinder) service;
            mExoProgress.setDuration(mServiceStub.duration());
            mExoPosition.setText(getStringForTime(mStringBuilder, mFormatter, mServiceStub.position()));
            mExoDuration.setText(getStringForTime(mStringBuilder, mFormatter, mServiceStub.duration()));
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mServiceStub = null;
        }
    };

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
        mStringBuilder = new StringBuilder();
        mFormatter = new Formatter();
        mExoProgress.addListener(new OnScrubListener() {
            @Override
            public void onScrubMove(TimeBar timeBar, long position) {
                mServiceStub.seekTo((int) position);
            }

            @Override
            public void onScrubStart(TimeBar timeBar, long position) {
            }

            @Override
            public void onScrubStop(TimeBar timeBar, long position, boolean canceled) {
                mServiceStub.seekTo((int) position);
            }
        });
    }

    @Override
    protected void onDestroy() {
        unbindService(mServiceConnection);
        super.onDestroy();
    }
}
