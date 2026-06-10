package de.andreas.cadas.domain.model;

import de.andreas.cadas.domain.geometry.Length;
import de.andreas.cadas.domain.geometry.PlanSegment;
import java.util.UUID;

public record Wall(UUID id, PlanSegment axis, Length thickness) {

    public static Wall create(PlanSegment axis, Length thickness) {
        return new Wall(UUID.randomUUID(), axis, thickness);
    }
}

