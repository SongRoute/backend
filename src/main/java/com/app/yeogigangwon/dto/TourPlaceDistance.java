package com.app.yeogigangwon.dto;

import com.app.yeogigangwon.domain.TourPlace;

public class TourPlaceDistance {
    private TourPlace place;
    private double distance; // meter

    public TourPlaceDistance(TourPlace place, double distance) {
        this.place = place;
        this.distance = distance;
    }

    public TourPlace getPlace() { return place; }
    public double getDistance() { return distance; }
}
