package me.p2p.bootstrap;



public class BootstrapNodeMain {
	static final String filePath = "E:/";
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		BootstrapNode bootstrapNode = new BootstrapNode(filePath);
		// listen for request
		bootstrapNode.listenRequest();
	}
}
