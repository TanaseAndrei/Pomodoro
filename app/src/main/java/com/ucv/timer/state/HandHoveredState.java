package com.ucv.timer.state;

public class HandHoveredState {

    private boolean wasHandHovered;

    public HandHoveredState() {
        this.wasHandHovered = true;
    }

    public void handWasHovered() {
        this.wasHandHovered = true;
    }

    public void resetHandHovered() {
        this.wasHandHovered = false;
    }

    public boolean getHandHovered() {
        return wasHandHovered;
    }
}
