package app.model;

public final class BoardCell {

    private int ownerIndex = -1;
    private int orbCount;

    public int getOwnerIndex() {
        return ownerIndex;
    }

    public void setOwnerIndex(int ownerIndex) {
        this.ownerIndex = ownerIndex;
    }

    public int getOrbCount() {
        return orbCount;
    }

    public void setOrbCount(int orbCount) {
        if (orbCount < 0) {
            throw new IllegalArgumentException("Orb count cannot be negative.");
        }

        this.orbCount = orbCount;
        if (orbCount == 0) {
            this.ownerIndex = -1;
        }
    }

    public boolean isEmpty() {
        return orbCount == 0;
    }
}
