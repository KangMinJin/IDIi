package com.idii.project_idii.issue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.stereotype.Repository;

@Repository
public class IssueDAO {

    private final DataSource dataSource;

    public IssueDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // 이슈 등록
    public int addIssue(Issue issue, long userId)
            throws SQLException {

        String sql = "INSERT INTO issue "
                   + "(project_id, reporter_id, title, content, "
                   + "type, priority, due_date, assignee_id) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, issue.getProjectId());
            pstmt.setLong(2, userId);
            pstmt.setString(3, issue.getTitle());
            pstmt.setString(4, issue.getContent());
            pstmt.setString(5, issue.getType());
            pstmt.setString(6, issue.getPriority());
            pstmt.setObject(7, issue.getDueDate(), Types.DATE);
            pstmt.setObject(8, issue.getAssigneeId(), Types.BIGINT);

            return pstmt.executeUpdate();
        }
    }
    
    // 이슈 공통 매핑 메서드
	private Issue mapIssue(ResultSet rs) throws SQLException {
	
	    Issue issue = new Issue();
	
	    issue.setIssueId(rs.getLong("issue_id"));
	    issue.setProjectId(rs.getLong("project_id"));
	    issue.setReporterId(rs.getLong("reporter_id"));
	    issue.setReporterNickname(rs.getString("reporter_nickname"));
	    issue.setAssigneeNickname(rs.getString("assignee_nickname"));
	    issue.setAssigneeId(rs.getObject("assignee_id", Long.class));
	    issue.setTitle(rs.getString("title"));
	    issue.setContent(rs.getString("content"));
	    issue.setType(rs.getString("type"));
	    issue.setStatus(rs.getString("status"));
	    issue.setPriority(rs.getString("priority"));
	    issue.setDueDate(rs.getObject("due_date", LocalDate.class));
	    issue.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
	
	    return issue;
	}

    // 이슈 목록 조회
	public List<Issue> getIssueList(long projectId) throws SQLException {
	
	    String sql = "SELECT i.*, "
	               + "reporter.nickname AS reporter_nickname, "
	               + "assignee.nickname AS assignee_nickname "
	               + "FROM issue i "
	               + "LEFT JOIN users reporter ON reporter.user_id = i.reporter_id "
	               + "LEFT JOIN users assignee ON assignee.user_id = i.assignee_id "
	               + "WHERE i.project_id = ? "
	               + "AND i.deleted_at IS NULL "
	               + "ORDER BY i.created_at DESC, i.issue_id DESC";
	
	    List<Issue> issueList = new ArrayList<>();
	
	    try (Connection conn = dataSource.getConnection();
	         PreparedStatement pstmt = conn.prepareStatement(sql)) {
	
	        pstmt.setLong(1, projectId);
	
	        try (ResultSet rs = pstmt.executeQuery()) {
	            while (rs.next()) {
	                issueList.add(mapIssue(rs));
	            }
	        }
	    }
	
	    return issueList;
	}
	
	// 이슈 상세 조회
	public Issue getIssue(long issueId) throws SQLException {
	
	    String sql = "SELECT i.*, "
	               + "reporter.nickname AS reporter_nickname, "
	               + "assignee.nickname AS assignee_nickname "
	               + "FROM issue i "
	               + "LEFT JOIN users reporter ON reporter.user_id = i.reporter_id "
	               + "LEFT JOIN users assignee ON assignee.user_id = i.assignee_id "
	               + "WHERE i.issue_id = ? "
	               + "AND i.deleted_at IS NULL";
	
	    try (Connection conn = dataSource.getConnection();
	         PreparedStatement pstmt = conn.prepareStatement(sql)) {
	
	        pstmt.setLong(1, issueId);
	
	        try (ResultSet rs = pstmt.executeQuery()) {
	            if (rs.next()) {
	                return mapIssue(rs);
	            }
	        }
	    }
	
	    return null;
	}
	
	// 이슈 수정
	public int updateIssue(Issue issue) throws SQLException {
	
	    String sql = "UPDATE issue "
	               + "SET title = ?, content = ?, type = ?, "
	               + "due_date = ?, assignee_id = ? "
	               + "WHERE issue_id = ? "
	               + "AND project_id = ? "
	               + "AND deleted_at IS NULL";
	
	    try (Connection conn = dataSource.getConnection();
	         PreparedStatement pstmt = conn.prepareStatement(sql)) {
	
	        pstmt.setString(1, issue.getTitle());
	        pstmt.setString(2, issue.getContent());
	        pstmt.setString(3, issue.getType());
	        pstmt.setObject(4, issue.getDueDate(), Types.DATE);
	        pstmt.setObject(5, issue.getAssigneeId(), Types.BIGINT);
	        pstmt.setLong(6, issue.getIssueId());
	        pstmt.setLong(7, issue.getProjectId());
	
	        return pstmt.executeUpdate();
	    }
	}

	// 이슈 삭제
	public int deleteIssue(long issueId, long projectId)
	        throws SQLException {
	
	    String sql = "UPDATE issue "
	               + "SET deleted_at = CURRENT_TIMESTAMP "
	               + "WHERE issue_id = ? "
	               + "AND project_id = ? "
	               + "AND deleted_at IS NULL";
	
	    try (Connection conn = dataSource.getConnection();
	         PreparedStatement pstmt = conn.prepareStatement(sql)) {
	
	        pstmt.setLong(1, issueId);
	        pstmt.setLong(2, projectId);
	
	        return pstmt.executeUpdate();
	    }
	}

	// 상태, 우선순위 변경
	public int updateIssueField(long issueId, long projectId,
	                            String column, String value)
	        throws SQLException {
	
	    if (!"status".equals(column) && !"priority".equals(column)) {
	        return 0;
	    }
	
	    String sql = "UPDATE issue SET " + column + " = ? "
	               + "WHERE issue_id = ? "
	               + "AND project_id = ? "
	               + "AND deleted_at IS NULL";
	
	    try (Connection conn = dataSource.getConnection();
	         PreparedStatement pstmt = conn.prepareStatement(sql)) {
	
	        pstmt.setString(1, value);
	        pstmt.setLong(2, issueId);
	        pstmt.setLong(3, projectId);
	
	        return pstmt.executeUpdate();
	    }
	}

}
