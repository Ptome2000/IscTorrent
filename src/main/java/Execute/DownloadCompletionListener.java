package Execute;

import java.util.Map;

public interface DownloadCompletionListener {
    void onDownloadComplete(Map<String, Integer> nodeBlockCounts, long elapsedTime);
}

