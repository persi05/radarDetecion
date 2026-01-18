package com.example.demo;

import java.util.*;

class AssociationNode {
    Integer measurement, target;
    double probability;
    boolean isClutter;
    List<AssociationNode> children = new ArrayList<>();
    AssociationNode parent;

    public AssociationNode(Integer m, Integer t, double p, boolean c) {
        measurement = m; target = t; probability = p; isClutter = c;
    }

    public void addChild(AssociationNode child) {
        child.parent = this;
        children.add(child);
    }

    public void collectLeaves(List<AssociationNode> leaves) {
        if (children.isEmpty()) leaves.add(this);
        else for (AssociationNode child : children) child.collectLeaves(leaves);
    }

    public double getTotalProbability() {
        double p = probability;
        AssociationNode n = parent;
        while (n != null && n.probability > 0) {
            p *= n.probability;
            n = n.parent;
        }
        return p;
    }

    public List<AssociationNode> getPath() {
        List<AssociationNode> path = new ArrayList<>();
        AssociationNode n = this;
        while (n.parent != null) {
            path.add(0, n);
            n = n.parent;
        }
        return path;
    }

    public Integer getMeasurement() { return measurement; }
    public Integer getTarget() { return target; }
    public double getProbability() { return probability; }
    public boolean isClutter() { return isClutter; }
    public AssociationNode getParent() { return parent; }
}