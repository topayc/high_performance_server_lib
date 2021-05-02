package ean.network.test;

import java.nio.ByteBuffer;

import ean.network.crypt.Crypt;

public class CryptTest {
	public static void main(String[] args) {
		ByteBuffer buffer = ByteBuffer.allocateDirect(1000);
		buffer.putInt(1000);
		buffer.putInt(2000);
	
		for (int i = 0; i < 8; i++){
			System.out.printf("%02x    ", buffer.get(i));
		}
		System.out.println();
		System.out.println(buffer.getInt(0));
		System.out.println(buffer.getInt(4));
		
		System.out.println();
		System.out.println("암호화 합니다");
		Crypt.encrypt(buffer,buffer,8);
		
		System.out.println("암호화된 정보를 출력하여 원래의 데이타  비교합니다. ");
		for (int i = 0; i < 8; i++){
			System.out.printf("%02x    ", buffer.get(i));
		}
		
		System.out.println();
		System.out.println(buffer.getInt(0));
		System.out.println(buffer.getInt(4));
		
		System.out.println();
		System.out.println("복호화 합니다.");
		Crypt.decrypt(buffer,buffer,8);
		System.out.println("복호와 해서 원래 정보로 복원되는지 확인합니다.");
		for (int i = 0; i < 8; i++){
			System.out.printf("%02x    ", buffer.get(i));
		}
		System.out.println();
		System.out.println(buffer.getInt(0));
		System.out.println(buffer.getInt(4));
		
		System.out.println("암호와 복호와 완료");
		
		

	}

}
