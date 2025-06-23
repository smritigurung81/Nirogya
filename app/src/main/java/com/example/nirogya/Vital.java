package com.example.nirogya;

public class Vital {
    private String name;
    private String value;
    private int iconResource;

    public Vital(String name, String value, int iconResource) {
        this.name = name;
        this.value = value;
        this.iconResource = iconResource;
    }

    // Getters
    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public int getIconResource() {
        return iconResource;
    }

    // Setters
    public void setName(String name) {
        this.name = name;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setIconResource(int iconResource) {
        this.iconResource = iconResource;
    }
}