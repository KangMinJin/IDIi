package com.idii.project_idii.testcase;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.idii.project_idii.project.ProjectService;

@Service
public class TestCaseService {

    private final ProjectService projectService;
    private final TestCaseDAO testCaseDAO;

    public TestCaseService(
            ProjectService projectService,
            TestCaseDAO testCaseDAO) {

        this.projectService = projectService;
        this.testCaseDAO = testCaseDAO;
    }

    // 테스트케이스 수정 권한 확인
    public boolean canEdit(long projectId, long userId) {
        return projectService.canEditContent(projectId, userId);
    }

    // 테스트케이스 입력값 검증
    private boolean valid(TestCase tc, boolean create) {

        if (tc == null
                || tc.getTitle() == null
                || tc.getTitle().isBlank()
                || tc.getTitle().length() > 200) {
            return false;
        }

        if (create && (tc.getCaseCode() == null
                || tc.getCaseCode().isBlank()
                || tc.getCaseCode().length() > 30)) {
            return false;
        }

        return tc.getResult() != null
                && List.of("0", "1", "2", "3", "4")
                          .contains(tc.getResult());
    }

    // 테스트케이스 목록 조회
    public List<TestCase> getTestCaseList(
            long projectId, long userId) {

        if (!projectService.canAccessProject(projectId, userId)) {
            return new ArrayList<>();
        }

        try {
            return testCaseDAO.getTestCaseList(projectId);

        } catch (Exception e) {
            throw new IllegalStateException("TC 목록 조회 실패", e);
        }
    }

    // 테스트케이스 상세 조회
    public TestCase getTestCase(long testCaseId, long userId) {

        try {
            TestCase tc = testCaseDAO.getTestCase(testCaseId);

            if (tc == null) {
                return null;
            }

            if (!projectService.canAccessProject(
                    tc.getProjectId(), userId)) {
                return null;
            }

            return tc;

        } catch (Exception e) {
            throw new IllegalStateException("TC 조회 실패", e);
        }
    }

    // 테스트케이스 등록
    public int addTestCase(TestCase tc, long userId) {

        if (!valid(tc, true)
                || !canEdit(tc.getProjectId(), userId)) {
            return 0;
        }

        try {
            return testCaseDAO.addTestCase(tc, userId);

        } catch (SQLIntegrityConstraintViolationException e) {
            // 기존 동작 유지: 중복 코드 등 제약조건 위반
            return -1;

        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    // 테스트케이스 수정
    public int updateTestCase(TestCase tc, long userId) {

        if (!valid(tc, false)
                || !canEdit(tc.getProjectId(), userId)) {
            return 0;
        }

        try {
            return testCaseDAO.updateTestCase(tc);

        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    // 테스트케이스 삭제
    public int deleteTestCase(
            long testCaseId, long projectId, long userId) {

        if (!canEdit(projectId, userId)) {
            return 0;
        }

        try {
            return testCaseDAO.deleteTestCase(testCaseId, projectId);

        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    // 테스트 결과 변경
    public int updateTestCaseResult(
            long testCaseId, String result, long userId) {

        if (result == null
                || !List.of("0", "1", "2", "3", "4")
                          .contains(result)) {
            return 0;
        }

        TestCase existing = getTestCase(testCaseId, userId);

        if (existing == null
                || !canEdit(existing.getProjectId(), userId)) {
            return 0;
        }

        try {
            return testCaseDAO.updateTestCaseResult(
                    testCaseId, existing.getProjectId(), result);

        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
}
