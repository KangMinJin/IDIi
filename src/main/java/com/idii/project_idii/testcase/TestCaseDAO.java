package com.idii.project_idii.testcase;

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
public class TestCaseDAO {

    private final DataSource dataSource;

    public TestCaseDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // 테스트케이스 목록 조회
    public List<TestCase> getTestCaseList(long projectId)
            throws SQLException {

        String sql = "SELECT tc.*, u.nickname AS author_nickname "
                   + "FROM test_case tc "
                   + "LEFT JOIN users u ON u.user_id = tc.created_by "
                   + "WHERE tc.project_id = ? "
                   + "ORDER BY tc.test_case_id DESC";

        List<TestCase> list = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, projectId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapTestCase(rs));
                }
            }
        }

        return list;
    }

    // 테스트케이스 상세 조회
    public TestCase getTestCase(long testCaseId)
            throws SQLException {

        String sql = "SELECT tc.*, u.nickname AS author_nickname "
                   + "FROM test_case tc "
                   + "LEFT JOIN users u ON u.user_id = tc.created_by "
                   + "WHERE tc.test_case_id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, testCaseId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapTestCase(rs);
                }
            }
        }

        return null;
    }

    // 테스트케이스 등록
    public int addTestCase(TestCase tc, long userId)
            throws SQLException {

        String sql = "INSERT INTO test_case "
                   + "(project_id, created_by, case_code, title, "
                   + "precondition, test_steps, expected_result, "
                   + "result, remarks, executed_at) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, "
                   + "CASE WHEN ? = '4' "
                   + "THEN NULL ELSE CURRENT_TIMESTAMP END)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, tc.getProjectId());
            pstmt.setLong(2, userId);
            pstmt.setString(3, tc.getCaseCode().trim());
            pstmt.setString(4, tc.getTitle().trim());
            pstmt.setString(5, tc.getPrecondition());
            pstmt.setString(6, tc.getTestSteps());
            pstmt.setString(7, tc.getExpectedResult());
            pstmt.setString(8, tc.getResult());
            pstmt.setString(9, tc.getRemarks());
            pstmt.setString(10, tc.getResult());

            return pstmt.executeUpdate();
        }
    }

    // 테스트케이스 수정
    public int updateTestCase(TestCase tc)
            throws SQLException {

        String sql = "UPDATE test_case SET "
                   + "title = ?, precondition = ?, test_steps = ?, "
                   + "expected_result = ?, "
                   + "executed_at = CASE WHEN ? = '4' "
                   + "THEN NULL ELSE CURRENT_TIMESTAMP END, "
                   + "result = ?, remarks = ? "
                   + "WHERE test_case_id = ? AND project_id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, tc.getTitle().trim());
            pstmt.setString(2, tc.getPrecondition());
            pstmt.setString(3, tc.getTestSteps());
            pstmt.setString(4, tc.getExpectedResult());
            pstmt.setString(5, tc.getResult());
            pstmt.setString(6, tc.getResult());
            pstmt.setString(7, tc.getRemarks());
            pstmt.setLong(8, tc.getTestCaseId());
            pstmt.setLong(9, tc.getProjectId());

            return pstmt.executeUpdate();
        }
    }

    // 테스트케이스 삭제
    public int deleteTestCase(long testCaseId, long projectId)
            throws SQLException {

        String sql = "DELETE FROM test_case "
                   + "WHERE test_case_id = ? AND project_id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, testCaseId);
            pstmt.setLong(2, projectId);

            return pstmt.executeUpdate();
        }
    }

    // 테스트 결과 변경
    public int updateTestCaseResult(
            long testCaseId, long projectId, String result)
            throws SQLException {

        String sql = "UPDATE test_case SET "
                   + "executed_at = CASE WHEN ? = '4' "
                   + "THEN NULL ELSE CURRENT_TIMESTAMP END, "
                   + "result = ? "
                   + "WHERE test_case_id = ? AND project_id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, result);
            pstmt.setString(2, result);
            pstmt.setLong(3, testCaseId);
            pstmt.setLong(4, projectId);

            return pstmt.executeUpdate();
        }
    }

    // ResultSet → TestCase 객체 변환
    private TestCase mapTestCase(ResultSet rs)
            throws SQLException {

        TestCase tc = new TestCase();

        tc.setTestCaseId(rs.getLong("test_case_id"));
        tc.setProjectId(rs.getLong("project_id"));
        tc.setCreatedBy(rs.getLong("created_by"));
        tc.setCaseCode(rs.getString("case_code"));
        tc.setTitle(rs.getString("title"));
        tc.setPrecondition(rs.getString("precondition"));
        tc.setTestSteps(rs.getString("test_steps"));
        tc.setExpectedResult(rs.getString("expected_result"));
        tc.setResult(rs.getString("result"));
        tc.setRemarks(rs.getString("remarks"));
        tc.setCreatedAt(
                rs.getObject("created_at", LocalDateTime.class));
        tc.setUpdatedAt(
                rs.getObject("updated_at", LocalDateTime.class));
        tc.setExecutedAt(
                rs.getObject("executed_at", LocalDateTime.class));
        tc.setAuthorNickname(rs.getString("author_nickname"));

        return tc;
    }
}
