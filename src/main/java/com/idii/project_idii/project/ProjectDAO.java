package com.idii.project_idii.project;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.stereotype.Repository;

@Repository
public class ProjectDAO {

    private final DataSource dataSource;

    public ProjectDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // 프로젝트 생성 + 생성자를 관리자 멤버로 등록
    public int addProject(Project project) throws SQLException {

        try (Connection conn = dataSource.getConnection()) {

            boolean previousAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            try {
                long projectId = insertProject(conn, project);

                int memberCnt = insertOwnerMember(
                        conn, projectId, project.getOwnerId());

                if (memberCnt != 1) {
                    conn.rollback();
                    return 0;
                }

                conn.commit();
                return 1;

            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;

            } finally {
                conn.setAutoCommit(previousAutoCommit);
            }
        }
    }

    // 프로젝트 INSERT 후 생성된 PK 반환
    private long insertProject(Connection conn, Project project)
            throws SQLException {

        String sql = "INSERT INTO project "
                   + "(owner_id, title, description, start_date, due_date) "
                   + "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setLong(1, project.getOwnerId());
            pstmt.setString(2, project.getTitle());
            pstmt.setString(3, project.getDescription());
            pstmt.setObject(4, project.getStartDate());
            pstmt.setObject(5, project.getDueDate());

            if (pstmt.executeUpdate() != 1) {
                throw new SQLException("프로젝트 생성 실패");
            }

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }

            throw new SQLException("프로젝트 ID 조회 실패");
        }
    }

    // 생성자를 관리자 멤버로 등록
    private int insertOwnerMember(Connection conn, long projectId,
                                  long ownerId) throws SQLException {

        String sql = "INSERT INTO project_member "
                   + "(user_id, project_id, role) "
                   + "VALUES (?, ?, '0')";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, ownerId);
            pstmt.setLong(2, projectId);

            return pstmt.executeUpdate();
        }
    }
    

    // 프로젝트가 진행 중이고 해당 사용자가 멤버인지 확인
    public boolean canEditContent(long projectId, long userId)
            throws SQLException {

        String sql = "SELECT 1 FROM project p "
                   + "JOIN project_member pm ON pm.project_id = p.project_id "
                   + "WHERE p.project_id = ? "
                   + "AND pm.user_id = ? "
                   + "AND p.deleted_at IS NULL "
                   + "AND p.status = '0'";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, projectId);
            pstmt.setLong(2, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    // 관리자 권한이 있는 프로젝트의 진행 상태 변경
    public int updateProjectStatus(long projectId, long userId,
                                   String status) throws SQLException {

        String sql = "UPDATE project SET status = ? "
                   + "WHERE project_id = ? "
                   + "AND deleted_at IS NULL "
                   + "AND EXISTS ("
                   + "SELECT 1 FROM project_member pm "
                   + "WHERE pm.project_id = project.project_id "
                   + "AND pm.user_id = ? "
                   + "AND pm.role = '0')";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, status);
            pstmt.setLong(2, projectId);
            pstmt.setLong(3, userId);

            return pstmt.executeUpdate();
        }
    }
    
    // 공통 객체 변환 메서드
	private Project mapProject(ResultSet rs) throws SQLException {
	
	    Project project = new Project();
	
	    project.setProjectId(rs.getLong("project_id"));
	    project.setOwnerId(rs.getLong("owner_id"));
	    project.setTitle(rs.getString("title"));
	    project.setDescription(rs.getString("description"));
	    project.setStartDate(
	            rs.getObject("start_date", LocalDate.class));
	    project.setDueDate(
	            rs.getObject("due_date", LocalDate.class));
	    project.setStatus(rs.getString("status"));
	
	    return project;
	}


    // 프로젝트 목록 조회
	public List<Project> getProjectList(long userId) throws SQLException {
	
	    String sql = "SELECT p.* FROM project p "
	               + "JOIN project_member pm ON p.project_id = pm.project_id "
	               + "WHERE pm.user_id = ? "
	               + "AND p.deleted_at IS NULL "
	               + "ORDER BY p.created_at DESC, p.project_id DESC";
	
	    List<Project> projectList = new ArrayList<>();
	
	    try (Connection conn = dataSource.getConnection();
	         PreparedStatement pstmt = conn.prepareStatement(sql)) {
	
	        pstmt.setLong(1, userId);
	
	        try (ResultSet rs = pstmt.executeQuery()) {
	            while (rs.next()) {
	                projectList.add(mapProject(rs));
	            }
	        }
	    }
	
	    return projectList;
	}

	// 프로젝트 상세 조회
	public Project getProject(long projectId, long userId)
	        throws SQLException {
	
	    String sql = "SELECT p.* FROM project p "
	               + "JOIN project_member pm ON p.project_id = pm.project_id "
	               + "WHERE p.project_id = ? "
	               + "AND pm.user_id = ? "
	               + "AND p.deleted_at IS NULL";
	
	    try (Connection conn = dataSource.getConnection();
	         PreparedStatement pstmt = conn.prepareStatement(sql)) {
	
	        pstmt.setLong(1, projectId);
	        pstmt.setLong(2, userId);
	
	        try (ResultSet rs = pstmt.executeQuery()) {
	            if (rs.next()) {
	                return mapProject(rs);
	            }
	        }
	    }
	
	    return null;
	}
	
	// 프로젝트 수정 - 프로젝트 관리자만
	public int updateProject(Project project, long userId)
	        throws SQLException {
	
	    String sql = "UPDATE project "
	               + "SET title = ?, description = ?, "
	               + "start_date = ?, due_date = ? "
	               + "WHERE project_id = ? "
	               + "AND owner_id = ? "
	               + "AND deleted_at IS NULL";
	
	    try (Connection conn = dataSource.getConnection();
	         PreparedStatement pstmt = conn.prepareStatement(sql)) {
	
	        pstmt.setString(1, project.getTitle());
	        pstmt.setString(2, project.getDescription());
	        pstmt.setObject(3, project.getStartDate());
	        pstmt.setObject(4, project.getDueDate());
	        pstmt.setLong(5, project.getProjectId());
	        pstmt.setLong(6, userId);
	
	        return pstmt.executeUpdate();
	    }
	}

	// 프로젝트 관리자가 프로젝트 삭제
	public int deleteProject(long projectId, long userId)
	        throws SQLException {
	
	    String sql = "UPDATE project "
	               + "SET deleted_at = CURRENT_TIMESTAMP "
	               + "WHERE project_id = ? "
	               + "AND owner_id = ? "
	               + "AND deleted_at IS NULL";
	
	    try (Connection conn = dataSource.getConnection();
	         PreparedStatement pstmt = conn.prepareStatement(sql)) {
	
	        pstmt.setLong(1, projectId);
	        pstmt.setLong(2, userId);
	
	        return pstmt.executeUpdate();
	    }
	}

	// 프로젝트 접근 권한 확인
	public boolean canAccessProject(long projectId, long userId)
	        throws SQLException {
	
	    String sql = "SELECT 1 FROM project p "
	               + "JOIN project_member pm ON p.project_id = pm.project_id "
	               + "WHERE p.project_id = ? "
	               + "AND pm.user_id = ? "
	               + "AND p.deleted_at IS NULL";
	
	    try (Connection conn = dataSource.getConnection();
	         PreparedStatement pstmt = conn.prepareStatement(sql)) {
	
	        pstmt.setLong(1, projectId);
	        pstmt.setLong(2, userId);
	
	        try (ResultSet rs = pstmt.executeQuery()) {
	            return rs.next();
	        }
	    }
	}

	// 프로젝트 관리자인지 확인
	public boolean canManageProject(long projectId, long userId)
	        throws SQLException {
	
	    String sql = "SELECT 1 FROM project p "
	               + "JOIN project_member pm ON p.project_id = pm.project_id "
	               + "WHERE p.project_id = ? "
	               + "AND pm.user_id = ? "
	               + "AND pm.role = '0' "
	               + "AND p.deleted_at IS NULL";
	
	    try (Connection conn = dataSource.getConnection();
	         PreparedStatement pstmt = conn.prepareStatement(sql)) {
	
	        pstmt.setLong(1, projectId);
	        pstmt.setLong(2, userId);
	
	        try (ResultSet rs = pstmt.executeQuery()) {
	            return rs.next();
	        }
	    }
	}

	// 프로젝트 멤버 역할 확인
	public String getMemberRole(long projectId, long userId)
	        throws SQLException {
	
	    String sql = "SELECT role FROM project_member "
	               + "WHERE project_id = ? AND user_id = ?";
	
	    try (Connection conn = dataSource.getConnection();
	         PreparedStatement pstmt = conn.prepareStatement(sql)) {
	
	        pstmt.setLong(1, projectId);
	        pstmt.setLong(2, userId);
	
	        try (ResultSet rs = pstmt.executeQuery()) {
	            if (rs.next()) {
	                return rs.getString("role");
	            }
	        }
	    }
	
	    return null;
	}

	// 메인 화면 대시보드 통계
	public Map<Long, Map<String, Long>> getProjectStats(long userId)
	        throws SQLException {
	
	    Map<Long, Map<String, Long>> stats = new HashMap<>();
	
	    String sql = "SELECT p.project_id, "
	            + "(SELECT COUNT(*) FROM issue i "
	            + "WHERE i.project_id = p.project_id) AS issue_count, "
	
	            + "(SELECT COUNT(*) FROM issue i "
	            + "WHERE i.project_id = p.project_id "
	            + "AND i.status = '3') AS done_count, "
	
	            + "(SELECT COUNT(*) FROM issue i "
	            + "WHERE i.project_id = p.project_id "
	            + "AND i.status = '1') AS progress_count, "
	
	            + "(SELECT COUNT(*) FROM issue i "
	            + "WHERE i.project_id = p.project_id "
	            + "AND i.status = '0') AS waiting_count, "
	
	            + "(SELECT COUNT(*) FROM issue i "
	            + "WHERE i.project_id = p.project_id "
	            + "AND i.status = '2') AS review_count, "
	
	            + "(SELECT COUNT(*) FROM test_case tc "
	            + "WHERE tc.project_id = p.project_id) AS tc_count, "
	
	            + "(SELECT COUNT(*) FROM test_case tc "
	            + "WHERE tc.project_id = p.project_id "
	            + "AND tc.result = '0') AS tc_pass, "
	
	            + "(SELECT COUNT(*) FROM test_case tc "
	            + "WHERE tc.project_id = p.project_id "
	            + "AND tc.result = '1') AS tc_fail, "
	
	            + "(SELECT COUNT(*) FROM test_case tc "
	            + "WHERE tc.project_id = p.project_id "
	            + "AND tc.result = '2') AS tc_block, "
	
	            + "(SELECT COUNT(*) FROM test_case tc "
	            + "WHERE tc.project_id = p.project_id "
	            + "AND tc.result = '3') AS tc_na, "
	
	            + "(SELECT COUNT(*) FROM test_case tc "
	            + "WHERE tc.project_id = p.project_id "
	            + "AND tc.result = '4') AS tc_no_run "
	
	            + "FROM project p "
	            + "JOIN project_member pm "
	            + "ON p.project_id = pm.project_id "
	            + "WHERE pm.user_id = ? "
	            + "AND p.deleted_at IS NULL";
	
	    try (Connection conn = dataSource.getConnection();
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
	    }
	
	    return stats;
	}

}
