package com.example.multistream;

import java.io.IOException;

import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.MediaPlayer.OnPreparedListener;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

public class Fragment1 extends Fragment implements SurfaceHolder.Callback {
	String TAG = "Fragment1";
	MediaPlayer mediaPlayer;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		mediaPlayer = new MediaPlayer(getActivity());
		mediaPlayer.setOnPreparedListener(new OnPreparedListener() {

			@Override
			public void onPrepared(MediaPlayer mp) {
				// TODO Auto-generated method stub
				Log.e(TAG, "mediaPlayer has prepared");
				mediaPlayer.start();
			}
		});
	}

	@Override
	public View onCreateView(LayoutInflater inflater,
			@Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		View view = inflater.inflate(R.layout.frag_layout, null);
		SurfaceView surfaceView = (SurfaceView) view
				.findViewById(R.id.video_view);
		SurfaceHolder surfaceHolder = surfaceView.getHolder();
		surfaceHolder.addCallback(this);
		surfaceHolder.setFormat(PixelFormat.RGBA_8888);

		return view;
	}

	@Override
	public void surfaceCreated(final SurfaceHolder holder) {
		// TODO Auto-generated method stub
		Log.e(TAG, "surface created");

		// TODO Auto-generated method stub
		mediaPlayer.setDisplay(holder);

		try {
			mediaPlayer.setDataSource(MainActivity.LINK);
			mediaPlayer.prepareAsync();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// TODO Auto-generated method stub

	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub

	}
}
