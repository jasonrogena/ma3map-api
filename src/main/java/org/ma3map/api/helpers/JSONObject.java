package org.ma3map.api.helpers;

import java.io.Serializable;

import org.json.JSONException;

public class JSONObject extends org.json.JSONObject implements Serializable {
    public JSONObject(String jsonString) throws JSONException {
        super(jsonString);
    }

    public JSONObject(org.json.JSONObject jsonObject) throws JSONException {
        this(jsonObject.toString());
    }

    public JSONObject(){
        super();
    }

    @Override
    public int getInt(String name) throws JSONException {
        if(isNull(name)){
            return -1;
        }
        else {
            return super.getInt(name);
        }
    }

    @Override
    public String getString(String name) throws JSONException {
        if(isNull(name)){
            return "";
        }
        else {
            return super.getString(name).trim();
        }
    }

    @Override
    public JSONArray getJSONArray(String name) throws JSONException {
        return new JSONArray(super.getJSONArray(name).toString());
    }
}
