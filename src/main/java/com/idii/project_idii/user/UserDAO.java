package com.idii.project_idii.user;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.sql.DataSource;

import org.springframework.stereotype.Repository;

@Repository
public class UserDAO {

    private final DataSource dataSource;

    public UserDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // 회원가입
    public int addUser(User user) throws Exception {

        String sql = "INSERT INTO users(email, password, nickname) "
                   + "VALUES (?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, user.getEmail());
            pstmt.setString(2, user.getPassword());
            pstmt.setString(3, user.getNickname());

            return pstmt.executeUpdate();
        }
    }

    // 이메일 중복 확인
    public int countUserByEmail(String email) throws Exception {

        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
                return 0;
            }
        }
    }

    // 이메일로 회원 조회
    public User findByEmail(String email) throws Exception {

        String sql = "SELECT user_id, email, nickname, password "
                   + "FROM users "
                   + "WHERE email = ? AND deleted_at IS NULL";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email);

            try (ResultSet rs = pstmt.executeQuery()) {

                if (rs.next()) {
                    User user = new User();

                    user.setUserId(rs.getLong("user_id"));
                    user.setEmail(rs.getString("email"));
                    user.setNickname(rs.getString("nickname"));
                    user.setPassword(rs.getString("password"));

                    return user;
                }

                return null;
            }
        }
    }
    
    // 탈퇴 회원 제외하고 회원 조회
    public User findActiveUser(long userId) throws Exception {
    	
        String sql = "SELECT user_id, email, nickname "
        		   + "FROM users WHERE user_id = ? "
        		   + "AND deleted_at IS NULL";
        try (Connection conn = dataSource.getConnection();
        	 PreparedStatement pstmt = conn.prepareStatement(sql)) {
        	
            pstmt.setLong(1, userId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
            	
                if (!rs.next()) {
                	return null;
                }
                
                User user = new User();
                user.setUserId(rs.getLong("user_id"));
                user.setEmail(rs.getString("email"));
                user.setNickname(rs.getString("nickname"));
                
                return user;
            }
        }
    }
    
    public String findPwByUserId(long userId) throws Exception {
    	
    	String sql = "SELECT password FROM users "
    			   + "WHERE user_id = ? "
    			   + "AND deleted_at IS NULL";
    	
        try (Connection conn = dataSource.getConnection();
    		 PreparedStatement pstmt = conn.prepareStatement(sql);) {
	        	pstmt.setLong(1, userId);
            
                try (ResultSet rs = pstmt.executeQuery()) {
                    
                	if (!rs.next()) {
                    	return null;
                    }                	
                
                return rs.getString("password");
            }
        }
    }
    
    public int updateProfile(long userId, String nickname, String encodedPassword) throws Exception {
        
        boolean changePassword = encodedPassword != null;
        
        String sql = changePassword
                ? "UPDATE users SET nickname = ?, password = ? WHERE user_id = ? AND deleted_at IS NULL"
                : "UPDATE users SET nickname = ? WHERE user_id = ? AND deleted_at IS NULL";
        
        try (Connection conn = dataSource.getConnection();
    		 PreparedStatement pstmt = conn.prepareStatement(sql)) {
            	pstmt.setString(1, nickname);
            	
            if (changePassword) {
                pstmt.setString(2, encodedPassword);
                pstmt.setLong(3, userId);
                
            } else {
                pstmt.setLong(2, userId);
            }
            return pstmt.executeUpdate();
        }
    }
    
    public boolean hasOwnedProjects(long userId) throws Exception {
        String sql = "SELECT 1 FROM project "
                   + "WHERE owner_id = ? AND deleted_at IS NULL LIMIT 1";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }
    
 // 회원 소프트 삭제 (소유 프로젝트가 있으면 탈퇴 거부)
    public boolean withdraw(long userId) throws Exception {

        try (Connection conn = dataSource.getConnection()) {

            boolean previousAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            try {
                String chkSql =
                        "SELECT project_id FROM project "
                      + "WHERE owner_id = ? AND deleted_at IS NULL FOR UPDATE";

                try (PreparedStatement pstmt = conn.prepareStatement(chkSql)) {
                    pstmt.setLong(1, userId);

                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            conn.rollback();
                            return false;
                        }
                    }
                }

                String updateSql =
                        "UPDATE users SET deleted_at = CURRENT_TIMESTAMP "
                      + "WHERE user_id = ? AND deleted_at IS NULL";

                int updated;

                try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                    pstmt.setLong(1, userId);
                    updated = pstmt.executeUpdate();
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
