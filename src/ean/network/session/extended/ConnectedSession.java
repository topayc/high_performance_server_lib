package ean.network.session.extended;

import ean.network.session.PacketSession;

enum SessionStatus{
	SESSEION_NONE,
	SESSION_BEFORE_AUTHENTICATION,
	SESSION_AUTHENTICATIONING,
	SESSION_AUTHENTICATED,
}

public class ConnectedSession extends PacketSession {
	private int sessinId;
	private String userId;
	private String userName;
	
	private String virtualAddress;
	private String realAddress;
	
	private int realPort;
	private int virtualPort;
	
	private SessionStatus sessionStatus;
	private boolean isConnected;
	
	public ConnectedSession(){
		this.sessionStatus = SessionStatus.SESSEION_NONE;
		this.isConnected = true;
	}
	
	public boolean end(){
		this.sessionStatus = SessionStatus.SESSEION_NONE;
		this.isConnected = false;
		return super.end();
	}
	
	public int getSessinId() {return sessinId;}
	public void setSessinId(int sessinId) {this.sessinId = sessinId;}
	public String getmUserId() {return userId;}
	public void setmUserId(String mUserId) {this.userId = mUserId;}
	public String getmUserName() {return userName;}
	public void setmUserName(String mUserName) {this.userName = mUserName;}
	public String getmVirtualAddress() {return virtualAddress;}
	public void setmVirtualAddress(String mVirtualAddress) {this.virtualAddress = mVirtualAddress;}
	public String getmRealAddress() {return realAddress;}
	public void setmRealAddress(String mRealAddress) {this.realAddress = mRealAddress;}
	public int getmRealPort() {return realPort;}
	public void setmRealPort(int mRealPort) {this.realPort = mRealPort;}
	public int getmVirtualPort() {return virtualPort;}
	public void setmVirtualPort(int mVirtualPort) {this.virtualPort = mVirtualPort;}
	public SessionStatus getmStatus() {return sessionStatus;}
	public void setmStatus(SessionStatus mStatus) {this.sessionStatus = mStatus;}
	public boolean ismIsConnected() {return isConnected;}
	public void setmIsConnected(boolean mIsConnected) {this.isConnected = mIsConnected;}
	

}

