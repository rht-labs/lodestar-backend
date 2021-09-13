package com.redhat.labs.lodestar.model;

public enum HostingRollup {
    OCP_VERSION("ocpVersion"), OCP_VERSION_MAJOR("ocpMajorVersion"), OCP_VERSION_MINOR("ocpMinorVersion");

    String column;

    HostingRollup(String column) {
        this.column = column;
    }

    public final String getColumn() {
        return column;
    }

}
