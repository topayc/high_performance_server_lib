package ean.network.logsevice;

import javax.xml.crypto.Data;


public abstract class Log {

	protected boolean enabled;
	protected Data data;
	
	public Log(){
		
	}
	
	public  void println(String logData){
		
	}
	
	public  void print(String logData){
		
	}

	public abstract void print(byte[] logData, int length);

}
