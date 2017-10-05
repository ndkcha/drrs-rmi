package data;

public class Campus {
    private int port;
    private String virtualAddress, code;
    public String name;

    public Campus(String name, String virtualAddress, int port, String code) {
        this.name = name;
        this.virtualAddress = virtualAddress;
        this.port = port;
        this.code = code;
    }

    public String getVirtualAddress() {
        return this.virtualAddress;
    }

    public String getCode() {
        return this.code;
    }

    public int getPort() {
        return this.port;
    }
}
