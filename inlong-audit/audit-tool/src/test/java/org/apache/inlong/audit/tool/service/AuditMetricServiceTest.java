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
