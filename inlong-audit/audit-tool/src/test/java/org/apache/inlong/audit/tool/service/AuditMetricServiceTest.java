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
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class AuditMetricServiceTest {
    @Test
    public void testGetDataproxyAuditMetrics(){
        //查询服务初始化
        AuditMetricService auditMetricService=new AuditMetricService();
        //查询相关数据
        List<AuditMetricVo> dataproxyAuditMetrics = auditMetricService.getDataproxyAuditMetrics();
        //输出数据
        for(AuditMetricVo auditMetricVo:dataproxyAuditMetrics){
            System.out.println(auditMetricVo.getInlongGroupId()+" "+auditMetricVo.getInlongStreamId()+" "+auditMetricVo.getCount());
        }
    }

    @Test
    public void testGetIcebergAuditMetrics(){
        //查询服务初始化
        AuditMetricService auditMetricService=new AuditMetricService();

        //获取iceberg的auditId列表
        List<String> icebergAuditIds=new ArrayList<>();
        icebergAuditIds.add("1073741838");

        //查询相关数据
        List<AuditMetricVo> dataproxyAuditMetrics = auditMetricService.getIcebergAuditMetrics(icebergAuditIds);
        //输出数据
        for(AuditMetricVo auditMetricVo:dataproxyAuditMetrics){
            System.out.println(auditMetricVo.getInlongGroupId()+" "+auditMetricVo.getInlongStreamId()+" "+auditMetricVo.getCount());
        }
    }

    @Test
    public void testGetHiveAuditMetrics(){
        //查询服务初始化
        AuditMetricService auditMetricService=new AuditMetricService();

        //获取hive的auditId列表
        List<String> icebergAuditIds=new ArrayList<>();
        icebergAuditIds.add("1073741838");

        //查询相关数据
        List<AuditMetricVo> dataproxyAuditMetrics = auditMetricService.getHiveAuditMetrics(icebergAuditIds);
        //输出数据
        for(AuditMetricVo auditMetricVo:dataproxyAuditMetrics){
            System.out.println(auditMetricVo.getInlongGroupId()+" "+auditMetricVo.getInlongStreamId()+" "+auditMetricVo.getCount());
        }
    }
}
