package ean.network.event;

import java.util.Map;


public class Job {
	
	private int eventType;
	private Map<String, Object> session = null;
	
	public Job() {}
	
	public Job(int eventType, Map<String, Object> session) {
		this.eventType = eventType;
		this.session = session;
	}

	/**
	 * @return Returns the session.
	 */
	public Map<String, Object> getSession() {
		return session;
	}
	/**
	 * @param session The session to set.
	 */
	public void setSession(Map<String, Object> session) {
		this.session = session;
	}
	
	/**
	 * @return Returns the eventType.
	 */
	public int getEventType() {
		return eventType;
	}
	/**
	 * @param eventType The eventType to set.
	 */
	public void setEventType(int eventType) {
		this.eventType = eventType;
	}
}
