package com.idii.project_idii.project;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class ProjectService {

	private final ProjectDAO projectDAO;
	
	public ProjectService(ProjectDAO projectDAO) {
	    this.projectDAO = projectDAO;
	}

	public int addProject(Project project) {
	
	    if (project == null) {
	        return 0;
	    }
	
	    try {
	        return projectDAO.addProject(project);
	
	    } catch (Exception e) {
	        e.printStackTrace();
	        return 0;
	    }
	}
    

    public boolean canEditContent(long projectId, long userId) {

        try {
            return projectDAO.canEditContent(projectId, userId);

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    public int updateProjectStatus(long projectId, long userId,
                                   String status) {

        // 허용된 상태값만 사용
        if (!"0".equals(status) && !"1".equals(status)) {
            return 0;
        }

        // 프로젝트 관리자 권한 확인
        if (!canManageProject(projectId, userId)) {
            return 0;
        }

        try {
            return projectDAO.updateProjectStatus(projectId, userId, status);

        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

	public List<Project> getProjectList(long userId) {
	
	    try {
	        return projectDAO.getProjectList(userId);
	
	    } catch (Exception e) {
	        e.printStackTrace();
	        return new ArrayList<>();
	    }
	}

	public Project getProject(long projectId, long userId) {
	
	    try {
	        return projectDAO.getProject(projectId, userId);
	
	    } catch (Exception e) {
	        e.printStackTrace();
	        return null;
	    }
	}

	public int updateProject(Project project, long userId) {
	
	    if (project == null) {
	        return 0;
	    }
	
	    try {
	        return projectDAO.updateProject(project, userId);
	
	    } catch (Exception e) {
	        e.printStackTrace();
	        return 0;
	    }
	}

	public int deleteProject(long projectId, long userId) {
	
	    try {
	        return projectDAO.deleteProject(projectId, userId);
	
	    } catch (Exception e) {
	        e.printStackTrace();
	        return 0;
	    }
	}

	public boolean canAccessProject(long projectId, long userId) {
	
	    try {
	        return projectDAO.canAccessProject(projectId, userId);
	
	    } catch (Exception e) {
	        e.printStackTrace();
	        return false;
	    }
	}
	
	public boolean canManageProject(long projectId, long userId) {
	
	    try {
	        return projectDAO.canManageProject(projectId, userId);
	
	    } catch (Exception e) {
	        e.printStackTrace();
	        return false;
	    }
	}
	
	public String getMemberRole(long projectId, long userId) {
	
	    try {
	        return projectDAO.getMemberRole(projectId, userId);
	
	    } catch (Exception e) {
	        e.printStackTrace();
	        return null;
	    }
	}

	public Map<Long, Map<String, Long>> getProjectStats(long userId) {
	
	    try {
	        return projectDAO.getProjectStats(userId);
	
	    } catch (Exception e) {
	        throw new IllegalStateException("프로젝트 통계 조회 실패", e);
	    }
	}

}
