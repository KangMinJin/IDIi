package com.idii.project_idii.issue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.idii.project_idii.project.ProjectService;

@Service
public class IssueCommentService {

    private final IssueService issueService;
    private final ProjectService projectService;

    public IssueCommentService(IssueService issueService, ProjectService projectService) {
        this.projectService = projectService;
        this.issueService = issueService;
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

    public int addComment(long issueId, long userId, String content) {
    	
        if (content == null || content.isBlank() || content.length() > 500) {
            return 0;
        }

        if (issueService.getIssue(issueId, userId) == null || !projectService.canEditContent(issueService.getIssue(issueId, userId).getProjectId(), userId)) {
            return 0;
        }

        Connection conn = null;
        PreparedStatement pstmt = null;
        int cnt = 0;

        try {
        	conn = getConnection();
        	
        	String sql = "INSERT INTO issue_comment (issue_id, user_id, content) "
        			   + "VALUES (?, ?, ?)";
        	
        	pstmt = conn.prepareStatement(sql);
        	
        	pstmt.setLong(1, issueId);
        	pstmt.setLong(2, userId);
        	pstmt.setString(3, content);
        	
        	cnt = pstmt.executeUpdate();
        	
        }catch(Exception e) {
        	e.printStackTrace();
        } finally {
        	closeDatabase(conn, pstmt, null);
        }
        
        return cnt;
    }
    
    public List<IssueComment> getCommentList(long issueId, long userId) {

        if (issueService.getIssue(issueId, userId) == null) {
            return null;
        }

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        List<IssueComment> commentList = new ArrayList<>();

        // 여기부터 SELECT 작성
        try {
        	conn = getConnection();
        	String sql = "SELECT ic.*, u.nickname AS author_nickname FROM issue_comment ic LEFT JOIN users u ON u.user_id=ic.user_id "
        			   + "WHERE ic.issue_id = ? "
        			   + "AND ic.deleted_at IS NULL "
        			   + "ORDER BY ic.created_at, ic.comment_id";
        	
        	pstmt = conn.prepareStatement(sql);
        	pstmt.setLong(1, issueId);
        	
        	rs = pstmt.executeQuery();
        	
        	while(rs.next()) {
        		IssueComment c = new IssueComment();
        		
        		c.setCommentId(rs.getLong("comment_id"));
        		c.setIssueId(rs.getLong("issue_id"));
        		c.setUserId(rs.getLong("user_id"));
                c.setAuthorNickname(rs.getString("author_nickname"));
        		c.setContent(rs.getString("content"));
        		c.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        		c.setUpdatedAt(rs.getObject("updated_at", LocalDateTime.class));
        		c.setDeletedAt(rs.getObject("deleted_at", LocalDateTime.class));
        		
        		commentList.add(c);
        	}
        } catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			closeDatabase(conn, pstmt, rs);
		}
        
        return commentList;
    }
}