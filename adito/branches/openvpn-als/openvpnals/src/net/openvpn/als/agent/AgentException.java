package net.openvpn.als.agent;

import net.openvpn.als.boot.HttpConstants;

public class AgentException extends Exception {

	int status = HttpConstants.RESP_403_FORBIDDEN;

	public AgentException(String message) {
		super(message);
	}
	
	public AgentException(String message, int status) {
		super(message);
		this.status = status;
	}

	public int getStatus() {
		return status;
	}

}
