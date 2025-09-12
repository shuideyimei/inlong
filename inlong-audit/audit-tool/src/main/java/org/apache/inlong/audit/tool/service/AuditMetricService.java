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

package org.apache.inlong.audit.tool.service;

import org.apache.inlong.audit.tool.VO.AuditMetricVo;
import org.apache.inlong.audit.tool.basemetric.mapper.AuditMapper;
import org.apache.inlong.audit.tool.basemetric.util.AuditSQLUtil;

import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class AuditMetricService {

    private Integer intervalTimeMinute;

    private static final String DATAPROXY_AUDIT_ID = "5";
    private static final DateTimeFormatter LOGTS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Logger LOGGER = LoggerFactory.getLogger(AuditMetricService.class);

    public AuditMetricService() {
        // Retrieve time interval from configuration file, default to 1 minute if not found
        Properties properties = new Properties();
        try {
            properties.load(getClass().getClassLoader().getResourceAsStream("application.properties"));
            this.intervalTimeMinute = Integer.parseInt(properties.getProperty("audit.data.time.interval.minute", "1"));
        } catch (IOException e) {
            this.intervalTimeMinute = 1;
            LOGGER.error("No time interval configuration found, using default value of 1 minute");
        }
    }

    /**
     * Query DataProxy related audit metrics
     * @return List of AuditMetricVo
     */
    public List<AuditMetricVo> getDataproxyAuditMetrics() {
        String logts = getLogTs();
        SqlSession sqlSession = null;
        try {
            sqlSession = AuditSQLUtil.getSqlSession();
            AuditMapper auditMapper = sqlSession.getMapper(AuditMapper.class);
            List<AuditMetricVo> auditMetricVos = auditMapper.queryDataproxyAuditMetric(logts, DATAPROXY_AUDIT_ID);
            return auditMetricVos != null ? auditMetricVos : Collections.emptyList();
        } catch (Exception e) {
            LOGGER.error("Exception occurred during database query: " + e.getMessage());
            return Collections.emptyList();
        } finally {
            if (sqlSession != null) {
                sqlSession.close();
            }
        }
    }

    /**
     * Retrieve Iceberg related audit metrics
     * @param auditIds List of audit IDs to query
     * @return List of AuditMetricVo
     */
    public List<AuditMetricVo> getIcebergAuditMetrics(List<String> auditIds) {
        if (auditIds == null || auditIds.isEmpty()) {
            return Collections.emptyList();
        }

        String logts = getLogTs();
        SqlSession sqlSession = null;
        List<AuditMetricVo> auditMetricVos = new ArrayList<>();

        try {
            sqlSession = AuditSQLUtil.getSqlSession();
            AuditMapper auditMapper = sqlSession.getMapper(AuditMapper.class);

            // Process each audit ID and aggregate results
            for (String auditId : auditIds) {
                List<AuditMetricVo> tempList = auditMapper.queryDataproxyAuditMetric(logts, auditId);
                if (tempList != null && !tempList.isEmpty()) {
                    auditMetricVos.addAll(tempList);
                }
            }
            return auditMetricVos;
        } catch (Exception e) {
            LOGGER.error("Exception occurred during database query: " + e.getMessage());
            return Collections.emptyList();
        } finally {
            if (sqlSession != null) {
                sqlSession.close();
            }
        }
    }

    /**
     * Retrieve Hive related audit metrics
     * @param auditIds List of audit IDs to query
     * @return List of AuditMetricVo
     */
    public List<AuditMetricVo> getHiveAuditMetrics(List<String> auditIds) {
        if (auditIds == null || auditIds.isEmpty()) {
            return Collections.emptyList();
        }

        String logts = getLogTs();
        SqlSession sqlSession = null;
        List<AuditMetricVo> auditMetricVos = new ArrayList<>();

        try {
            sqlSession = AuditSQLUtil.getSqlSession();
            AuditMapper auditMapper = sqlSession.getMapper(AuditMapper.class);

            // Process each audit ID and aggregate results
            for (String auditId : auditIds) {
                List<AuditMetricVo> tempList = auditMapper.queryDataproxyAuditMetric(logts, auditId);
                if (tempList != null && !tempList.isEmpty()) {
                    auditMetricVos.addAll(tempList);
                }
            }
            return auditMetricVos;
        } catch (Exception e) {
            LOGGER.error("Exception occurred during database query: " + e.getMessage());
            return Collections.emptyList();
        } finally {
            if (sqlSession != null) {
                sqlSession.close();
            }
        }
    }

    /**
     * Get the log timestamp value for querying
     * @return formatted timestamp string
     */
    private String getLogTs() {
        return LocalDateTime.now()
                .withSecond(0)
                .minusMinutes(intervalTimeMinute)
                .format(LOGTS_FMT);
    }

    public List<AuditMetricVo> getStorageAuditMetrics(String auditId) {
        //todo：罗哥，写这个接口，用于获取存储相关的指标
        return null;
    }
}