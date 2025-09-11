package org.apache.inlong.audit.tool.VO;

public class AuditMetricVo {
    private String inlongGroupId;
    private String inlongStreamId;
    private long count;

    public String getInlongGroupId() {
        return inlongGroupId;
    }

    public void setInlongGroupId(String inlongGroupId) {
        this.inlongGroupId = inlongGroupId;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public String getInlongStreamId() {
        return inlongStreamId;
    }

    public void setInlongStreamId(String inlongStreamId) {
        this.inlongStreamId = inlongStreamId;
    }
}
