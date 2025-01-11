package com.epam.training.gen.ai.dating.domain;

public enum Sex {

    M("male"),
    F("female");

    private final String description;

    Sex(String description) {

        this.description = description;
    }

    public String getDescription() {

        return description;
    }
}
