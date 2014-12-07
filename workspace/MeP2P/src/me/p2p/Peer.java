package me.p2p;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

import me.p2p.constant.PeerPort;
import me.p2p.data.DataManager;
import me.p2p.log.Log;
import me.p2p.message.EMsgType;
import me.p2p.message.Message;
import me.p2p.message.MessageDataParser;
import me.p2p.message.MessageParser;
import me.p2p.request.Request;
import me.p2p.request.RequestHandler;
import me.p2p.spec.IP2PProtocol;
import me.p2p.spec.IPeer;
import me.p2p.spec.MessageCallback;

import org.json.JSONObject;

public class Peer extends Thread implements IPeer, MessageCallback {
	final String TAG = "Peer";

	// msg handler to handle message;
	ServerSocket peerServerSocket;

	/**
	 * Đối tượng xử lý request từ boostrap sau khi thông điệp msg_join<br>
	 * được gửi đi.
	 */
	RequestHandler bootstrapRequestHandler;

	// socket to request to bootstrap;
	Socket bootstrapSocket;
	Request requestBootstrap;

	InetAddress localAddress;
	String bstrAddress;

	/**
	 * Chứa thông tin của một peer. Bao gồm:<br>
	 * - Thông tin về địa chỉ ip của peer.<br>
	 * - Thông tin về username đăng ký.
	 */
	PeerInfo peerInfo;

	/**
	 * Biến dùng để set tình trạng cho Peer: có tiếp tục lắng nghe request<br>
	 * từ những peer khác hay không?
	 */
	boolean shutdown = false;

	/**
	 * Đối tượng quản lý data của peer node;
	 */
	DataManager dataManager;

	/**
	 * Boolean để kiểm tra thử peer có init hay chưa?
	 */
	boolean hasInit = false;

	public Peer(String fileListPeerPath, String userName,
			InetAddress localAdress, String bootstrapAddress) {
		// khởi tạo inetadress;
		if (localAdress != null) {
			/**
			 * Khởi tạo lại local Address bằng cách truyền lại giá trị
			 * localAdress. Trên máy java thông thường thì sử dụng được nhưng
			 * trên thiết bị Android thì ko sử dụng được, nên phải lấy ip theo
			 * cách khác và khởi tạo theo cách này;
			 */
			this.localAddress = localAdress;
		} else {
			try {
				this.localAddress = InetAddress.getLocalHost();
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		// khởi tạo thông tin về peerInfo;
		peerInfo = new PeerInfo(this.localAddress.getHostAddress(), userName);

		// khởi tạo serverSocket cho việc lắng nghe update thông tin từ những
		// nút khác
		try {
			peerServerSocket = new ServerSocket(PeerPort.PORT_PEER,
					IP2PProtocol.BACK_LOG, localAddress);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// khởi tạo socket bootstrap và request cho để message cho nó;
		try {
			this.bstrAddress = bootstrapAddress;
			bootstrapSocket = new Socket(this.bstrAddress.toString(),
					PeerPort.PORT_BOOTSTRAP);
			requestBootstrap = new Request(bootstrapSocket);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// khởi tạo đối tượng quản lý dữ liệu;
		DataManager.prepare(fileListPeerPath);
		dataManager = DataManager.getInstance();
	}

	public Peer(String fileListPeerPath, String userName,
			String bootstrapAddress) {
		// TODO Auto-generated constructor stub
		this(fileListPeerPath, userName, null, bootstrapAddress);
	}

	public InetAddress getAddress() {
		return localAddress;
	}

	@Override
	public void joinRequest() {
		// TODO Auto-generated method stub
		// start msg;
		requestBootstrap.startMsg();

		// send message join with peer info;
		Message message = new Message(EMsgType.JOIN, peerInfo.toJSONObject());
		requestBootstrap.sendMessage(message);

		// send end msg;
		requestBootstrap.endMsg();

		/**
		 * Sau khi send msg join thì peer chờ boostrap node xử lý join msg sau
		 * đó đợi nhận list peer ở port 8686 đã được peer mở ra và lắng nghe từ
		 * đầu;
		 */
	}

	@Override
	public void leaveRequest() {
		// TODO Auto-generated method stub
	}

	@Override
	public void updateRequest() {
		// TODO Auto-generated method stub

	}

	/**
	 * Implement as Server, always listen change from other node; Chỉ có một
	 * handler để lắng nghe request, vì chương trình chỉ đang trong quá trình
	 * xây dựng nên điều này sẽ làm cho dễ quản lý chương trình. Nếu trường hợp
	 * có nhiều thread thì socket sẽ được đồng bộ hóa, những thread khác sẽ phải
	 * chờ cho đến khi thread xử lý xong.
	 */
	@Override
	public void run() {
		// TODO Auto-generated method stub
		while (!shutdown) {
			// listen;
			try {
				Socket localSocket = peerServerSocket.accept();
				synchronized (localSocket) {
					bootstrapRequestHandler = new RequestHandler(localSocket,
							this);
					bootstrapRequestHandler.handleRequest();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		bootstrapRequestHandler.stopHandle();
		Log.logToConsole(TAG, "Stop handle request from bootstrap");
	}

	@Override
	public void shutdown() {
		shutdown = true;
	}

	@SuppressWarnings("incomplete-switch")
	@Override
	public void onMessage(JSONObject message) {
		// TODO Auto-generated method stub
		Log.logToConsole(TAG, "onMessage(): bootstrap send start message");
		/**
		 * Hàm này được gọi khi bootstrap node gửi dữ liệu list data;
		 */
		MessageParser msgParser = new MessageParser(message);
		switch (msgParser.getMessageType()) {
		case TRANSFER_LIST: {
			handleTransferRequest(msgParser.getMessageData());
		}
			break;
		}
	}

	@Override
	public void handleTransferRequest(JSONObject bstrListPeerInfo) {
		// TODO Auto-generated method stub
		Log.logToConsole(TAG, "handleTransferRequest()");

		MessageDataParser msgDataParser = new MessageDataParser(
				bstrListPeerInfo);
		ArrayList<PeerInfo> listPeerInfo = msgDataParser.getListPeerInfo();

		for (PeerInfo pi : listPeerInfo) {
			dataManager.add(pi);
		}

		// thay đổi status của peer;
		dataManager.joined();

		// tải danh sách xong rồi thì close socket;
		try {
			bootstrapSocket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void listenRequest() {
		// TODO Auto-generated method stub
		Log.logToConsole(TAG, "listenRequest()");
		start();
	}

	@Override
	public void handleLeaveMsg(JSONObject data, Socket peerSocket) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleUpdateMsg(JSONObject data) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onMessageStart() {
		// TODO Auto-generated method stub
		Log.logToConsole(TAG, "onSessionStart(): bootstrap send start message");
	}

	@Override
	public void onMessageEnd() {
		// TODO Auto-generated method stub
		Log.logToConsole(TAG, "onSessionEnd(): bootstrap send end session");
		/**
		 * Nhận được lệnh kết thúc phiên từ bootstrap node, dừng xử lý request
		 * từ server và bắt đầu tự vận động
		 */
		bootstrapRequestHandler.stopHandle();
	}
}
