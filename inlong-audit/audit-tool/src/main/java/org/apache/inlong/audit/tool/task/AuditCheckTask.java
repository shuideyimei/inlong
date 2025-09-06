package org.apache.inlong.audit.tool.task;

import lombok.Getter;
import org.apache.inlong.audit.tool.config.AlertPolicy;
import org.apache.inlong.audit.tool.evaluator.AlertEvaluator;
import org.apache.inlong.audit.tool.manager.ManagerClient;
import org.apache.inlong.audit.tool.DTO.AuditData;
import org.apache.inlong.audit.tool.reporter.PrometheusReporter;
import org.apache.inlong.audit.tool.reporter.OpenTelemetryReporter;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 审计检查任务类，定期获取审计数据并评估告警
 */
public class AuditCheckTask {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final AlertEvaluator alertEvaluator;
    @Getter
    private PrometheusReporter prometheusReporter;
    @Getter
    private OpenTelemetryReporter openTelemetryReporter;
    private final ManagerClient managerClient;
    
    public AuditCheckTask(PrometheusReporter prometheusReporter, OpenTelemetryReporter openTelemetryReporter, ManagerClient managerClient, AlertEvaluator alertEvaluator) {
        this.prometheusReporter = prometheusReporter;
        this.openTelemetryReporter = openTelemetryReporter;
        this.managerClient = managerClient;
        this.alertEvaluator = alertEvaluator;
    }

    /**
     * 启动审计检查任务
     */
    public void start() {
        scheduler.scheduleAtFixedRate(this::checkAuditData, 0, 30, TimeUnit.SECONDS);
    }
    
    /**
     * 检查审计数据并触发告警评估
     */
    private void checkAuditData() {
        try {
            // 获取审计数据
            List<AuditData> auditDataList = managerClient.fetchAuditData();
            
            // 获取告警策略
            List<AlertPolicy> policies = managerClient.fetchAlertPolicies();
            
            // 对每个审计数据和每个策略进行评估
            for (AuditData auditData : auditDataList) {
                for (AlertPolicy policy : policies) {
                    if (alertEvaluator.shouldTriggerAlert(auditData, policy)) {
                        alertEvaluator.triggerAlert(auditData, policy);
                    }
                }
            }
        } catch (Exception e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
            System.out.println("Error occurred while checking audit data: " + e.getMessage());
        }
    }
    
    /**
     * 停止审计检查任务
     */
    public void stop() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

}