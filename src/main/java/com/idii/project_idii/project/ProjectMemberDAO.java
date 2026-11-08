package com.idii.project_idii.project;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.stereotype.Repository;

@Repository
public class ProjectMemberDAO {

    private final DataSource dataSource;

    public ProjectMemberDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // 프로젝트 멤버 목록 조회
    public List<ProjectMember> getMemberList(long projectId)
            throws SQLException {

        String sql = "SELECT pm.member_id, pm.user_id, "
                   + "pm.project_id, pm.role, pm.joined_at, "
                   + "u.nickname, u.email "
                   + "FROM project_member pm "
                   + "JOIN users u ON pm.user_id = u.user_id "
                   + "WHERE pm.project_id = ? "
                   + "AND u.deleted_at IS NULL "
                   + "ORDER BY pm.joined_at ASC, pm.member_id ASC";

        List<ProjectMember> members = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, projectId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ProjectMember member = new ProjectMember();

                    member.setMemberId(rs.getLong("member_id"));
                    member.setUserId(rs.getLong("user_id"));
                    member.setProjectId(rs.getLong("project_id"));
                    member.setRole(rs.getString("role"));
                    member.setJoinedAt(
                        rs.getTimestamp("joined_at").toLocalDateTime()
                    );
                    member.setNickname(rs.getString("nickname"));
                    member.setEmail(rs.getString("email"));

                    members.add(member);
                }
            }
        }

        return members;
    }

    // 이메일로 기존 회원을 프로젝트 멤버로 추가
    public int addMember(long projectId, String email, String role)
            throws SQLException {

        String sql = "INSERT INTO project_member "
                   + "(user_id, project_id, role) "
                   + "SELECT u.user_id, ?, ? FROM users u "
                   + "WHERE u.email = ? "
                   + "AND u.deleted_at IS NULL "
                   + "AND NOT EXISTS ("
                   + "SELECT 1 FROM project_member pm "
                   + "WHERE pm.user_id = u.user_id "
                   + "AND pm.project_id = ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, projectId);
            pstmt.setString(2, role);
            pstmt.setString(3, email);
            pstmt.setLong(4, projectId);

            return pstmt.executeUpdate();
        }
    }

    // 프로젝트 생성자를 제외한 멤버 삭제
    public int deleteMember(long projectId, long memberId)
            throws SQLException {

        String sql = "DELETE pm FROM project_member pm "
                   + "JOIN project p ON pm.project_id = p.project_id "
                   + "WHERE pm.member_id = ? "
                   + "AND pm.project_id = ? "
                   + "AND pm.user_id <> p.owner_id";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, memberId);
            pstmt.setLong(2, projectId);

            return pstmt.executeUpdate();
        }
    }
}
