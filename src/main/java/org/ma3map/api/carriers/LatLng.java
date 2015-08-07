package org.ma3map.api.carriers;

import java.util.ArrayList;
import java.lang.StringBuffer;
import java.lang.Math;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import org.json.JSONObject;
import org.json.JSONException;

import org.ma3map.api.handlers.Log;

@PersistenceCapable
public class LatLng {

    private final static String TAG = "ma3map.LatLng";    
	
	@PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
    private Long id;
	
	@Persistent
	public final double latitude;
	
	@Persistent
	public final double longitude;
	
	public Long getId(){
    	return this.id;
    }
    
    public void setId(Long id){
    	this.id = id;
    }

    public JSONObject getJSONObject() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("lat", latitude);
            jsonObject.put("lng", longitude);
            return jsonObject;
        }
        catch (JSONException e) {
            Log.e(TAG, "An error occurred while trying to create a JSONObject for this object");
            e.printStackTrace();
        }
        return null;
    }	

	public LatLng(double latitude, double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}
	
	public boolean equals(LatLng compare){
		if(compare.latitude == latitude && compare.longitude == longitude){
			return true;
		}
		return false;
	}

    public String getString() {
        return String.valueOf(latitude)+","+String.valueOf(longitude);
    }

    /**
     * Encodes a sequence of LatLngs into an encoded path string.
     */
    public static String encodePolyline(final ArrayList<LatLng> path) {
        long lastLat = 0;
        long lastLng = 0;

        final StringBuffer result = new StringBuffer();

        for (final LatLng point : path) {
            long lat = Math.round(point.latitude * 1e5);
            long lng = Math.round(point.longitude * 1e5);

            long dLat = lat - lastLat;
            long dLng = lng - lastLng;

            encode(dLat, result);
            encode(dLng, result);

            lastLat = lat;
            lastLng = lng;
        }
        return result.toString();
    }
    
    private static void encode(long v, StringBuffer result) {
        v = v < 0 ? ~(v << 1) : v << 1;
        while (v >= 0x20) {
            result.append(Character.toChars((int) ((0x20 | (v & 0x1f)) + 63)));
            v >>= 5;
        }
        result.append(Character.toChars((int) (v + 63)));
    }
}
