package org.ma3map.api.helpers;

import java.io.Serializable;

import org.json.JSONException;

/**
 * Created by jason on 26/09/14.
 */
public class JSONArray extends org.json.JSONArray implements Serializable {
    public JSONArray(String arrayString) throws JSONException {
        super(arrayString);
    }

    public JSONArray(){
        super();
    }

    @Override
    public JSONObject getJSONObject(int index) throws JSONException {
        return new JSONObject(super.getJSONObject(index).toString());
    }
}
