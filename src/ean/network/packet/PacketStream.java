package ean.network.packet;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

public class PacketStream {
	private ByteBuffer buffer;
	private int length;

	private byte[] divide;
	private boolean isDivided;
	private int devideLength;
	private int currentPositon;

	private CharsetEncoder encoder = null;
	private CharsetDecoder decoder = null;
	private String charsetName = null;

	// 기본 생성자를 호출하게 되면, 디폴트로 255, 255 를 문자열 구분자로 사용하게 된다.
	// 문자열 구분자를 사용하게끔 하는 이 생성자를 호출한다고 하더라도, 길이를 명시하여 문자열을 읽어올 수 있는
	// 메서드도 있다.

	public PacketStream() {
		this.charsetName  = Charset.defaultCharset().name();
		this.encoder = Charset.forName(this.charsetName).newEncoder();
		this.decoder = Charset.forName(this.charsetName).newDecoder();
		init();
	}

	public PacketStream(String charsetName) {
		this.charsetName = charsetName;
		this.encoder = Charset.forName(charsetName).newEncoder();
		this.decoder = Charset.forName(charsetName).newDecoder();
	
		init();
	}

	private void init() {
		divide = new byte[] { (byte) 255, (byte) 255 };
		setIsDivided(true);
		setDevideLength(divide.length);
		setCurrentPositon(0);
		setLength(0);
	}

	public ByteBuffer getBuffer() {return buffer;}
	
	public void setBuffer(ByteBuffer buffer) {
		this.buffer = buffer;
		init();
	}

	public int getLength() {return length;}
	public void setLength(int length) {this.length = length;}
	public byte[] getDivide() {return divide;}
	public void setDivide(byte[] divide) {this.divide = divide;}
	public boolean getIsDivided() {return isDivided;}
	public void setIsDivided(boolean isDivided) {this.isDivided = isDivided;}
	public int getDevideLength() {return devideLength;}
	public void setDevideLength(int devideLength) {this.devideLength = devideLength;}
	public int getCurrentPositon() {return currentPositon;}
	public void setCurrentPositon(int currentPositon) {this.currentPositon = currentPositon;}
	
	public byte[] getArray(){
		if (length <=0) return null;
		if (buffer.hasArray()) return buffer.array();
		
		byte[] result = new byte[length];
		for (int i = 0; i <length ; i++){
			result[i] = buffer.get(i);
		}
		return result;
	}
	public int readInt() {
		int result = buffer.getInt();
		length += Integer.SIZE / 8;
		return result;
	}

	public float readFloat() {
		float result = buffer.getFloat();
		length += Float.SIZE / 8;
		return result;
	}

	public double readDouble() {
		double result = buffer.getDouble();
		length += Double.SIZE / 8;
		return result;
	}

	public long readLong() {
		long result = buffer.getLong();
		length += Long.SIZE / 8;
		return result;
	}

	public byte readByte() {
		byte result = buffer.get();
		length += Byte.SIZE / 8;
		return result;
	}

	public byte[] readBytes(int readLength) {
		byte[] result = new byte[readLength];
		for (int i = 0; i < readLength; i++) {
			result[i] = buffer.get();
		}
		length += Byte.SIZE * readLength / 8;
		return result;
	}
	
	/**
	 * 이미 초기화된 구분자를 이용하여 문자열을 읽어들인다.
	 * @return
	 */
	public String readString() {
		return readStringWithSep(this.divide);
	}

	/**
	 * 구분자를 직접 지정하여 문자열을 읽는다.
	 * @param divideArr
	 * @return
	 */
	public String readString(byte[] divideArr) {
		if (divideArr != null) {
			return readStringWithSep(divideArr);
		} else {
			return readStringWithSep(this.divide);
		}
	}

	/**
	 * 구분자에 의해 실제로 문자열을 읽어들이는 메서드 
	 * @param divide
	 * @return
	 */
	public  String readStringWithSep(byte[] divide) {
		
		String result = null;
		int tmpLimit = buffer.limit();
		int tmpPosition = buffer.position();
		int remainLength = buffer.remaining();

		// 버퍼에 남아있는 길이가 구분자 길이보다 작다면 잘못된 패킷이기 때문에 널을 리턴한다.
		if (remainLength < divide.length)
			return null;

		for (int i = 0; i < remainLength; i++) {
			if (remainLength - i < getDevideLength())
				break;

			boolean bFind = true;
			for (int j = 0; j < getDevideLength(); j++) {
				if (buffer.get(buffer.position() + i + j) != getDivide()[j]) {
					bFind = false;
					break;
				}
			}

			if (bFind) {
				int iLen = i;
				if (iLen <= 0) {
					result = "";
				} else {
					buffer.limit(buffer.position() + iLen);
					try {
						result = decoder.decode(buffer).toString();
						return result;
					} catch (CharacterCodingException e) {
						e.printStackTrace();
					} finally {
						buffer.limit(tmpLimit);
						buffer.position(tmpPosition + iLen + getDevideLength());
						length +=iLen + getDevideLength();
					}
				}
				return result;
			}
		}
		return null;
	}

	/**
	 * 구분자 없이 지정한 길이만큼(바이트 단위) 읽어서 문자열로 변환한다.
	 * @param readLength
	 * @return
	 */
	public String readString(int readLength) {
		int tmpLimit = buffer.limit();
		buffer.limit(buffer.position() + readLength);
		String result = null;
		try {
			result = decoder.decode(buffer).toString();
		} catch (CharacterCodingException e) {
			e.printStackTrace();
		}
		buffer.limit(tmpLimit);
		length +=readLength;
		return result;
	}

	
	public boolean wirteByte(byte data) {
		buffer.put(data);
		length += Byte.SIZE / 8;
		return true;
	}

	public boolean writeBytes(byte[] data) {
		buffer.put(data);
		length += Byte.SIZE * data.length / 8;
		return true;

	}

	public boolean writeDouble(Double data) {
		buffer.putDouble(data);
		length += Double.SIZE / 8;
		return true;
	}

	public boolean writeFloat(float data) {
		buffer.putFloat(data);
		length += Float.SIZE / 8;
		return true;
	}

	public boolean writeInt(int data) {
		buffer.putInt(data);
		length += Integer.SIZE / 8;
		return true;
	}

	public boolean writeLong(long data) {
		buffer.putLong(data);
		length += Long.SIZE / 8;
		return true;
	}

	/**
	 * 디폴트 구문자를 이용해서 문자열을 쓴다.
	 * @param data
	 * @return
	 */
	public boolean writeString(String data) {
		writeStringWithSep(data);
		return true;
	}

	/**
	 * isDivided 변수에 따라 구분자를 넣거나 넣지 않음으로써 문자열을 쓴다.
	 * @param data
	 * @param isDivided
	 * @return
	 */
	public boolean wirteString(String data, boolean isDivided) {
		if (isDivided) {
			writeStringWithSep(data);
		} else {
			writeStringWithoutSep(data);
		}
		return true;
	}

	/**
	 * 구분자를 통하여  문자열을 실제로 쓰는 메서드 
	 * @param data
	 * @return
	 */
	boolean writeStringWithSep(String data) {
		byte[] dataArr = null;
		try {
			dataArr = data.getBytes(charsetName);
		} catch (UnsupportedEncodingException e) {}

		
		buffer.put(dataArr);
		length += dataArr.length;
		writeDivide(this.divide);
		return true;
	}

	/**
	 * 구분자 없이 문자열을 실제로 쓰는 메서드 
	 * @param data
	 * @return
	 */
	private boolean writeStringWithoutSep(String data) {
		byte[] dataArr = null;
		try {
			dataArr = data.getBytes(charsetName);
		} catch (UnsupportedEncodingException e) {
		}
		buffer.put(dataArr);

		length += dataArr.length;
		return true;
	}

	/**
	 * 직접 구분자를 지정하여, 지정된 구분자로 문자열을 쓴다.
	 * @param data
	 * @param divided
	 * @return
	 */
	public boolean writeString(String data, byte[] divided) {
		if (divided != null) {
			writeStringWithoutSep(data);
			writeDivide(divided);
		} else {
			writeStringWithSep(data);
		}
		return true;
	}

	/**
	 * 구분자를 추가하는 메서드 
	 * @param divide
	 * @return
	 */
	private boolean writeDivide(byte[] divide) {
		buffer.put(divide);
		length += divide.length;
		return true;
	}
}
