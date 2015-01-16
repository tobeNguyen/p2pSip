package android.me.p2psip.activity;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;

import local.ua.UserAgent;
import me.sip.ua.specify.UACListener;
import net.majorkernelpanic.streaming.Session;
import net.majorkernelpanic.streaming.SessionBuilder;
import net.majorkernelpanic.streaming.rtsp.RtspServer;
import net.majorkernelpanic.streaming.rtsp.RtspServer.CallbackListener;

import org.zoolu.sip.address.NameAddress;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.me.p2psip.R;
import android.me.p2psip.constant.Constant;
import android.me.p2psip.log.LogAndroid;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.VideoView;

public class OnCallActivity extends SipActivity implements UACListener,
		CallbackListener {
	final String TAG = "OnCallActivity";
	/**
	 * Surface View được dùng cho hiển thị Video Streaming;
	 */
	VideoView mVideoView1;

	/**
	 * Surface View được dùng cho hiển thị hình ảnh Camera;
	 */
	SurfaceView mSurfaceViewPreview;
	SurfaceHolder mSurfaceHolderPr;

	ImageButton mBtEndCall;

	/**
	 * Phiên streaming;
	 */
	Session mSession;

	private RtspServer mRtspServer;

	String mRtspServerAddress = null;
	String mRtspServerStreamingLink = null;

	private ServiceConnection mRtspServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			LogAndroid.log(TAG, "Connected to RtspService");

			/**
			 * Khởi tạo Rtsp Server bên này để cho máy bên kia có thể streaming
			 */
			mRtspServer = (RtspServer) ((RtspServer.LocalBinder) service)
					.getService();
			mRtspServer.addCallbackListener(OnCallActivity.this);
			mRtspServer.start();

			/**
			 * Bắt đầu thực thi streaming video từ Rtsp Server;
			 */
			startPlayStreaming();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {

		}

	};

	Timer mRtspTimer = null;

	/**
	 * Hàm khởi tạo Activity;
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);

		/*
		 * Cài đặt layout cho Activity;
		 */
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.activity_on_call);

		// init all views;
		initViews();

		// init preview;
		initStreamingPreview();

		startService(new Intent(getApplicationContext(), RtspServer.class));
	}

	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();

		/**
		 * Kết nối đến Rtsp Service;
		 */
		// bind rtsp service;
		Intent intent = new Intent(this, RtspServer.class);
		bindService(intent, mRtspServiceConnection, Context.BIND_AUTO_CREATE);
	}

	/**
	 * Hàm này được gọi sau khi SipService được kết nối
	 */
	@Override
	public void onSipServiceConnected(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		// init components;
		initComponents();
	}

	private void initViews() {
		LogAndroid.log(TAG, "initViews(): init all views");

		/**
		 * Init preview component;
		 */
		mSurfaceViewPreview = (SurfaceView) findViewById(R.id.onCall_SurfaceView_Preview);
		mSurfaceHolderPr = mSurfaceViewPreview.getHolder();
		mSurfaceHolderPr.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		/**
		 * Init streaming component;
		 */
		mVideoView1 = (VideoView) findViewById(R.id.onCall_SurfaceView1);

		/**
		 * Init other component;
		 */
		mBtEndCall = (ImageButton) findViewById(R.id.onCall_btEnCall);
		mBtEndCall.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				/*
				 * Kết thúc cuộc gọi
				 */
				mSipService.hangup();

				/*
				 * Tiếp tục lắng nghe cuộc gọi
				 */
				mSipService.listen();

				/*
				 * Kết thúc Activity
				 */
				finish();
			}
		});
	}

	private void initStreamingPreview() {
		LogAndroid.log(TAG, "initPreview(): init preview surfaceview");

		// Configures the SessionBuilder
		SessionBuilder.getInstance().setSurfaceHolder(mSurfaceHolderPr);

		try {
			mSession = SessionBuilder.getInstance().build();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			Log.d(TAG, "initStreamingPreview(): mSurfaceHolderPr null");
			e.printStackTrace();
		}
	}

	/**
	 * Hàm khởi tạo các thành phần quan trọng
	 */
	private void initComponents() {
		mSipService.setUACListener(this);
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
	}

	@Override
	public void onUACCallIncoming(UserAgent ua, NameAddress caller,
			NameAddress callee) {
		// TODO Auto-generated method stub
		/*
		 * This Activity doesn't catch this event
		 */
	}

	@Override
	public void onCallUASCancelled(UserAgent ua) {
		// TODO Auto-generated method stub
		/*
		 * This Activity doesn't catch this event
		 */
	}

	@Override
	public void onUACCallClosed(UserAgent ua) {
		// TODO Auto-generated method stub
		LogAndroid.log(TAG, "onUACCallClosed()");
		/*
		 * Hàm này được gọi khi kết thúc cuộc gọi;
		 */
		finish();
	}

	/**
	 * Bắt đầu streaming dữ liệu từ Rtsp Server<br>
	 * - Khởi tạo link streaming.<br>
	 * - Kiểm tra mỗi 0.5s mỗi lần xem thử Rtsp Server đã chạy chưa.<br>
	 * - Nếu đã chạy rồi thì thực thi streaming.
	 */
	private void startPlayStreaming() {
		/*
		 * Đóng vai trò là UAC, khi có người gọi gọi tới thì địa chỉ caller sip
		 * sẽ khác null, nếu ngược lại thì là UAS
		 */
		LogAndroid.log(TAG,
				"startPlayStreaming(): start play streaming from rtsp server");

		if (mSipService.getCallerSipUrl() != null) {
			mRtspServerAddress = mSipService.getCallerSipUrl().getHost();
			mRtspServerStreamingLink = "rtsp://"
					+ mSipService.getCallerSipUrl().getHost() + ":"
					+ Integer.toString(Constant.RTSP_PORT);
			LogAndroid.log(TAG, "Device is Callee. Link Caller: "
					+ mRtspServerStreamingLink);
		} else {
			mRtspServerAddress = mSipService.getCallSipUrl().getHost();
			mRtspServerStreamingLink = "rtsp://"
					+ mSipService.getCallSipUrl().getHost() + ":"
					+ Integer.toString(Constant.RTSP_PORT);
			LogAndroid.log(TAG, "Device is Caller. Link Callee: "
					+ mRtspServerStreamingLink);
		}

		LogAndroid
				.log(TAG, "startPlayStreaming(): " + mRtspServerStreamingLink);

		/**
		 * Check for Rstp Server running each 0.5s;
		 */
		mRtspTimer = new Timer();
		mRtspTimer.schedule(new TimerTask() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				try {
					/**
					 * Nếu rtsp đã chạy thì đoạn lệnh tạo socket sẽ được thực
					 * hiện. Nếu không thì đoạn lệnh sẽ chạy đến exception.
					 */
					Socket socket = new Socket(mRtspServerAddress,
							Constant.RTSP_PORT);
					socket.close();

					LogAndroid.log(TAG,
							"RtspServer is running. Start stream video");

					/**
					 * Streaming video;
					 */
					runOnUiThread(new Runnable() {

						@Override
						public void run() {
							// TODO Auto-generated method stub
							playStreaming(mRtspServerStreamingLink);
						}
					});

					/**
					 * Hủy bỏ timer;
					 */
					cancel();
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					LogAndroid.log(TAG, "Rtsp Server is not running");
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					LogAndroid.log(TAG, "Rtsp Server is not running");
					e.printStackTrace();
				}
			}
		}, 0, 500);
	}

	/**
	 * Sử dụng Vitamio để thực hiện live streaming từ Rtsp Server;
	 * 
	 * @param linkRtspStreaming
	 */
	private void playStreaming(String linkRtspStreaming) {
		Uri uri = Uri.parse(linkRtspStreaming);
		mVideoView1.setVideoURI(uri);
		mVideoView1.setOnPreparedListener(new OnPreparedListener() {

			@Override
			public void onPrepared(MediaPlayer mp) {
				// TODO Auto-generated method stub
				Log.e(TAG, "mVideoView1 prepared");
			}
		});
		mVideoView1.start();
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();

		/*
		 * Stop timer if running
		 */
		if (mRtspTimer != null) {
			mRtspTimer.cancel();
		}

		/*
		 * Stop preview streaming;
		 */
		mSession.stop();
		mSession.flush();

		/*
		 * Unbind service connection;
		 */
		unbindService(mRtspServiceConnection);
		stopService(new Intent(this, RtspServer.class));
	}

	@Override
	public void onError(RtspServer server, Exception e, int error) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onMessage(RtspServer server, int message) {
		// TODO Auto-generated method stub

	}
}
