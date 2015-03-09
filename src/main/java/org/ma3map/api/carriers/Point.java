package org.ma3map.api.carriers;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import org.ma3map.api.helpers.JSONObject;
import org.ma3map.api.handlers.Log;

import org.json.JSONException;

@PersistenceCapable
public class Point {
	private static final String TAG = "ma3map.Point";
	public static final String PARCELABLE_KEY = "Point";
    private static final int PARCELABLE_DESC = 6302;
    public static final String[] ALL_COLUMNS = new String[]{"line_id", "point_lat", "point_lon", "point_sequence", "dist_traveled"};

    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
    private Long id;
    
    @Persistent
    private String lat;
    
    @Persistent
    private String lon;
    
    @Persistent
    private int sequence;
    
    @Persistent
    private int distTraveled;

    public Point() {
        lat = null;
        lon = null;
        sequence = -1;
        distTraveled = -1;
    }
    
    public Point(JSONObject pointData) throws JSONException{
        lat = String.valueOf(pointData.getDouble("point_lat"));
        lon = String.valueOf(pointData.getDouble("point_lon"));
        sequence = pointData.getInt("point_sequence");
        distTraveled = pointData.getInt("dist_traveled");
    }

    public org.json.JSONObject getJSONObject() {
        org.json.JSONObject jsonObject = new org.json.JSONObject();
        try {
            jsonObject.put("lat",lat);
            jsonObject.put("lng",lon);
            jsonObject.put("sequence",sequence);
            jsonObject.put("dist_traveled", distTraveled);
            return jsonObject;
        }
        catch(JSONException e) {
            Log.e(TAG, "An error occurred while trying to create a JSONObject for this object");
            e.printStackTrace();
        }
        return null;
    }
    
    public Long getId(){
    	return this.id;
    }
    
    public void setId(Long id){
    	this.id = id;
    }
    
    /**
     * This method returns the LatLng corresponding to this point
     * @return
     */
    public LatLng getLatLng(){
        return new LatLng(Double.parseDouble(lat), Double.parseDouble(lon));
    }

    public int getSequence(){
        return sequence;
    }
}
