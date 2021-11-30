package be.kuleuven.distributedsystems.cloud.entities;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class FireStoreShow implements Serializable {
    private String company;
    private String showId;
    private String name;
    private String location;
    private String image;

    public FireStoreShow(String company, String showId, String name, String location, String image) {
        this.company = company;
        this.showId = showId;
        this.name = name;
        this.location = location;
        this.image = image;
    }

    public FireStoreShow(Show show) {
        this.company = show.getCompany();
        this.showId = show.getShowId().toString();
        this.name = show.getName();
        this.location = show.getLocation();
        this.image = show.getImage();
    }

    public FireStoreShow() {
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getShowId() {
        return showId;
    }

    public void setShowId(String showId) {
        this.showId = showId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FireStoreShow that = (FireStoreShow) o;
        return Objects.equals(company, that.company) && Objects.equals(showId, that.showId) && Objects.equals(name, that.name) && Objects.equals(location, that.location) && Objects.equals(image, that.image);
    }

    @Override
    public int hashCode() {
        return Objects.hash(company, showId, name, location, image);
    }
}
