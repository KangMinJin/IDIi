package com.idii.project_idii.project;

import java.time.LocalDateTime;

public class ProjectMember {
	
	public ProjectMember() {}
	
	public ProjectMember(long memberId, long userId, long projectId, String role, LocalDateTime joinedAt) {
		this.memberId = memberId;
		this.userId = userId;
		this.projectId = projectId;
		this.role = role;
		this.joinedAt = joinedAt;
	}

	private long memberId;
	private long userId;
	private long projectId;
	private String role;
	private LocalDateTime joinedAt;

	// user 테이블 JOIN 하여 화면 표시용
	private String nickname;
	private String email;
	
	public long getMemberId() {
		return memberId;
	}
	public void setMemberId(long memberId) {
		this.memberId = memberId;
	}
	public long getUserId() {
		return userId;
	}
	public void setUserId(long userId) {
		this.userId = userId;
	}
	public long getProjectId() {
		return projectId;
	}
	public void setProjectId(long projectId) {
		this.projectId = projectId;
	}
	public String getRole() {
		return role;
	}
	public void setRole(String role) {
		this.role = role;
	}
	public LocalDateTime getJoinedAt() {
		return joinedAt;
	}
	public void setJoinedAt(LocalDateTime joinedAt) {
		this.joinedAt = joinedAt;
	}
	public String getNickname() {
		return nickname;
	}
	public void setNickname(String nickname) {
		this.nickname = nickname;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	
	
}
