package info.pauek.dontwork;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity
{
    public static int OVERLAY_PERMISSION_REQ_CODE = 1;
    private DontWorkService service;
    private Button startButton;
    private SeekBar barParam1, barParam2;

    public void requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == OVERLAY_PERMISSION_REQ_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(this)) {
                    Log.e("DontWork", "Permission not granted!");
                    Toast.makeText(
                        this,
                        R.string.permission_not_granted,
                        Toast.LENGTH_SHORT
                    ).show();
                    finish();
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestOverlayPermission();

        // Start service
        Intent intent = new Intent(this, DontWorkService.class);
        startService(intent);

        startButton = (Button)findViewById(R.id.button);
        barParam1 = (SeekBar)findViewById(R.id.bar_param_1);
        barParam2 = (SeekBar)findViewById(R.id.bar_param_2);
        barParam1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Log.i("DontWork", String.format("Progress: %d", progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private ServiceConnection connection;

    @Override
    protected void onStart() {
        super.onStart();

        // Bind to service
        connection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                DontWorkService.LocalBinder binder = (DontWorkService.LocalBinder)service;
                MainActivity.this.service = binder.getService();

                if (MainActivity.this.service.isWatching()) {
                    startButton.setText(R.string.stop);
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                MainActivity.this.service = null;
            }
        };

        Intent intent = new Intent(this, DontWorkService.class);
        boolean boundService = bindService(intent, connection, 0);
        if (!boundService) {
            Log.e("DontWork", "Cannot bind to service!");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service
        if (service != null) {
            unbindService(connection);
            service = null;
        }
    }

    public void onStartOrStop(View view) {
        if (!service.isWatching()) {
            service.startWatching();
            startButton.setText(R.string.stop);
            startButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_media_pause, 0, 0, 0);
        } else {
            service.stopWatching();
            startButton.setText(R.string.start);
            startButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_media_play, 0, 0, 0);
        }
    }
}
