package util;

public enum Channel {
	CHANNEL_ZERO("O canal deve começar do 1"),
    FIRE_BRIGADE("Fire Brigade"),
    AMBULANCE("Ambulance"),
    POLICE_FORCE("Police Force"),
    BROADCAST("Broadcast");
    private String name;

    private Channel(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
