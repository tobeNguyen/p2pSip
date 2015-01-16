package android.me.p2psip.application;

import me.p2p.Peer;
import net.majorkernelpanic.streaming.SessionBuilder;
import net.majorkernelpanic.streaming.video.VideoQuality;
import android.app.Application;
import android.hardware.Camera.CameraInfo;

public class MeApplication extends Application {
	final String TAG = "MeApplication";
	private Peer peer;

	/** Default quality of video streams. */
	VideoQuality videoQuality = new VideoQuality(640, 480, 15, 500000);

	/** By default AMR is the audio encoder. */
	int audioEncoder = SessionBuilder.AUDIO_NONE;

	/** By default H.263 is the video encoder. */
	int videoEncoder = SessionBuilder.VIDEO_H264;

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();

		// Configures the SessionBuilder
		audioEncoder = (android.os.Build.VERSION.SDK_INT) < 14 ? SessionBuilder.AUDIO_AMRNB
				: SessionBuilder.AUDIO_AAC;
		SessionBuilder.getInstance().setContext(getApplicationContext())
				.setCamera(CameraInfo.CAMERA_FACING_FRONT)
				.setAudioEncoder(SessionBuilder.AUDIO_NONE).setVideoEncoder(videoEncoder)
				.setVideoQuality(videoQuality);
	}

	public Peer getPeer() {
		return this.peer;
	}

	public void setPeer(Peer peer) {
		this.peer = peer;
	}
}
