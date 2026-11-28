package mod.azure.atomicdawn.item;

public enum NukaCola {

    CLASSIC("nuka_cola"),
    CRANBERRY("nuka_cola_cranberry"),
    CHERRY("nuka_cherry"),
    DARK("nuka_cola_dark"),
    ORANGE("nuka_cola_orange"),
    QUANTUM("nuka_cola_quantum"),
    TWIST("nuka_cola_twist"),
    WILD("nuka_cola_wild"),
    VACCINATED("nuka_cola_vaccinated"),
    GRAPE("nuka_grape");

    private final String id;

    NukaCola(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public String descriptionKey() {
        return "item.atomicdawn." + id + ".desc";
    }
}
