package org.ma3map.api.carriers;

import org.ma3map.api.handlers.Log;

import java.util.ArrayList;
import java.util.Comparator;

/**
 * Created by jrogena on 02/08/2015.
 */
public class Path {
    private static final String TAG = "ma3map.Path";
    private final ArrayList<Stop> stops;
    private final double weight;

    public Path(ArrayList<Stop> stops, double weight) {
        this.stops = stops;
        this.weight = weight;
        Log.d(TAG, "Path with weight "+String.valueOf(weight)+" has "+String.valueOf(stops.size())+" stops");
    }

    public ArrayList<Stop> getStops() {
        for(int index = 0; index < stops.size(); index++) {
            if(stops.get(index) == null) {
                Log.e(TAG, "One of the path stops is null");
            }
        }
        return stops;
    }

    public double getWeight() {
        return weight;
    }

    /**
     * This class is used to compare two paths' weights.
     * Can be used with List.sort
     */
    public static class WeightComparator implements Comparator<Path> {

        public int compare(Path o1, Path o2) {
            double s0 = o1.getWeight();
            double s1 = o2.getWeight();

            if(s0 < s1){
                return -1;
            }
            else if(s0 == s1){
                return 0;
            }
            else {
                return 1;
            }
        }
    }
}
