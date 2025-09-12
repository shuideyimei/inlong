/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.audit.tool;

import org.apache.inlong.audit.tool.basemetric.util.AuditSQLUtil;
import org.apache.inlong.audit.tool.config.AppConfig;
import org.apache.inlong.audit.tool.evaluator.AlertEvaluator;
import org.apache.inlong.audit.tool.manager.ManagerClient;
import org.apache.inlong.audit.tool.reporter.PrometheusReporter;
import org.apache.inlong.audit.tool.task.AuditCheckTask;

import java.util.List;

public class AuditToolMain {

    public static void main(String[] args) {
        // Load application configuration
        AppConfig appConfig = new AppConfig();

        // Initialize manager client
        ManagerClient managerClient = new ManagerClient(appConfig);
        List<String> strings = managerClient.fetchAuditIds();

        // Initialize reporters
        PrometheusReporter prometheusReporter = new PrometheusReporter();
        prometheusReporter.init(appConfig.getPrometheusConfig());

        // Database query initialization
        AuditSQLUtil.initialize(appConfig.getProperties());

        // Initialize alert evaluator
        AlertEvaluator alertEvaluator = new AlertEvaluator(prometheusReporter, managerClient);
        AuditCheckTask auditCheckTask =
                new AuditCheckTask(managerClient, alertEvaluator, appConfig);
        auditCheckTask.start();

        // Keep the application running
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            auditCheckTask.stop();
            System.out.println("Audit Tool stopped.");
        }));

        System.out.println("Audit Tool started.");
    }
}