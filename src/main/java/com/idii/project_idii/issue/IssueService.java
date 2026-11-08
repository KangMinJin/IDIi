package com.idii.project_idii.issue;

import java.util.List;

import org.springframework.stereotype.Service;

import com.idii.project_idii.project.ProjectService;
import com.idii.project_idii.project.ProjectMember;
import com.idii.project_idii.project.ProjectMemberService;


@Service
public class IssueService {

	private final ProjectService projectService;
	private final ProjectMemberService projectMemberService;
	private final IssueDAO issueDAO;
	
	public IssueService(
	        ProjectService projectService,
	        ProjectMemberService projectMemberService,
	        IssueDAO issueDAO) {
	
	    this.projectService = projectService;
	    this.projectMemberService = projectMemberService;
	    this.issueDAO = issueDAO;
	}

	public int addIssue(Issue issue, long userId) {
	
	    // 이슈 유효성 검사
	    if (issue == null) {
	        return 0;
	    }
	
	    if (issue.getTitle() == null
	            || issue.getTitle().isBlank()
	            || issue.getTitle().length() > 100) {
	        return 0;
	    }
	
	    if (!"0".equals(issue.getType())
	            && !"1".equals(issue.getType())) {
	        return 0;
	    }
	
	    if (!"0".equals(issue.getPriority())
	            && !"1".equals(issue.getPriority())
	            && !"2".equals(issue.getPriority())) {
	        return 0;
	    }
	
	    // 프로젝트가 진행 중이며 접근 가능한지 확인
	    if (!projectService.canEditContent(
	            issue.getProjectId(), userId)) {
	        return 0;
	    }
	
	    // 담당자가 해당 프로젝트의 멤버인지 확인
	    if (!validAssignee(
	            issue.getProjectId(), issue.getAssigneeId(), userId)) {
	        return 0;
	    }
	
	    try {
	        return issueDAO.addIssue(issue, userId);
	
	    } catch (Exception e) {
	        e.printStackTrace();
	        return 0;
	    }
	}

	public List<Issue> getIssueList(long projectId, long userId) {
	
	    // 프로젝트 접근 권한 확인
	    if (!projectService.canAccessProject(projectId, userId)) {
	        return null;
	    }
	
	    try {
	        return issueDAO.getIssueList(projectId);
	
	    } catch (Exception e) {
	        throw new IllegalStateException("이슈 목록 조회 중 오류 발생", e);
	    }
	}

	public Issue getIssue(long issueId, long userId) {
	
	    try {
	        Issue issue = issueDAO.getIssue(issueId);
	
	        if (issue == null) {
	            return null;
	        }
	
	        // 조회된 이슈의 프로젝트 접근 권한 확인
	        if (!projectService.canAccessProject(
	                issue.getProjectId(), userId)) {
	            return null;
	        }
	
	        return issue;
	
	    } catch (Exception e) {
	        throw new IllegalStateException("이슈 상세 조회 실패", e);
	    }
	}

	public int updateIssue(Issue issue, long userId) {
	
	    if (issue == null) {
	        return 0;
	    }
	
	    if (issue.getTitle() == null
	            || issue.getTitle().isBlank()
	            || issue.getTitle().length() > 100) {
	        return 0;
	    }
	
	    if (!"0".equals(issue.getType())
	            && !"1".equals(issue.getType())) {
	        return 0;
	    }
	
	    Issue existingIssue = getIssue(issue.getIssueId(), userId);
	
	    if (existingIssue == null) {
	        return 0;
	    }
	
	    if (existingIssue.getProjectId() != issue.getProjectId()) {
	        return 0;
	    }
	
	    if (!projectService.canEditContent(issue.getProjectId(), userId)) {
	        return 0;
	    }
	
	    if (!validAssignee(
	            issue.getProjectId(), issue.getAssigneeId(), userId)) {
	        return 0;
	    }
	
	    try {
	        return issueDAO.updateIssue(issue);
	
	    } catch (Exception e) {
	        e.printStackTrace();
	        return 0;
	    }
	}



	public int deleteIssue(long issueId, long userId) {
	
	    Issue issue = getIssue(issueId, userId);
	
	    if (issue == null
	            || !projectService.canEditContent(
	                    issue.getProjectId(), userId)) {
	        return 0;
	    }
	
	    try {
	        return issueDAO.deleteIssue(issueId, issue.getProjectId());
	
	    } catch (Exception e) {
	        e.printStackTrace();
	        return 0;
	    }
	}



	// 이슈 상태 변경
	public int updateIssueStatus(long issueId, String status, long userId) {

	    if (!List.of("0", "1", "2", "3").contains(status)) {
	        return 0;
	    }

	    return updateIssueField(issueId, "status", status, userId);
	}

	// 이슈 우선순위 변경
	public int updateIssuePriority(long issueId, String priority, long userId) {

	    if (!List.of("0", "1", "2").contains(priority)) {
	        return 0;
	    }

	    return updateIssueField(issueId, "priority", priority, userId);
	}

	// 상태·우선순위 공통 처리
	private int updateIssueField(long issueId, String column,
	                             String value, long userId) {

	    Issue issue = getIssue(issueId, userId);

	    if (issue == null
	            || !projectService.canEditContent(
	                    issue.getProjectId(), userId)) {
	        return 0;
	    }

	    try {
	        return issueDAO.updateIssueField(
	                issueId, issue.getProjectId(), column, value);

	    } catch (Exception e) {
	        e.printStackTrace();
	        return 0;
	    }
	}

    private boolean validAssignee(long projectId, Long assigneeId, long userId) {
    	
        if (assigneeId == null) {
        	return true;
        }
        
        List<ProjectMember> members = projectMemberService.getMemberList(projectId, userId);
        
        return members != null && members.stream().anyMatch(m -> m.getUserId() == assigneeId);
    }

}
