package com.redhat.labs.lodestar.model;

public enum HostingEnvOpenShfitRollup {
    OCP_VERSION("ocpVersion"), OCP_VERSION_MAJOR("ocpMajorVersion"), OCP_VERSION_MINOR("ocpMinorVersion");
    
    String depth;
    
    HostingEnvOpenShfitRollup(String column) {
        this.depth = column;
    }
    
    public final String getColumn() {
        return depth;
    }
}
