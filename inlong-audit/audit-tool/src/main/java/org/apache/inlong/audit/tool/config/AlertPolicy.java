package org.apache.inlong.audit.tool.config;

import lombok.Getter;

import java.util.List;

@Getter
public class AlertPolicy {
    private final String name;
    private final String description;
    private final double threshold;
    private final String comparisonOperator;
    private final String alertType;
    private final List<String> targets;

    public AlertPolicy(String name, String description, double threshold, String comparisonOperator, String alertType, List<String> targets) {
        this.name = name;
        this.description = description;
        this.threshold = threshold;
        this.comparisonOperator = comparisonOperator;
        this.alertType = alertType;
        this.targets = targets;
    }

    @Override
    public String toString() {
        return "AlertPolicy{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", threshold=" + threshold +
                ", comparisonOperator='" + comparisonOperator + '\'' +
                ", alertType='" + alertType + '\'' +
                ", targets=" + targets +
                '}';
    }
}