package com.github.mustafamalikdev.banking.models;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum Location {
    LOC_AFRICA("AFRICA"),
    LOC_ASIA("ASIA"),
    LOC_EUROPE("EUROPE"),
    LOC_NORTH_AMERICA("NORTH_AMERICA"),
    LOC_OCEANIA("OCEANIA"),
    LOC_SOUTH_AMERICA("SOUTH_AMERICA");

    private final String value;
}
