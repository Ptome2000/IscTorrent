package Execute;

import java.util.Map;

public interface ConnectionListener {
    void onConnectionUpdated();
    void onConnectionError(String errorMessage);
    void onConnectionEstablished(String successMessage);
    void onConnectionTerminated(String terminatedMessage);
}
