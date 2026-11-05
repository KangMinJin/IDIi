package com.idii.project_idii.issue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.idii.project_idii.project.ProjectService;
import com.idii.project_idii.project.ProjectMemberService;


@Service
public class IssueService {
	private final ProjectService projectService;
    private final ProjectMemberService projectMemberService;

	public IssueService(ProjectService projectService, ProjectMemberService projectMemberService) {
	    this.projectService = projectService;
        this.projectMemberService = projectMemberService;
	}

	@Value("${db.url}")
	private String jdbcURL;

    @Value("${db.username}")
    private String dbId;

    @Value("${db.password}")
    private String dbPw;
    
    public Connection getConnection() throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");

        return DriverManager.getConnection(jdbcURL, dbId, dbPw);
    }
    
    public void closeDatabase(Connection conn,
    						  PreparedStatement pstmt,
    						  ResultSet rs) {
    	if(rs != null) { try { rs.close(); } catch (Exception e) { e.printStackTrace(); }}
    	if(pstmt != null) { try { pstmt.close(); } catch (Exception e) { e.printStackTrace(); }}
		if(conn != null) { try { conn.close(); } catch (Exception e) { e.printStackTrace(); }}
	}
    
    public int addIssue(Issue issue, long userId) {

    	// 유효성 검사 먼저
    	if (issue == null) {
    	    return 0;
    	}
    	
    	if (issue.getTitle() == null || issue.getTitle().isBlank() || issue.getTitle().length() > 100) {
    		return 0;
    	}
    	
    	if (!"0".equals(issue.getType()) && !"1".equals(issue.getType())) {
    		return 0;
    	}
    	
    	if (!"0".equals(issue.getPriority())
    			&& !"1".equals(issue.getPriority())
    			&& !"2".equals(issue.getPriority())) {
    		return 0;
    	}
    	
    	// 프로젝트 접근 권한 확인
    	if (!projectService.canEditContent(issue.getProjectId(), userId)) {
    	    return 0;
    	}
    	
    	        if (!validAssignee(issue.getProjectId(), issue.getAssigneeId(), userId)) return 0;

Connection conn = null;
    	PreparedStatement pstmt = null;
    	int cnt = 0;

    	try {
    	    conn = getConnection();
    	    
    	    // SQL 작성 및 실행
    	    String sql = "INSERT INTO issue (project_id, reporter_id, title, content, type, priority, due_date, assignee_id) "
    	    		   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    	    
    	    pstmt = conn.prepareStatement(sql);
    	    
    	    
    	    pstmt.setLong(1, issue.getProjectId());
    	    pstmt.setLong(2, userId);
    	    pstmt.setString(3, issue.getTitle());
    	    pstmt.setString(4, issue.getContent());
    	    pstmt.setString(5, issue.getType());
    	    pstmt.setString(6, issue.getPriority());
    	    pstmt.setObject(7, issue.getDueDate(), Types.DATE);
            pstmt.setObject(8, issue.getAssigneeId(), Types.BIGINT);
    	    
    	    cnt = pstmt.executeUpdate();


    	} catch (Exception e) {
    	    e.printStackTrace();

    	} finally {
    	    closeDatabase(conn, pstmt, null);
    	}
    	return cnt;
    }
    
    public List<Issue> getIssueList(long projectId, long userId) {
    	
    	if (!projectService.canAccessProject(projectId, userId)) {
    	    return null;
    	}
    	
    	Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        List<Issue> issueList = new ArrayList<>();
        
		try {
			conn = getConnection();
			
			String sql = "SELECT i.*, reporter.nickname AS reporter_nickname, assignee.nickname AS assignee_nickname FROM issue i LEFT JOIN users reporter ON reporter.user_id=i.reporter_id LEFT JOIN users assignee ON assignee.user_id=i.assignee_id "
					   + "WHERE i.project_id = ? AND i.deleted_at IS NULL "
					   + "ORDER BY i.created_at DESC, i.issue_id DESC";
			
			pstmt = conn.prepareStatement(sql);
			pstmt.setLong(1, projectId);
			
			rs = pstmt.executeQuery();
			
			while(rs.next()) {
				Issue i = new Issue();
				
				i.setIssueId(rs.getLong("issue_id"));
				i.setProjectId(projectId);
				i.setReporterId(rs.getLong("reporter_id"));
                i.setReporterNickname(rs.getString("reporter_nickname"));
                i.setAssigneeNickname(rs.getString("assignee_nickname"));
				i.setAssigneeId(rs.getObject("assignee_id", Long.class));
				i.setTitle(rs.getString("title"));
				i.setContent(rs.getString("content"));
				i.setType(rs.getString("type"));
				i.setStatus(rs.getString("status"));
				i.setPriority(rs.getString("priority"));
				i.setDueDate(rs.getObject("due_date", LocalDate.class));
				i.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
				
				issueList.add(i);
			}
		} catch (Exception e) {
			
			throw new IllegalStateException("이슈 목록 조회 중 오류 발생", e);
			
		} finally {
			closeDatabase(conn, pstmt, rs);
		}
		
		return issueList;
    }
    
    public Issue getIssue(long issueId, long userId) {
    		
    		Connection conn = null;
            PreparedStatement pstmt = null;
            ResultSet rs = null;
            Issue issue = null;
    		
    		try {
    			
    			conn = getConnection();
    			
    			String sql = "SELECT i.*, reporter.nickname AS reporter_nickname, assignee.nickname AS assignee_nickname FROM issue i LEFT JOIN users reporter ON reporter.user_id=i.reporter_id LEFT JOIN users assignee ON assignee.user_id=i.assignee_id "
    					   + "WHERE i.issue_id = ? "
    					   + "AND i.deleted_at IS NULL";
    			pstmt = conn.prepareStatement(sql);
    			pstmt.setLong(1, issueId);
    			
    			rs = pstmt.executeQuery();
    			
    			if(rs.next()) {
    				
    				long projectId = rs.getLong("project_id");
    				
    				if (!projectService.canAccessProject(projectId, userId)) {
    				    return null;
    				}
    				
    				issue = new Issue();
    				
    				issue.setIssueId(rs.getLong("issue_id"));
    				issue.setProjectId(projectId);
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
    			}
    			
			
    		} catch (Exception e) {
    			throw new IllegalStateException("이슈 상세 조회 실패", e);
    		} finally {
    			closeDatabase(conn, pstmt, rs);
    		}
    		return issue;
    	}

public int updateIssue(Issue issue, long userId) {

    // 유효성 검사
    if (issue == null) {
        return 0;
    }

    if (issue.getTitle() == null
            || issue.getTitle().isBlank()
            || issue.getTitle().length() > 100) {
        return 0;
    }

    if (!"0".equals(issue.getType())
            && !"1".equals(issue.getType())) {
        return 0;
    }

    // 프로젝트 접근 권한 확인
    Issue existingIssue = getIssue(issue.getIssueId(), userId);

    if (existingIssue == null) {
        return 0;
    }

    if (existingIssue.getProjectId() != issue.getProjectId()) {
        return 0;
    }

    if (!projectService.canEditContent(issue.getProjectId(), userId)) return 0;
    if (!validAssignee(issue.getProjectId(), issue.getAssigneeId(), userId)) return 0;

    Connection conn = null;
    PreparedStatement pstmt = null;
    int cnt = 0;

    try {
        conn = getConnection();

        String sql = "UPDATE issue "
                   + "SET title = ?, content = ?, type = ?, due_date = ?, assignee_id = ? "
                   + "WHERE issue_id = ? "
                   + "AND project_id = ? "
                   + "AND deleted_at IS NULL";

        pstmt = conn.prepareStatement(sql);

        pstmt.setString(1, issue.getTitle());
        pstmt.setString(2, issue.getContent());
        pstmt.setString(3, issue.getType());
        pstmt.setObject(4, issue.getDueDate(), Types.DATE);
        pstmt.setObject(5, issue.getAssigneeId(), Types.BIGINT);
        pstmt.setLong(6, issue.getIssueId());
        pstmt.setLong(7, issue.getProjectId());

        cnt = pstmt.executeUpdate();

    } catch (Exception e) {
        e.printStackTrace();
    } finally {
        closeDatabase(conn, pstmt, null);
    }

    return cnt;
	}

	public int deleteIssue(long issueId, long userId) {
		
		Issue issue = getIssue(issueId, userId);
		
		if (issue == null || !projectService.canEditContent(issue.getProjectId(), userId)) {
	        return 0;
	    }
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		int cnt = 0;
		
		try {
			conn = getConnection();
			String sql = "UPDATE issue "
			           + "SET deleted_at = CURRENT_TIMESTAMP "
			           + "WHERE issue_id = ? "
			           + "AND project_id = ? "
			           + "AND deleted_at IS NULL";
			
			pstmt = conn.prepareStatement(sql);
			
			pstmt.setLong(1, issueId);
			pstmt.setLong(2, issue.getProjectId());
			
			cnt = pstmt.executeUpdate();
			
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			closeDatabase(conn, pstmt, null);
		}
		return cnt;
	}

    // 프로젝트 멤버만 이슈 상태 변경 가능
    public int updateIssueStatus(long issueId, String status, long userId) {
        if (!List.of("0", "1", "2", "3").contains(status)) return 0;
        return updateIssueField(issueId, "status", status, userId);
    }

    // 프로젝트 멤버만 이슈 우선순위 변경 가능
    public int updateIssuePriority(long issueId, String priority, long userId) {
        if (!List.of("0", "1", "2").contains(priority)) return 0;
        return updateIssueField(issueId, "priority", priority, userId);
    }

    private int updateIssueField(long issueId, String column, String value, long userId) {
        Issue issue = getIssue(issueId, userId);
        if (issue == null || !projectService.canEditContent(issue.getProjectId(), userId)) return 0;

        String sql = "UPDATE issue SET " + column + " = ? "
                   + "WHERE issue_id = ? AND project_id = ? AND deleted_at IS NULL";
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, value);
            pstmt.setLong(2, issueId);
            pstmt.setLong(3, issue.getProjectId());
            return pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        } finally {
            closeDatabase(conn, pstmt, null);
        }
    }
    private boolean validAssignee(long projectId, Long assigneeId, long userId) {
        if (assigneeId == null) {
        	return true;
        }
        var members = projectMemberService.getMemberList(projectId, userId);
        return members != null && members.stream().anyMatch(m -> m.getUserId() == assigneeId);
    }

}
