package com.jornal.entity;

public enum AssignmentStatus {
    INVITED, REJECTED, IN_PROGRESS, COMPLETED, CANCELLED;

    /** Una asignacion activa ocupa un cupo del frente y la agenda del trabajador. */
    public boolean isActive() {
        return this == INVITED || this == IN_PROGRESS;
    }
}
