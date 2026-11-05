package com.idii.project_idii.issue;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Issue {
	private long issueId;
	private long projectId;
	private long reporterId;
	private Long assigneeId; // 담당자 미배정 시 null 허용
	private String title;
	private String content;
	private String type = "0"; // 기본값: 할 일
	private String status = "0"; // 기본값: 대기
	private String priority = "1"; // 기본값: 중간
	private LocalDate dueDate;
	private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    
    public Issue() {}

	public Issue(long issueId, long projectId, long reporterId, Long assigneeId, String title, String content,
			String type, String status, String priority, LocalDate dueDate, LocalDateTime createdAt,
			LocalDateTime updatedAt, LocalDateTime deletedAt) {
		
		this.issueId = issueId;
		this.projectId = projectId;
		this.reporterId = reporterId;
		this.assigneeId = assigneeId;
		this.title = title;
		this.content = content;
		this.type = type;
		this.status = status;
		this.priority = priority;
		this.dueDate = dueDate;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
		this.deletedAt = deletedAt;
	}

	private String reporterNickname;
	private String assigneeNickname;
	public String getReporterNickname() { return reporterNickname; }
	public void setReporterNickname(String v) { reporterNickname=v; }
	public String getAssigneeNickname() { return assigneeNickname; }
	public void setAssigneeNickname(String v) { assigneeNickname=v; }
	public long getIssueId() {
		return issueId;
	}

	public void setIssueId(long issueId) {
		this.issueId = issueId;
	}

	public long getProjectId() {
		return projectId;
	}

	public void setProjectId(long projectId) {
		this.projectId = projectId;
	}

	public long getReporterId() {
		return reporterId;
	}

	public void setReporterId(long reporterId) {
		this.reporterId = reporterId;
	}

	public Long getAssigneeId() {
		return assigneeId;
	}

	public void setAssigneeId(Long assigneeId) {
		this.assigneeId = assigneeId;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getPriority() {
		return priority;
	}

	public void setPriority(String priority) {
		this.priority = priority;
	}

	public LocalDate getDueDate() {
		return dueDate;
	}

	public void setDueDate(LocalDate dueDate) {
		this.dueDate = dueDate;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}

	public LocalDateTime getDeletedAt() {
		return deletedAt;
	}

	public void setDeletedAt(LocalDateTime deletedAt) {
		this.deletedAt = deletedAt;
	}
  
}
