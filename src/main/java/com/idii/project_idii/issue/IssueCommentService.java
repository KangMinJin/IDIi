package com.idii.project_idii.issue;

import java.util.List;

import org.springframework.stereotype.Service;

import com.idii.project_idii.project.ProjectService;

@Service
public class IssueCommentService {

    private final IssueCommentDAO issueCommentDAO;
    private final IssueService issueService;
    private final ProjectService projectService;

    public IssueCommentService(
            IssueCommentDAO issueCommentDAO,
            IssueService issueService,
            ProjectService projectService) {

        this.issueCommentDAO = issueCommentDAO;
        this.issueService = issueService;
        this.projectService = projectService;
    }

    // 댓글 등록
    public int addComment(long issueId, long userId, String content) {

        if (content == null
                || content.isBlank()
                || content.length() > 500) {
            return 0;
        }

        // 이슈 조회 및 프로젝트 접근 권한 확인
        Issue issue = issueService.getIssue(issueId, userId);

        if (issue == null) {
            return 0;
        }

        // 댓글 작성 권한 확인
        if (!projectService.canEditContent(
                issue.getProjectId(), userId)) {
            return 0;
        }

        try {
            return issueCommentDAO.addComment(
                    issueId, userId, content);

        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    // 댓글 목록 조회
    public List<IssueComment> getCommentList(
            long issueId, long userId) {

        // 접근할 수 없는 이슈의 댓글은 조회 불가
        Issue issue = issueService.getIssue(issueId, userId);

        if (issue == null) {
            return null;
        }

        try {
            return issueCommentDAO.getCommentList(issueId);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
