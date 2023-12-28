package ee.tenman.elektrihind.electricity;

import java.util.Map;

public interface CarSearchUpdateListener {
    void onUpdate(Map<String, String> data, boolean isFinalUpdate);
}

