package Execute;

public interface ConnectionListener {
    void onConnectionUpdated();
    void onConnectionError(String errorMessage);
}
