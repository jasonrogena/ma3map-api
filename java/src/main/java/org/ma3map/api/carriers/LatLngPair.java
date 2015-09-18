package org.ma3map.api.carriers;

public class LatLngPair {
    private final LatLng pointA;
    private final LatLng pointB;
    private double distance;

    public LatLngPair(LatLng pointA, LatLng pointB, double distance){
        this.pointA = pointA;
        this.pointB = pointB;
        this.distance = distance;
    }

    public LatLngPair(LatLng pointA, LatLng pointB) {
        this.pointA = pointA;
        this.pointB = pointB;
        this.distance = -1;
    }

    public LatLng getPointA(){
        return pointA;
    }

    public LatLng getPointB(){
        return pointB;
    }

    public double getDistance(){
        if(this.distance == -1) {
            final int earthRadius = 6371;

            double latDiff = Math.toRadians(pointA.latitude - pointB.latitude);
            double lonDiff = Math.toRadians(pointA.longitude - pointB.longitude);

            double a = (Math.sin(latDiff/2) * Math.sin(latDiff/2))
                    + Math.sin(lonDiff/2)
                    * Math.sin(lonDiff/2)
                    * Math.cos(Math.toRadians(pointA.latitude))
                    * Math.cos(Math.toRadians(pointB.latitude));

            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

            double d = earthRadius * c;

            this.distance = d * 1609;//convert to metres
        }
        return this.distance;
    }

    public boolean equals(LatLngPair latLngPair){
        if(latLngPair.getPointA().equals(pointA) && latLngPair.getPointB().equals(pointB)){
            return true;
        }
        return false;
    }
}
