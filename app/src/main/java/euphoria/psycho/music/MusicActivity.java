package euphoria.psycho.music;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

import euphoria.psycho.music.MusicService.LocalBinder;

public class MusicActivity extends Activity {
    LocalBinder mServiceStub;
    ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mServiceStub = (LocalBinder) service;
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
    }

    @Override
    protected void onDestroy() {
        unbindService(mServiceConnection);
        super.onDestroy();
    }
}
