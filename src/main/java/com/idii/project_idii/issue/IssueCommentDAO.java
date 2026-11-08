package com.idii.project_idii.issue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.stereotype.Repository;

@Repository
public class IssueCommentDAO {

    private final DataSource dataSource;

    public IssueCommentDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // 댓글 등록
    public int addComment(long issueId, long userId, String content)
            throws SQLException {

        String sql = "INSERT INTO issue_comment "
                   + "(issue_id, user_id, content) "
                   + "VALUES (?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, issueId);
            pstmt.setLong(2, userId);
            pstmt.setString(3, content);

            return pstmt.executeUpdate();
        }
    }

    // 댓글 목록 조회
    public List<IssueComment> getCommentList(long issueId)
            throws SQLException {

        String sql = "SELECT ic.*, "
                   + "u.nickname AS author_nickname "
                   + "FROM issue_comment ic "
                   + "LEFT JOIN users u ON u.user_id = ic.user_id "
                   + "WHERE ic.issue_id = ? "
                   + "AND ic.deleted_at IS NULL "
                   + "ORDER BY ic.created_at, ic.comment_id";

        List<IssueComment> commentList = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, issueId);

            try (ResultSet rs = pstmt.executeQuery()) {

                while (rs.next()) {
                    commentList.add(mapComment(rs));
                }
            }
        }

        return commentList;
    }

    // 조회 결과를 IssueComment 객체로 변환
    private IssueComment mapComment(ResultSet rs)
            throws SQLException {

        IssueComment comment = new IssueComment();

        comment.setCommentId(rs.getLong("comment_id"));
        comment.setIssueId(rs.getLong("issue_id"));
        comment.setUserId(rs.getLong("user_id"));
        comment.setAuthorNickname(rs.getString("author_nickname"));
        comment.setContent(rs.getString("content"));
        comment.setCreatedAt(
                rs.getObject("created_at", LocalDateTime.class));
        comment.setUpdatedAt(
                rs.getObject("updated_at", LocalDateTime.class));
        comment.setDeletedAt(
                rs.getObject("deleted_at", LocalDateTime.class));

        return comment;
    }
}
