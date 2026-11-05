package com.idii.project_idii.project;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ProjectMemberService {
	
	private final ProjectService projectService;

	public ProjectMemberService(ProjectService projectService) {
	    this.projectService = projectService;
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
    /**
     * getMemberList
     * 해당 프로젝트의 멤버 목록 가져오기
     * @param projectId,userId
     * @return projectMemberList
     */
    public List<ProjectMember> getMemberList(long projectId, long userId) {
    	
    	if (!projectService.canAccessProject(projectId, userId)) {
    	    return null;
    	}
    	
    	Connection conn = null;
    	PreparedStatement pstmt = null;
    	ResultSet rs = null;
    	
    	List<ProjectMember> projectMemberList = new ArrayList<>();
    			
    	try {
    		conn = getConnection();
    		String sql = "SELECT pm.member_id, "
    					+ "pm.user_id, "
    					+ "pm.project_id, "
    					+ "pm.role, "
    					+ "pm.joined_at, "
    					+ "u.nickname, "
    					+ "u.email "
    					+ "FROM project_member pm "
    					+ "JOIN users u ON pm.user_id = u.user_id "
    					+ "WHERE pm.project_id = ? "
    					+ "AND u.deleted_at IS NULL "
    					+ "ORDER BY pm.joined_at ASC, pm.member_id ASC";
    		
    		pstmt = conn.prepareStatement(sql);
    		pstmt.setLong(1, projectId);
    		
    		rs = pstmt.executeQuery();
    		
    		while(rs.next()) {
    			ProjectMember pm = new ProjectMember();
    			pm.setMemberId(rs.getLong("member_id"));
    			pm.setUserId(rs.getLong("user_id"));
    			pm.setProjectId(rs.getLong("project_id"));
    			pm.setRole(rs.getString("role"));
    			pm.setJoinedAt(rs.getObject("joined_at", LocalDateTime.class));
    			pm.setNickname(rs.getString("nickname"));
    			pm.setEmail(rs.getString("email"));
    			
    			projectMemberList.add(pm);
    		}
    	} catch (Exception e) {
    	    throw new IllegalStateException("프로젝트 멤버 목록 조회 실패", e);
    	    
    	} finally {
    		closeDatabase(conn, pstmt, rs);
    	}
    	return projectMemberList;
    }
    
    /**
     * addMember
     * 프로젝트 관리자가 유저 이메일로 프로젝트에 멤버(QA, 개발 분류하여) 추가
     * 이미 존재하는 멤버는 제외
     * @param projectId, email, role, userId
     * @return int
     */
	public int addMember(long projectId, String email,
	                     String role, long userId) {
	
	    // 1. 관리자 권한 검사
	    if (!projectService.canManageProject(projectId, userId) || !projectService.canEditContent(projectId, userId)) {
	        return 0;
	    }
	
	    // 2. 입력값 검사
	    if (email == null || email.isBlank()) {
	        return 0;
	    }
	
	    if (!"1".equals(role) && !"2".equals(role)) {
	        return 0;
	    }
	
	    Connection conn = null;
	    PreparedStatement pstmt = null;
	
	    try {
	        conn = getConnection();
	
	        String sql = "INSERT INTO project_member "
	                   + "(user_id, project_id, role) "
	                   + "SELECT u.user_id, ?, ? "
	                   + "FROM users u "
	                   + "WHERE u.email = ? "
	                   + "AND u.deleted_at IS NULL "
	                   + "AND NOT EXISTS ( "
	                   + "SELECT 1 FROM project_member pm "
	                   + "WHERE pm.user_id = u.user_id "
	                   + "AND pm.project_id = ?)";
	
	        pstmt = conn.prepareStatement(sql);
	
	        pstmt.setLong(1, projectId);
	        pstmt.setString(2, role);
	        pstmt.setString(3, email.trim());
	        pstmt.setLong(4, projectId);
	
	        return pstmt.executeUpdate();
	
	    } catch (Exception e) {
	        e.printStackTrace();
	        return 0;
	
	    } finally {
	        closeDatabase(conn, pstmt, null);
	    }
	}
	
	/**
	 * deleteMember
	 * 프로젝트 관리자가 프로젝트에서 멤버 제거(프로젝트 생성자 제외)
	 * @param projectId, memberId, userId
	 * @return int
	 */
	public int deleteMember(long projectId, long memberId,
	                        long userId) {
	
	    if (!projectService.canManageProject(projectId, userId) || !projectService.canEditContent(projectId, userId)) {
	        return 0;
	    }
	
	    Connection conn = null;
	    PreparedStatement pstmt = null;
	
	    try {
	        conn = getConnection();
	
	        String sql = "DELETE pm FROM project_member pm "
	                   + "JOIN project p ON pm.project_id = p.project_id "
	                   + "WHERE pm.member_id = ? "
	                   + "AND pm.project_id = ? "
	                   + "AND pm.user_id <> p.owner_id";
	
	        pstmt = conn.prepareStatement(sql);
	        pstmt.setLong(1, memberId);
	        pstmt.setLong(2, projectId);
	
	        return pstmt.executeUpdate();
	
	    } catch (Exception e) {
	        e.printStackTrace();
	        return 0;
	
	    } finally {
	        closeDatabase(conn, pstmt, null);
	    }
	}

}
