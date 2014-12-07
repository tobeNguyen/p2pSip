package me.p2p.peermain;

import me.p2p.Peer;



public class PeerDesktopMain {
	static final String TAG = "PeerMain";
	static final String filePath = "E:/PeerData";
	
	public static void main(String[] args) {
		String bootstrapAdress = "192.168.1.51";
		
		Peer peer = new Peer(filePath, "tobeNguyen", bootstrapAdress);
		// listen for request;
		peer.listenRequest();
		// send join request
		peer.joinRequest();
	}
}
