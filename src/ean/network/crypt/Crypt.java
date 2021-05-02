package ean.network.crypt;

import java.nio.ByteBuffer;

public class Crypt {
	
	public static final int C1= 52845;
	public static final int C2=22719;
	public static final int KEY = 72597;
	
	public static boolean encrypt(ByteBuffer srcBuffer, ByteBuffer destBuffer, int length){
		int i;
		int key = KEY;
		if (srcBuffer == null || destBuffer == null || length < 0)
		    	return false;
		
		for (i = 0; i <length; i++){
			destBuffer.put(i,(byte)(srcBuffer.get(i)^key >> 8));
			key = (destBuffer.get(i) + key) * C1 +C2;
		}
		return true;
	}
	
	public static boolean decrypt(ByteBuffer srcBuffer, ByteBuffer destBuffer, int length){
		
		int i;
		byte	previousBlock;
		int Key	= KEY;

		if (srcBuffer == null || destBuffer == null || length < 0)
			return false;

		for (i=0;i<length;i++)
		{
			previousBlock = srcBuffer.get(i);;
			destBuffer.put(i,(byte)(srcBuffer.get(i)^Key >> 8));
			Key = (previousBlock + Key) * C1 + C2;
		}
		return true;
	}
	
	public static boolean encrypt(byte[] srcArr, byte[] destArr, int length){
		int 	i;
		int	Key = KEY;

		if (srcArr == null || destArr ==null || length <= 0)
			return false;

		for (i=0;i<length;i++)
		{
			destArr[i] = (byte)(srcArr[i]^Key >> 8);
			Key = (destArr[i] + Key) * C1 + C2;
		}
		return true;
	}
	
	public static boolean decrypt(byte[] srcArr, byte[] destArr, int length){
		int 	i;
		byte	previousBlock;
		int	Key	= KEY;

		if (srcArr == null || destArr ==null || length <= 0)
			return false;

		for (i=0;i<length;i++)
		{
			previousBlock = srcArr[i];
			destArr[i] =(byte)( srcArr[i]^Key >> 8);
			Key = (previousBlock + Key) * C1 + C2;
		}
		return true;
	}
}
