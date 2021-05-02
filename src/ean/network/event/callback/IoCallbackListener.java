package ean.network.event.callback;

import java.io.IOException;

public interface IoCallbackListener {
	
	//리슨 서버 소켓 채널이 바인딩 되었을 때 호출됨 
	public void onIoChannelBinding(Object object, IoServerBindingEventArgs event) throws IOException;
	
	//클라이언트로 부터 연결 요청이 왔을 때 호출됨 
	public void onIoAccept(Object object, IoAcceptEventArgs event) throws IOException;
	
	//클라이언트로부 데이타가 수신될 경우 호출됨 
	public void onIoRead(Object object, IoReadEventArgs event) throws IOException;
	
	//클라이언트의 연결이 끊어졌을 때 호출됨 
	public void onIoDisconnected(Object object, IoDiconnectedEventArgs event) throws IOException;
	
	//데이타를 쓸 준비가 되었을 때 호출됨 
	public void onIoWrote();

}
