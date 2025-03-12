package uk.gov.dwp.uc.pairtest.model;

public enum TicketType {
    INFANT(0),
    CHILD(15),
    ADULT(25);

    private final int price;

    TicketType(int price) {
        this.price = price;
    }

    public int getPrice() {
        return price;
    }
}
