package ean.network.logsevice;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import ean.network.threadsync.Mutex;



/**
 * 스레드에 안전한 파일 로그 클래스.
 * </br>파일 로그를 남기기 위한 클래스로 스레드에 안전하게 처리가 되어 있으며, 스레드에 안전한 처리를 위하여 내부적으로 Mutex 클래스를
 * 사용한다.
 * <pre>
 * <dd>
 * FileLog log = new FileLog();
 * log.SetLogPath("C:\\"); </br>
 * Log.SetLogName("Test"); Log.SetResetType(0); // 하루단위
 * for (int i = 0; i < 1000; i++) { 
 * 	Log.println("11");
 * 	try { 
 * 	  Thread.sleep(1000); 
 * 	 } catch (Exception e) { } 
 * }
 * </dd>
 * </pre>
 */
public class FileLog {
	protected String logName;
	protected String logPath;
	protected String postFix;

	protected boolean isPreFix;
	protected boolean isPreTime;

	protected boolean isEnabled;

	protected int resetType; // 0 : 하루, 1 : 한시간, 2 : 껀수로
	protected Date dtOpen;
	protected Date dtReset;
	protected Mutex mutex = null;
	protected int lockTimeOut = 1000;
	protected File file = null;
	protected FileOutputStream fos = null;

	/**
	 * 기본 생성자 로그패스 :현재디렉토리 ,로그파일명 공백 등 기본 초기화 작업 진행
	 */
	public FileLog() {
		init();
	}

	/**
	 * @param logPath - 로그파일이 저장될 디렉토리
	 * @param logName - 로그파일이름
	 * @param isPreFix - 로그파일 앞에 붙은 프리픽스
	 * @param isPreTime - 파일명생성을 날짜 패턴으로 생성할지 여부
	 */
	public FileLog(String logPath, String logName, boolean isPreFix,boolean isPreTime) {
		init();
		setLogPath(logPath);
		setLogName(logName);
		setPreFix(isPreFix);
		setPreTime(isPreTime);
	}

	/**
	 * 가비지 컬랙팅시 파일 Close
	 */
	protected void finalize() throws Throwable {
		close(true);
		super.finalize();
	}

	/**
	 * 파일명 앞에 붙은 프리픽스 설정
	 */
	public void setPreFix(boolean bPreFix) {
		isPreFix = bPreFix;
	}

	/**
	 * 프리픽스를 구함
	 */
	public boolean getPreFix() {
		return isPreFix;
	}

	/**
	 * 로그 생성시 시간 로깅 여부 설정
	 * @param isPreTime - 로그생성시 시간 로깅 여부
	 */
	public void setPreTime(boolean isPreTime) {
		this.isPreTime = isPreTime;
	}

	public boolean getPreTime() {
		return isPreTime;
	}

	/**
	 * 로그 사용 가능 및 불가능 설정
	 * @param isEnabled - 로그 가능 및 불가능 설정 플래그
	 */
	public void setEnable(boolean isEnabled) {
		this.isEnabled = isEnabled;
	}

	public boolean getEnable() {
		return isEnabled;
	}

	/**
	 * 파일의 재설정 모드 설정 0 : 하루, 1 : 한시간, 2 : 껀수로
	 * @param iType -  파일의 재 설정 모드
	 */
	public void setResetType(int resetType){
		this.resetType = resetType;
	}

	public int getResetType() {
		return resetType;
	}

	/**
	 * 파일의 재설정 시간 계산
	 */

	public void calcResetTime() {
		switch (getResetType()) {
		case 0: {
			Calendar c = Calendar.getInstance();
			c.setTime(new Date());
			c.add(Calendar.DATE, 1);
			c.clear(Calendar.HOUR);
			c.clear(Calendar.HOUR_OF_DAY);
			c.clear(Calendar.MINUTE);
			c.clear(Calendar.SECOND);
			dtReset = c.getTime();
		}
			break;

		case 1: {
			Calendar c = Calendar.getInstance();
			c.setTime(new Date());
			c.add(Calendar.HOUR, 1);
			c.clear(Calendar.HOUR_OF_DAY);
			c.clear(Calendar.MINUTE);
			c.clear(Calendar.SECOND);
			dtReset = c.getTime();
		}
			break;

		case 2:
			dtReset = new Date();
			break;
		}
	}

	/**
	 * 파일로그 패스 설정
	 * @param logPath - 파일로그 패스
	 */
	public void setLogPath(String logPath) {
		this.logPath = logPath;
	}

	public String getLogPath() {
		return logPath;
	}

	/**
	 * 로그할 파일의 이름 설정
	 * @param logName - 파일 로그명
	 */
	public void setLogName(String logName) {
		this.logName = logName;
	}

	public String setLogName() {
		return logName;
	}

	/**
	 * 파일 확장자 설정
	 * @param postFix - 파일의 확장자명
	 */
	public void setPostFix(String postFix) {
		this.postFix = postFix;
	}

	public String GetPostFix() {
		return postFix;
	}

	/**
	 * 스레드 락 타임 아웃 설정
	 * @param lockTimeout  - 스레드락 타임 아웃 시간 설정
	 */
	public void setLockTimeOut(int lockTimeout) {
		this.lockTimeOut = lockTimeout;
	}

	public int getLockTimeOut() {
		return lockTimeOut;
	}

	/**
	 * 스레드 락을 시작함
	 */
	public void lock() throws InterruptedException {
		mutex.acquire(getLockTimeOut());
	}

	/**
	 * 스레드 락을 해제함
	 */
	public void unlock() {
		mutex.release();
	}

	/**
	 * 멤버 초기화함수로 각 생성자에서 이 초기화 메서드를 호출한다. LogPath - "" 으로 설정(현재 디렉토리) LogName -
	 * "" 으로 설정 (로그파일은 날짜로 정해짐) PostFix - .log 로 확장자 설정 Prefix true 설정 PreTime
	 * true 설정 로그 기능 활성화 스레드 락 획득
	 */
	public void init() {
		setLogPath("");setLogName("");setPostFix(".log");
		setPreFix(true);setPreTime(true);setEnable(true);

		if (mutex == null)
			mutex = new Mutex();
		dtOpen = new Date();

		setResetType(0);
		calcResetTime();
	}

	/**
	 * 로그 패스를 생성함
	 */
	public String makePathString(String path) {
		int iSize = path.length();
		int iCur = iSize - 1;
		// int iLim = 0;

		char cAt = (char) 0;
		char cSep = System.getProperty("file.separator").charAt(0);

		for (; iCur > 0; iCur--) {
			cAt = (char) path.charAt(iCur);
			if (cAt == cSep)
				return path;
			else if (cAt == ' ' || cAt == '\t' || cAt == '\r' || cAt == '\n'
					|| cAt == '\b' || cAt == '\f') {
				// iLim = iCur;
				continue;
			} else
				break;
		}

		return (path += cSep).toString();
	}

	/**
	 * 파일을 새로 생성
	 * @return 파일을 새로 생성했는지 여부를 boolean 값으로 반환
	 */
	public boolean newOpen() throws InterruptedException {
		boolean bRet = false;
		if (!getEnable())
			return bRet;

		String path;

		if (fos != null)
			return bRet;

		lock();

		path = getLogPath();
		path = makePathString(path);

		if (getPreFix()) {
			SimpleDateFormat sdt = null;
			switch (getResetType()) {
			case 0:
				sdt = new SimpleDateFormat("yyyyMMdd");
				break;

			case 1:
				sdt = new SimpleDateFormat("yyyyMMddHH");
				break;

			case 2:
				sdt = new SimpleDateFormat("yyyyMMddHHmmssSS");
				break;
			}
			path += setLogName() + sdt.format(new Date()).toString()
					+ GetPostFix();
		} else
			path += setLogName();

		if (file == null)
			file = new File(path);

		try {
			fos = new FileOutputStream(file);

			bRet = true;
		} catch (IOException e) {
			e.printStackTrace();
		}

		unlock();

		return bRet;
	}

	/**
	 * 파일이 열려있는 지 여부 반환
	 * @param isLock  - 스레드락 여부
	 * @return 파일이 열려 있는지 여부
	 */

	public boolean isOpened(boolean isLock) throws InterruptedException {
		boolean bRet = false;
		if (!getEnable() || fos == null)
			return bRet;

		if (isLock)
			lock();

		if (dtReset.getTime() < ((Date) (new Date())).getTime()) {
			close(false);
		} else
			bRet = true;

		if (isLock)
			unlock();

		return bRet;
	}

	/**
	 * 파일  열기
	 * @param isLock  - 스레드 락 여부
	 * @return 파일이 이상없이 열렸는지 여부
	 */
	public boolean open(boolean isLock) {
		boolean bRet = false;
		if (!getEnable() || fos != null)
			return bRet;

		String path;
		try {
			if (isLock)
				lock();

			path = getLogPath();
			path = makePathString(path);

			if (getPreFix()) {
				SimpleDateFormat sdt = null;
				switch (getResetType()) {
				case 0:
					sdt = new SimpleDateFormat("yyyyMMdd");
					break;

				case 1:
					sdt = new SimpleDateFormat("yyyyMMddHH");
					break;

				case 2:
					sdt = new SimpleDateFormat("yyyyMMddHHmmssSS");
					break;
				}
				path += setLogName() + sdt.format(new Date()).toString()
						+ GetPostFix();
			} else
				path += setLogName();

			if (file == null)
				file = new File(path);

			if (!file.exists()) {
				fos = new FileOutputStream(file);
				bRet = true;
			} else {
				if (file.canWrite()) {
					fos = new FileOutputStream(path, true);
					bRet = true;
				}
			}

			calcResetTime();

			if (isLock)
				unlock();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return bRet;
	}

	/**
	 * 파일 닫기
	 * @param isLock  - 스레드 락 여부
	 * @return 파일이 이상없이 닫혔는지 여부
	 */
	public boolean close(boolean isLock) {
		boolean bRet = false;
		if (!getEnable())
			return bRet;

		if (fos == null)
			return bRet;

		try {
			if (isLock)
				lock();

			if (file != null)
				file = null;

			fos.close();

			bRet = true;
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			fos = null;

			if (isLock)
				unlock();
		}

		return bRet;
	}

	/**
	 * byte 블럭 쓰기
	 * @param byteData - 블럭데이타
	 * @param size - 블럭 크기
	 * @param bLook - 스레드 락 여부
	 * @return 쓰여진 바이트 수
	 */
	protected int writeTo(byte[] byteData, int size, boolean isLock) {
		int iRet = 0;
		if (!getEnable() || fos == null)
			return -1;

		try {
			if (isLock)
				lock();

			fos.write(byteData, 0, size);
			iRet = size;

			if (isLock)
				unlock();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return iRet;
	}

	/**
	 * byte 블럭 쓰기
	 * @param byteData - 블럭데이타
	 * @param size - 블럭 크기
	 */
	public void print(byte[] byteData, int size) {
		if (!getEnable())
			return;

		byte[] byteWrite = null;

		try {
			lock();

			if (getPreTime()) {
				SimpleDateFormat sdt = new SimpleDateFormat("HH mm ss | ");
				String szTime = sdt.format(new Date()).toString();
				byteWrite = new byte[size + szTime.getBytes().length];
				System.arraycopy(szTime.getBytes(), 0, byteWrite, 0,
						szTime.getBytes().length);
				System.arraycopy(byteData, 0, byteWrite,
						szTime.getBytes().length, size);

				size += szTime.getBytes().length;
			} else
				byteWrite = byteData;

			if (!isOpened(false)) {
				if (open(false)) {
					writeTo(byteWrite, size, false);
				}
			} else
				writeTo(byteWrite, size, false);

			switch (getResetType()) {
			case 2:
				close(false);
				break;
			}

			unlock();
		} catch (Exception e) {
		}
	}

	/**
	 * 문자열 쓰기
	 * @param data - 로그파일에 쓸 문자열
	 */
	public void print(String data) {
		if (!getEnable())
			return;
		print(data.getBytes(), data.getBytes().length);
	}

	/**
	 * 문자열을 쓰고 난 후 개행
	 * @param data - 로그파일에 쓸 문자열
	 */
	public void println(String data) {
		if (!getEnable())
			return;
		data += "\r\n";
		print(data.getBytes(), data.getBytes().length);
	}
}

