package ean.network.pool;

import java.util.HashMap;
import java.util.Map;

import ean.network.event.Job;
import ean.network.packet.TCPPacket;
import ean.network.pool.buffer.IByteBufferPool;
import ean.network.selector.ISelectorPool;

public class PoolServiceProvider {

	private static Map<String, Object> map = new HashMap<String, Object>();
	//private static PoolManager instance = new PoolManager();

	private PoolServiceProvider() {}

	
	@SuppressWarnings("unchecked")
	public static ObjectPool<TCPPacket> getTCPPakcetPool(){return (ObjectPool<TCPPacket>)map.get("tcpPool");}
	public static void registerTCPPacketPool(ObjectPool<TCPPacket> tcpPacketPool) {map.put("tcpPool",tcpPacketPool);}
	
	public static void registerJobPool(ObjectPool<Job> jobPool){map.put("JobPool",jobPool);}
	@SuppressWarnings("unchecked")
	public static ObjectPool<Job> getJobPool(){return (ObjectPool<Job>)map.get("JobPool");}

	public static ISelectorPool getAcceptSelectorPool() {return (ISelectorPool) map.get("AcceptSelectorPool");}
	public static void registerAcceptSelectorPool(ISelectorPool selectorPool) {map.put("AcceptSelectorPool", selectorPool);}

	public static ISelectorPool getRequestSelectorPool() {return (ISelectorPool) map.get("RequestSelectorPool");}
	public static void registerRequestSelectorPool(ISelectorPool selectorPool) {map.put("RequestSelectorPool", selectorPool);}

	public static void regisertByteBufferPool(IByteBufferPool byteBufferPool) {map.put("ByteBufferPool", byteBufferPool);}
	public static IByteBufferPool getByteBufferPool() {return (IByteBufferPool) map.get("ByteBufferPool");}

}
