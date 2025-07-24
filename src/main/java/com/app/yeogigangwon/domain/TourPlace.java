package com.app.yeogigangwon.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Document(collection = "tour_places")
public class TourPlace {

    @Id
    private String id;
    private String name;
    private String category;
    private String location;
    private int crowdLevel;

    public TourPlace() {}

    public TourPlace(String id, String name, String category, String location, int crowdLevel) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.location = location;
        this.crowdLevel = crowdLevel;
    }

    // Getter & Setter
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public int getCrowdLevel() { return crowdLevel; }
    public void setCrowdLevel(int crowdLevel) { this.crowdLevel = crowdLevel; }
}
