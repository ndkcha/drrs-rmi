package schema;

import java.io.Serializable;
import java.util.HashMap;

public class AuthUdpPacket implements Serializable {
    private static final long serialVersionUID = 1L;
    public int operationName;
    public HashMap<String, Object> body;

    public AuthUdpPacket(int operationName, HashMap<String, Object> body) {
        this.operationName = operationName;
        this.body = body;
    }
}
