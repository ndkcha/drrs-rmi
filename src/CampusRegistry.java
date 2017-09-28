import java.io.*;

public class CampusRegistry implements Serializable {
	private static final long serialVersionUID = 1L;
	private String virtualAddress;
	private String code;
	private int port;
	String name;
	
	public CampusRegistry(String name, String virtualAddress, String code, int port) {
		this.name = name;
		this.virtualAddress = virtualAddress;
		this.port = port;
		this.code = code;
	}
	
	public String getVirtualAddress() {
		return this.virtualAddress;
	}
	
	public int getPort() {
		return this.port;
	}
	
	public String getCode() {
		return this.code;
	}
}
