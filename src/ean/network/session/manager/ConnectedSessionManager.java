package ean.network.session.manager;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import ean.network.session.PacketSession;
import ean.network.session.extended.ConnectedSession;

public class ConnectedSessionManager {

	private ArrayList<ConnectedSession> connectedSessionList;
	private int maxUserLimit;
	private int currentUserCount;
	private int maxCount;

	public ConnectedSessionManager() {
		maxUserLimit = 0;
		currentUserCount = 0;
	}
	
	public int getMaxCount() {return maxCount;}
	public void setMaxCount(int maxCount) {this.maxCount = maxCount;}
	public int getMaxUserLimit() {return maxUserLimit;}
	public synchronized int getCurrentUserCount() {return connectedSessionList.size();}

	public synchronized boolean begin(int maxUserLimit) {
		if (maxUserLimit < 0) return false;
		this.maxUserLimit = maxUserLimit;
		connectedSessionList = new ArrayList<ConnectedSession>(this.maxUserLimit);
		return true;
	}

	public synchronized boolean end() {
		connectedSessionList.clear();
		return true;
	}

	public boolean canRegistable() {
		if (currentUserCount >= maxUserLimit) return false;
		return true;
	
	}

	public synchronized boolean addSession(ConnectedSession session) {
		if (!canRegistable())
			return false;
		connectedSessionList.add(session);
		currentUserCount++;
		maxCount++;
		return true;
	}

	public synchronized boolean removeSession(PacketSession session) {
		connectedSessionList.remove(session);
		currentUserCount--;
		if (currentUserCount < 0)
			currentUserCount = 0;
		return true;
	}

	public synchronized boolean writeAll(int protocol, ByteBuffer packet) throws Exception {
		for (int i = 0; i < connectedSessionList.size(); i++) {
			PacketSession session = connectedSessionList.get(i);
			if (!session.writePacket(protocol, packet)) {
				session.end();
				connectedSessionList.remove(session);
				i--;
			}
			packet.rewind();
		}
		return true;
	}

	// 프로토콜을 구성하지 않고 그냥 버퍼를 송신( 문자열 전송등에 사용)
	public synchronized boolean writeRowPacketToAll(ByteBuffer packet)  {
		for (int i = 0; i < connectedSessionList.size(); i++) {
			ConnectedSession session = connectedSessionList.get(i);
			if (!session.writeRawPacket(packet, false)) {
				System.out.println("====================이상 작동 발생============================");
				session.end();
				connectedSessionList.remove(session);
				i--;
			}
			packet.rewind();
		}
		return true;
	}

	public synchronized boolean writePacketTo(PacketSession session,
			int protocol, ByteBuffer packet) throws Exception {
		if (!session.writePacket(protocol, packet)) {
			connectedSessionList.remove(session);
			return false;
		}
		return true;
	}

}
