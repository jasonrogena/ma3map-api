package org.ma3map.api.carriers;

import java.util.Comparator;

import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import org.ma3map.api.helpers.JSONObject;
import org.ma3map.api.handlers.Log;

import org.json.JSONException;

@PersistenceCapable
public class Stop {
	private static final String TAG = "ma3map.Stop";
    //stop_id text, stop_name text, stop_code text, stop_desc text, stop_lat text, stop_lon text, location_type int, parent_station text
    public static final String PARCELABLE_KEY = "Stop";
    private static final int PARCELABLE_DESC = 1834;
    public static final String[] ALL_COLUMNS = new String[]{"stop_id", "stop_name", "stop_code", "stop_desc", "stop_lat", "stop_lon", "location_type", "parent_station"};

    @PrimaryKey
    @Persistent
    private String id;
    
    @Persistent
    private String name;
    
    @Persistent
    private String code;
    
    @Persistent
    private String desc;
    
    @Persistent
    private String lat;
    
    @Persistent
    private String lon;
    
    @Persistent
    private int locationType;
    
    @Persistent
    private String parentStation;
    
    public Stop(){
        id = null;
        name = null;
        code = null;
        desc = null;
        lat = null;
        lon = null;
        locationType = -1;
        parentStation = null;
    }

    public org.json.JSONObject getJSONObject() {
        org.json.JSONObject jsonObject = new org.json.JSONObject();
        try {
            jsonObject.put("id", id);
            jsonObject.put("name", name);
            jsonObject.put("code", code);
            jsonObject.put("desc", desc);
            jsonObject.put("lat", lat);
            jsonObject.put("lng", lon);
            jsonObject.put("location_type", locationType);
            jsonObject.put("parent_station", parentStation);
            return jsonObject;     
        }
        catch(JSONException e){
            Log.e(TAG, "An error occurred while trying to generate JSONObject for object");
            e.printStackTrace();
        }

        return null;
    }
    
    public Stop(JSONObject stopData) throws JSONException{
        id = stopData.getString("stop_id");
        name = stopData.getString("stop_name");
        code = stopData.getString("stop_code");
        desc = stopData.getString("stop_desc");
        lat = stopData.getString("stop_lat");
        lon = stopData.getString("stop_lon");
        locationType = stopData.getInt("location_type");
        parentStation = stopData.getString("parent_station");
    }
    
    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public void setLat(String lat) {
        this.lat = lat;
    }

    public void setLon(String lon) {
        this.lon = lon;
    }

    public void setLocationType(int locationType) {
        this.locationType = locationType;
    }

    public void setParentStation(String parentStation) {
        this.parentStation = parentStation;
    }
    
    public double getDistance(LatLng point){
        final int earthRadius = 6371;
        LatLng stopLocation = getLatLng();

        double latDiff = Math.toRadians(stopLocation.latitude - point.latitude);
        double lonDiff = Math.toRadians(stopLocation.longitude - point.longitude);

        double a = (Math.sin(latDiff/2) * Math.sin(latDiff/2))
                    + Math.sin(lonDiff/2)
                    * Math.sin(lonDiff/2)
                    * Math.cos(Math.toRadians(stopLocation.latitude))
                    * Math.cos(Math.toRadians(point.latitude));

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        double d = earthRadius * c;

        return d * 1609;//convert to metres
    }
    
    public String getId(){
        return this.id;
    }

    public String getLat(){
        return this.lat;
    }

    public String getLon(){
        return this.lon;
    }

    public String getName(){
        return this.name;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public int getLocationType() {
        return locationType;
    }

    public String getParentStation() {
        return parentStation;
    }

    public LatLng getLatLng(){
        return new LatLng(Double.parseDouble(lat), Double.parseDouble(lon));
    }

    public boolean equals(Stop comparison){
        if(comparison.getLat().equals(lat) && comparison.getLon().equals(lon) && comparison.getName().equals(name)){
            return true;
        }
        return false;
    }
    
    public static class DistanceComparator implements Comparator<Stop> {

        LatLng reference;

        public DistanceComparator(LatLng reference){
            this.reference = reference;
        }

        @Override
        public int compare(Stop s0, Stop s1) {
            double d0 = s0.getDistance(reference);
            double d1 = s1.getDistance(reference);

            if(d0 < d1){
                return -1;
            }
            else if(d0 == d1){
                return 0;
            }
            else {
                return 1;
            }
        }
    }
}
