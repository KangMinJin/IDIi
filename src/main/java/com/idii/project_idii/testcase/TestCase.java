package com.idii.project_idii.testcase;

import java.time.LocalDateTime;

public class TestCase {
    private long testCaseId, projectId, createdBy;
    private String caseCode, title, precondition, testSteps, expectedResult, result, remarks;
    private LocalDateTime createdAt, updatedAt;
    private String authorNickname;
    private LocalDateTime executedAt;
    public String getAuthorNickname() { return authorNickname; }
    public void setAuthorNickname(String v) { authorNickname=v; }
    public LocalDateTime getExecutedAt() { return executedAt; }
    public void setExecutedAt(LocalDateTime v) { executedAt=v; }
    public long getTestCaseId() { return testCaseId; }
    public void setTestCaseId(long v) { testCaseId=v; }
    public long getProjectId() { return projectId; }
    public void setProjectId(long v) { projectId=v; }
    public long getCreatedBy() { return createdBy; }
    public void setCreatedBy(long v) { createdBy=v; }
    public String getCaseCode() { return caseCode; }
    public void setCaseCode(String v) { caseCode=v; }
    public String getTitle() { return title; }
    public void setTitle(String v) { title=v; }
    public String getPrecondition() { return precondition; }
    public void setPrecondition(String v) { precondition=v; }
    public String getTestSteps() { return testSteps; }
    public void setTestSteps(String v) { testSteps=v; }
    public String getExpectedResult() { return expectedResult; }
    public void setExpectedResult(String v) { expectedResult=v; }
    public String getResult() { return result; }
    public void setResult(String v) { result=v; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String v) { remarks=v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { createdAt=v; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime v) { updatedAt=v; }
}
