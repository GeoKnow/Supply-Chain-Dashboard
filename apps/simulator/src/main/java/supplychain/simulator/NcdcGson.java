package supplychain.simulator;

import com.google.gson.Gson;

/**
 * Created by rene on 02.09.14.
 */
public class NcdcGson {

    private Gson gson = new Gson();

    public NcdcLocationResult getLocationResult(String string) {
        return gson.fromJson(string, NcdcLocationResult.class);
    }

    public NcdcDailySummaryResult getDailySummaryResult(String string) {
        return gson.fromJson(string, NcdcDailySummaryResult.class);
    }
}
