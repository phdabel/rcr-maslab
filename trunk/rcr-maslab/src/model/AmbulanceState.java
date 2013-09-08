package model;

public enum AmbulanceState {

    FINDING("Finding"),
    RESCUING("Rescuing"),
    TRANSPORTING("Transporting");
    private String name;

    private AmbulanceState(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
