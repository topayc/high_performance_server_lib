package ean.network.server.config;

public interface Config {
	public static final String LISTEN_PORT = "listen_port";
	//최대 접속수
	public static final String MAX_CONNECTION = "max_connection";
	//세션당 사용하는 버퍼의 크기 
	public static final String SESSION_BUFFER_LENGTH = "session_buffer_length";
	//Accept 셀렉트를 위한 스레드 갯수 
	public static final String ACCEPT_SELECT_THREAD_COUNT = "accept_select_thread_count";
	//Read 셀렉트를 위한 스레드 갯수
	public static final String READ_SELECT_THREAD_COUNT = "read_selelct_thread_count";
	
	//Accept 된 소켓 채널을 처리하기 위한 스레드 최소 갯수 
	public static final String ACCEPT_PROCESSOR_THREAD_MIN_COUNT = "accept_processor_thread_min_count";
	//Accept 된 소켓 채널을 처리하기 위한 스레드 최대 갯수 
	public static final String ACCEPT_PROCESSOR_THREAD_MAX_COUNT = "accept_processor_thread_max_count";
	//Accept 처리 스레드를 동적 생성하기 위한 클래스 이름 
	public static final String ACCEPT_PROCESSOR_THREAD_CLASSNAME = "accept_processor_thread_classname";
	
	//Read 된 소켓 채널을 처리하기 위한 스레드 최대 갯수 
	public static final String READWRITE_PROCESSOR_THREAD_MAX_COUNT = "readwrite_processor_thread_max_count";
	//Read 된 소켓 채널을 처리하기 위한 스레드 최소 갯수 
	public static final String READWRITE_PROCESSOR_THREAD_MIN_COUNT = "readwrite_processor_thread_min_count";
	//Read 처리 스레드를 동적 생성하기 위한 클래스 이름 
	public static final String READWRITE_PROCESSOR_THREAD_CLASSNAME = "readwrite_processor_thread_classname";
	
	//이벤트 큐를 위해 순환큐를 사용할지 일반 큐를 사용할 지 여부 
	public static final String USE_QUEUE_CIRCULA = "use_circular_queue";
	//순환큐를 사용할 경우 큐의 크기 
	public static final String MAX_QUEUE_LENGTH = "max_queue_length";
	
	//문자열 인코딩/디코딩 셋
	public static final String CHARSET_FOR_ENCODING = "charset_for_encoding";
	public static final String CHARSET_FOR_DECODING = "charset_for_decoding";

	//바이트 오더 설정
	public static final String BYTE_ORDER = "byte_order";

	//바이트 버퍼풀에 사용되는 메모리 다이렉트 버퍼 블럭의 크기 
	public static final String MEMORY_BLOCK_SIZE = "memory_block_size";
	//메모리 버퍼의 블럭 갯수
	public static final String MEMORY_BLOCK_COUNT = "memory_block_count";

	//파일 버퍼에 사용되는 파일 다이렉트 버퍼 블럭의 크기
	public static final String FILE_BLOCK_SIZE = "file_block_size";
	//파일 버퍼 블럭의 갯수 
	public static final String FILE_BLOCK_COUNT = "file_block_count";
	//파일버퍼로 사용할 파일 이름 
	public static final String BUFFER_FILENAME = "buffer_filename";

	//KeepAlive 사용 여부 
	public static final String CAN_KEEP_ALIVE ="can_keep_alive";
	//KeepAlive 주기 (기본 30초)
	public static final String KEEP_ALIVE_INTERVAL="keep_alive_interval";
	//keepAlive 패킷
	public static final String KEEP_ALIVE_PACKET="keep_alive_packet";

	//메시지 콘솔 출력 여부
	public static final String CONSOLE_MESSAGE = "console_message";
	
	public static final String OBJECT_POOL_LIMIT="object_pool_limit";
	
	public java.util.Properties getProperties();
	public String getString(String key);
	public int getInt(String key);
	public double getDouble(String key);
	public boolean getBoolean(String key);
}
