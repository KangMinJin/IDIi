package com.idii.project_idii.project;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ProjectService {
	
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
    
    public int addProject(Project project) {

        Connection conn = null;

        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            // 프로젝트 생성
            long projectId = insertProject(conn, project);

            // 생성된 프로젝트에 소유자를 관리자 멤버로 등록
            int memberCnt = insertOwnerMember(
                    conn, projectId, project.getOwnerId());

            if (memberCnt != 1) {
                conn.rollback();
                return 0;
            }

            conn.commit();
            return 1;

        } catch (Exception e) {

            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }

            e.printStackTrace();
            return 0;

        } finally {
            closeDatabase(conn, null, null);
        }
    }
    
    private long insertProject(Connection conn, Project project)
            throws SQLException {

        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            String sql = "INSERT INTO project "
                       + "(owner_id, title, description, start_date, due_date) "
                       + "VALUES (?, ?, ?, ?, ?)";

            pstmt = conn.prepareStatement(
                    sql, Statement.RETURN_GENERATED_KEYS);

            pstmt.setLong(1, project.getOwnerId());
            pstmt.setString(2, project.getTitle());
            pstmt.setString(3, project.getDescription());
            pstmt.setObject(4, project.getStartDate());
            pstmt.setObject(5, project.getDueDate());

            int cnt = pstmt.executeUpdate();

            if (cnt != 1) {
                throw new SQLException("프로젝트 생성 실패");
            }

            rs = pstmt.getGeneratedKeys();

            if (rs.next()) {
                return rs.getLong(1);
            }

            throw new SQLException("프로젝트 ID 조회 실패");

        } finally {
            closeDatabase(null, pstmt, rs);
        }
    }
    
    private int insertOwnerMember(Connection conn, long projectId,long ownerId) throws SQLException {

		PreparedStatement pstmt = null;
		
		try {
			String sql = "INSERT INTO project_member "
			 + "(user_id, project_id, role) "
			 + "VALUES (?, ?, '0')";
			
			pstmt = conn.prepareStatement(sql);
			
			pstmt.setLong(1, ownerId);
			pstmt.setLong(2, projectId);
			
			return pstmt.executeUpdate();
		
		} finally {
		    closeDatabase(null, pstmt, null);
		}
	}
    
    public boolean canEditContent(long projectId, long userId) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement("SELECT 1 FROM project p JOIN project_member pm ON pm.project_id=p.project_id WHERE p.project_id=? AND pm.user_id=? AND p.deleted_at IS NULL AND p.status='0'");
            ps.setLong(1, projectId);
            ps.setLong(2, userId);
            rs = ps.executeQuery();
            return rs.next();
        } catch (Exception e) { e.printStackTrace(); return false; }
        finally { closeDatabase(conn, ps, rs); }
    }

    public int updateProjectStatus(long projectId, long userId, String status) {
        
    	if (!"0".equals(status) && !"1".equals(status)) {
        	return 0;
        }
        
        if (!canManageProject(projectId, userId)) {
        	return 0;
        }
        Connection conn = null;
        PreparedStatement ps = null;
        
        try {
            conn = getConnection();
            
            String sql = "UPDATE project SET status = ? WHERE project_id = ? "
            		   + "AND deleted_at IS NULL AND EXISTS "
            		   + "(SELECT 1 FROM project_member pm WHERE pm.project_id=project.project_id AND pm.user_id=? AND pm.role='0')";
            
            ps = conn.prepareStatement(sql);
            
            ps.setString(1,status); 
            ps.setLong(2,projectId); 
            ps.setLong(3,userId);
            
            return ps.executeUpdate();
            
        } catch (Exception e) { 
        	e.printStackTrace(); 
        	return 0; 
    	} finally { 
    		closeDatabase(conn, ps, null); 
		}
    }

	public List<Project> getProjectList(long userId) {
		Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        List<Project> projectList = new ArrayList<>();
        
		try {
			conn = getConnection();
			
			String sql = "SELECT p.* FROM project p "
			           + "JOIN project_member pm ON p.project_id = pm.project_id "
			           + "WHERE pm.user_id = ? "
			           + "AND p.deleted_at IS NULL "
			           + "ORDER BY p.created_at DESC, p.project_id DESC";

			pstmt = conn.prepareStatement(sql);
			pstmt.setLong(1, userId);
			
			rs = pstmt.executeQuery();
			
			while(rs.next()) {
				Project p = new Project();
				
				p.setProjectId(rs.getLong("project_id"));
				p.setOwnerId(rs.getLong("owner_id"));
				p.setTitle(rs.getString("title"));
				p.setDescription(rs.getString("description"));
				p.setStartDate(rs.getObject("start_date", LocalDate.class));
				p.setDueDate(rs.getObject("due_date", LocalDate.class));
                p.setStatus(rs.getString("status"));
				
				projectList.add(p);
			}
		} catch (Exception e) {
			
			e.printStackTrace();
			
		} finally {
			closeDatabase(conn, pstmt, rs);
		}
		
		return projectList;
	}
	

	public Project getProject(long projectId, long userId) {
	
	    Connection conn = null;
	    PreparedStatement pstmt = null;
	    ResultSet rs = null;
	    Project project = null;
	
	    try {
	        conn = getConnection();
	
	        String sql = "SELECT p.* FROM project p "
	                + "JOIN project_member pm ON p.project_id = pm.project_id "
	                + "WHERE p.project_id = ? "
	                + "AND pm.user_id = ? "
	                + "AND p.deleted_at IS NULL";

		     pstmt = conn.prepareStatement(sql);
		     pstmt.setLong(1, projectId);
		     pstmt.setLong(2, userId);
	
		     rs = pstmt.executeQuery();
	
	        if (rs.next()) {
	            project = new Project();
	
	            project.setProjectId(rs.getLong("project_id"));
	            project.setOwnerId(rs.getLong("owner_id"));
	            project.setTitle(rs.getString("title"));
	            project.setDescription(rs.getString("description"));
	            project.setStartDate(
	                rs.getObject("start_date", LocalDate.class)
	            );
	            project.setDueDate(
	                rs.getObject("due_date", LocalDate.class)
	            );
                project.setStatus(rs.getString("status"));
	        }
	
	    } catch (Exception e) {
	        e.printStackTrace();
	    } finally {
	        closeDatabase(conn, pstmt, rs);
	    }
	
	    return project;
	}
	
	public int updateProject(Project project, long userId) {
		
		Connection conn = null;
        PreparedStatement pstmt = null;
        
        int cnt = 0;
        
		try {
			
			conn = getConnection();
			
			String sql = "UPDATE project "
					   + "SET title = ?, description = ?, start_date = ?, due_date = ? "
					   + "WHERE project_id = ? "
					   + "AND owner_id = ? "
					   + "AND deleted_at IS NULL";
			pstmt = conn.prepareStatement(sql);
			
			pstmt.setString(1, project.getTitle());
			pstmt.setString(2, project.getDescription());
			pstmt.setObject(3, project.getStartDate());
			pstmt.setObject(4, project.getDueDate());
			pstmt.setLong(5, project.getProjectId());
			pstmt.setLong(6, userId);
			
			cnt = pstmt.executeUpdate();
			
			
		} catch (Exception e) {
			e.printStackTrace();
			
		} finally {
			closeDatabase(conn, pstmt, null);
		}
		
		return cnt;
	}
	
	public int deleteProject(long projectId, long userId) {
		
		Connection conn = null;
        PreparedStatement pstmt = null;
        
        int cnt = 0;
        
		try {
			
			conn = getConnection();
			
			String sql = "UPDATE project "
					   + "SET deleted_at = CURRENT_TIMESTAMP "
					   + "WHERE project_id = ? "
					   + "AND owner_id = ? "
					   + "AND deleted_at IS NULL";
			pstmt = conn.prepareStatement(sql);
			
			pstmt.setLong(1, projectId);
			pstmt.setLong(2, userId);
			
			cnt = pstmt.executeUpdate();
			
			
		} catch (Exception e) {
			e.printStackTrace();
			
		} finally {
			closeDatabase(conn, pstmt, null);
		}
		
		return cnt;
	}
	

	public boolean canAccessProject(long projectId, long userId) {

	    Connection conn = null;
	    PreparedStatement pstmt = null;
	    ResultSet rs = null;
	
	    try {
	        conn = getConnection();
	
	        String sql = "SELECT 1 FROM project p "
	                + "JOIN project_member pm ON p.project_id = pm.project_id "
	                + "WHERE p.project_id = ? "
	                + "AND pm.user_id = ? "
	                + "AND p.deleted_at IS NULL";

		     pstmt = conn.prepareStatement(sql);
		     pstmt.setLong(1, projectId);
		     pstmt.setLong(2, userId);
	
		     rs = pstmt.executeQuery();
	
		     return rs.next();
	
	    } catch (Exception e) {
	        e.printStackTrace();
	        return false;
	
	    } finally {
	        closeDatabase(conn, pstmt, rs);
	    }
	}
	/**
	 * canManageProject
	 * 프로젝트 관리자 권한 확인용(수정, 멤버추가 등)
	 * @param projectId, userId
	 * @return boolean
	 */
	public boolean canManageProject(long projectId, long userId) {

	    Connection conn = null;
	    PreparedStatement pstmt = null;
	    ResultSet rs = null;

	    try {
	    		conn = getConnection();

	    		String sql = "SELECT 1 FROM project p "
			                + "JOIN project_member pm ON p.project_id = pm.project_id "
			                + "WHERE p.project_id = ? "
			                + "AND pm.user_id = ? "
			                + "AND pm.role = '0' "
			                + "AND p.deleted_at IS NULL";

		     pstmt = conn.prepareStatement(sql);
		     pstmt.setLong(1, projectId);
		     pstmt.setLong(2, userId);

		     rs = pstmt.executeQuery();
		     return rs.next();

	    } catch (Exception e) {
	        e.printStackTrace();
	        return false;

	    } finally {
	        closeDatabase(conn, pstmt, rs);
	    }
	}
	
    public String getMemberRole(long projectId, long userId) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();

            String sql = "SELECT role FROM project_member "
            		   + "WHERE project_id = ? AND user_id = ?";

            pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, projectId);
            pstmt.setLong(2, userId);

            rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getString("role");
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeDatabase(conn, pstmt, rs);
        }

        return null;
    }
    
    public Map<Long, Map<String, Long>> getProjectStats(long userId) {
        Map<Long, Map<String, Long>> stats = new HashMap<>();

        String sql = " SELECT p.project_id,(SELECT COUNT(*) FROM issue i "
                   + "WHERE i.project_id = p.project_id) AS issue_count, "
                   + "(SELECT COUNT(*) FROM issue i "
                   + "WHERE i.project_id = p.project_id AND i.status = '3') AS done_count, "
                   + "(SELECT COUNT(*) FROM issue i WHERE i.project_id = p.project_id "
                   + "AND i.status = '1') AS progress_count, "
                   + "(SELECT COUNT(*) FROM issue i WHERE i.project_id = p.project_id AND i.status = '0') AS waiting_count, "
                   + "(SELECT COUNT(*) FROM issue i WHERE i.project_id = p.project_id AND i.status = '2') AS review_count, "
                   + "(SELECT COUNT(*) FROM test_case tc "
                   + "WHERE tc.project_id = p.project_id) AS tc_count, "
                   + "(SELECT COUNT(*) FROM test_case tc WHERE tc.project_id = p.project_id AND tc.result = '0') AS tc_pass, "
                   + "(SELECT COUNT(*) FROM test_case tc WHERE tc.project_id = p.project_id AND tc.result = '1') AS tc_fail, "
                   + "(SELECT COUNT(*) FROM test_case tc WHERE tc.project_id = p.project_id AND tc.result = '2') AS tc_block, "
                   + "(SELECT COUNT(*) FROM test_case tc WHERE tc.project_id = p.project_id AND tc.result = '3') AS tc_na, "
                   + "(SELECT COUNT(*) FROM test_case tc WHERE tc.project_id = p.project_id AND tc.result = '4') AS tc_no_run "
                   + "FROM project p JOIN project_member pm ON p.project_id = pm.project_id "
                   + "WHERE pm.user_id = ? AND p.deleted_at IS NULL";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Long> item = new HashMap<>();

                    item.put("issues", rs.getLong("issue_count"));
                    item.put("done", rs.getLong("done_count"));
                    item.put("progress", rs.getLong("progress_count"));
                    item.put("waiting", rs.getLong("waiting_count"));
                    item.put("review", rs.getLong("review_count"));
                    item.put("tc", rs.getLong("tc_count"));
                    item.put("pass", rs.getLong("tc_pass"));
                    item.put("fail", rs.getLong("tc_fail"));
                    item.put("block", rs.getLong("tc_block"));
                    item.put("na", rs.getLong("tc_na"));
                    item.put("noRun", rs.getLong("tc_no_run"));

                    stats.put(rs.getLong("project_id"), item);
                }
            }

        } catch (Exception e) {
            throw new IllegalStateException("프로젝트 통계 조회 실패", e);
        }

        return stats;
    }
}
