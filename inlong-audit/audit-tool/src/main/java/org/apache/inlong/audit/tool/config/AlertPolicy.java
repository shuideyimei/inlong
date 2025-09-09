package org.apache.inlong.audit.tool.config;

import lombok.Data;
import lombok.Getter;
import org.apache.inlong.common.monitor.CounterGroup;

import java.util.List;
import java.util.Arrays;
import java.util.List;


@Getter
public class AlertPolicy {
    private String name;
    private String description;
    private double threshold;
    private String comparisonOperator;
    private String alertType;
    @Getter
    private List<String> targets;

    public AlertPolicy(String name, String description, double threshold, String comparisonOperator, String alertType) {
        this.name = name;
        this.description = description;
        this.threshold = threshold;
        this.comparisonOperator = comparisonOperator;
        this.alertType = alertType;
    }

    public AlertPolicy() {

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
