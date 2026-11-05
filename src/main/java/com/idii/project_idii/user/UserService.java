package com.idii.project_idii.user;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
	
	private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

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
    
    public int addUser(User user) {

        if (user == null) {
            return 0;
        }

        String email = user.getEmail();
        String password = user.getPassword();
        String nickname = user.getNickname();

        if (email == null || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            return 0;
        }

        if (password == null || password.length() < 8 || password.length() > 20) {
            return 0;
        }

        if (nickname == null || nickname.trim().isEmpty()) {
            return 0;
        }

        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            conn = getConnection();

            String sql = "INSERT INTO users(email, password, nickname) "
                       + "VALUES (?, ?, ?)";

            pstmt = conn.prepareStatement(sql);

            pstmt.setString(1, email.trim());

            String encodedPw = passwordEncoder.encode(password);
            pstmt.setString(2, encodedPw);

            pstmt.setString(3, nickname.trim());

            return pstmt.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
            return 0;

        } finally {
            closeDatabase(conn, pstmt, null);
        }
    }
    
    public int countUserByEmail(String email) throws Exception {
    	Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
    	try {
			conn = getConnection();
			
			String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
			
			pstmt = conn.prepareStatement(sql);

	        pstmt.setString(1, email);
	        
	        rs = pstmt.executeQuery();
	        
	        if(rs.next()) {
	        	return rs.getInt(1);
	        }
	        
	        return 0;
			
		} finally {
			closeDatabase(conn, pstmt, rs);
		}
    }
    
    public User login(String email, String pw) throws Exception {
    	
    	Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
    	try {
    		conn = getConnection();
    		String sql ="SELECT user_id, email, nickname, password FROM users "
    				   +"WHERE email = ? AND deleted_at IS NULL";    		
    		pstmt = conn.prepareStatement(sql);
    		pstmt.setString(1, email);
    		
    		rs = pstmt.executeQuery();
    		
    		if(rs.next()) {
    			String encodePw = rs.getString("password");
    			
    			if(passwordEncoder.matches(pw, encodePw)) {
    				
    				User user = new User();
    				
    				user.setUserId(rs.getLong("user_id"));
    				user.setEmail(rs.getString("email"));
    				user.setNickname(rs.getString("nickname"));
    				
    				return user;
    			}
    		}
    		
    		return null;
    		
		} finally {
			closeDatabase(conn, pstmt, rs);
		}
    }

    // 로그인한 회원의 정보만 조회 (탈퇴 회원 제외)
    public User findActiveUser(long userId) throws Exception {
        String sql = "SELECT user_id, email, nickname FROM users WHERE user_id = ? AND deleted_at IS NULL";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                User user = new User();
                user.setUserId(rs.getLong("user_id"));
                user.setEmail(rs.getString("email"));
                user.setNickname(rs.getString("nickname"));
                return user;
            }
        }
    }

    // 현재 비밀번호를 확인하고 닉네임 및 선택적인 새 비밀번호를 함께 수정
    public boolean updateProfile(long userId, String currentPassword, String nickname, String newPassword) throws Exception {
        try (Connection conn = getConnection()) {
            String hash;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT password FROM users WHERE user_id = ? AND deleted_at IS NULL")) {
                ps.setLong(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return false;
                    hash = rs.getString("password");
                }
            }
            if (!passwordEncoder.matches(currentPassword, hash)) return false;
            boolean changePassword = newPassword != null && !newPassword.isBlank();
            String sql = changePassword
                    ? "UPDATE users SET nickname = ?, password = ? WHERE user_id = ? AND deleted_at IS NULL"
                    : "UPDATE users SET nickname = ? WHERE user_id = ? AND deleted_at IS NULL";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, nickname);
                if (changePassword) {
                    ps.setString(2, passwordEncoder.encode(newPassword));
                    ps.setLong(3, userId);
                } else {
                    ps.setLong(2, userId);
                }
                return ps.executeUpdate() == 1;
            }
        }
    }

    // 소유 프로젝트가 남아 있다면 탈퇴를 제한하여 관리자 없는 프로젝트를 방지
    public boolean hasOwnedProjects(long userId) throws Exception {
        String sql = "SELECT 1 FROM project WHERE owner_id = ? AND deleted_at IS NULL LIMIT 1";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    // 비밀번호 재확인 후 소프트 삭제 (협업 기록은 보존)
    public boolean withdraw(long userId, String currentPassword) throws Exception {
        if (currentPassword == null || currentPassword.isBlank()) return false;
        try (Connection conn = getConnection()) {
            String hash;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT password FROM users WHERE user_id = ? AND deleted_at IS NULL")) {
                ps.setLong(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return false;
                    hash = rs.getString(1);
                }
            }
            if (!passwordEncoder.matches(currentPassword, hash)) return false;
            // 프로젝트 소유권 검사와 삭제를 동일 트랜잭션에서 수행
            boolean previousAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT project_id FROM project WHERE owner_id = ? AND deleted_at IS NULL FOR UPDATE")) {
                    ps.setLong(1, userId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) { conn.rollback(); return false; }
                    }
                }
                int updated;
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE users SET deleted_at = CURRENT_TIMESTAMP WHERE user_id = ? AND deleted_at IS NULL")) {
                    ps.setLong(1, userId);
                    updated = ps.executeUpdate();
                }
                conn.commit();
                return updated == 1;
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(previousAutoCommit);
            }
        }
    }

}
