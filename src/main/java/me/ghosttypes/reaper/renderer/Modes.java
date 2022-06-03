
package me.ghosttypes.reaper.renderer;

public enum Modes {
    Lines,
    Sides,
    Custom,
    Both;

    public boolean lines() {
        return this == Lines || this == Both;
    }

    public boolean sides() {
        return this == Sides || this == Both;
    }

    public boolean customs() {
        return this == Custom || this == Both;
    }
}
