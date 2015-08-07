package org.ma3map.api.carriers;

/**
 * Created by jrogena on 03/08/2015.
 */
public class StopPair {
    private final Stop a;
    private final Stop b;
    public StopPair(Stop a, Stop b){
        this.a = a;
        this.b = b;
    }

    public boolean equals(StopPair otherPair) {
        Stop otherA = otherPair.getA();
        Stop otherB = otherPair.getB();
        if(otherA.equals(a)){
            if(otherB.equals(b)) {
                return true;
            }
        }
        else if(otherA.equals(b)) {
            if(otherB.equals(a)) {
                return true;
            }
        }
        return false;
    }

    public Stop getA() {
        return a;
    }

    public Stop getB() {
        return b;
    }
}
