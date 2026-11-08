package com.idii.project_idii.project;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class ProjectMemberService {

    private final ProjectService projectService;
    private final ProjectMemberDAO projectMemberDAO;

    public ProjectMemberService(
            ProjectService projectService,
            ProjectMemberDAO projectMemberDAO) {

        this.projectService = projectService;
        this.projectMemberDAO = projectMemberDAO;
    }

    // 프로젝트 멤버 목록
    public List<ProjectMember> getMemberList(long projectId, long userId) {

        if (!projectService.canAccessProject(projectId, userId)) {
            return null;
        }

        try {
            return projectMemberDAO.getMemberList(projectId);

        } catch (Exception e) {
            throw new IllegalStateException("프로젝트 멤버 조회 실패", e);
        }
    }

    // 프로젝트 멤버 추가
    public int addMember(long projectId, String email,
                         String role, long userId) {

        if (!projectService.canManageProject(projectId, userId)
                || !projectService.canEditContent(projectId, userId)) {
            return 0;
        }

        if (email == null || email.isBlank()) {
            return 0;
        }

        if (!"1".equals(role) && !"2".equals(role)) {
            return 0;
        }

        try {
            return projectMemberDAO.addMember(projectId, email, role);

        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    // 프로젝트 멤버 삭제
    public int deleteMember(long projectId, long memberId, long userId) {

        if (!projectService.canManageProject(projectId, userId)
                || !projectService.canEditContent(projectId, userId)) {
            return 0;
        }

        try {
            return projectMemberDAO.deleteMember(projectId, memberId);

        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
}
