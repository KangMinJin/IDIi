package com.idii.project_idii.project;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.idii.project_idii.issue.Issue;
import com.idii.project_idii.issue.IssueService;
import com.idii.project_idii.testcase.TestCaseService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@RequestMapping("/project")
@Controller
public class ProjectController {
	
	private final ProjectService projectService;
	private final IssueService issueService;
	private final ProjectMemberService projectMemberService;
	private final TestCaseService testCaseService;
	
	public ProjectController(ProjectService projectService,
							IssueService issueService,
							ProjectMemberService projectMemberService,
                            TestCaseService testCaseService) {
		this.projectService = projectService;
		this.issueService = issueService;
		this.projectMemberService = projectMemberService;
		this.testCaseService = testCaseService;
	}

	@GetMapping("/create")
	public String create(HttpSession session) {
	    Long userId = (Long) session.getAttribute("userId");

	    if (userId == null) {
	        return "redirect:/user/login";
	    }

	    return "project/create";
	}
	
	@PostMapping("/create")
	public String createPro(HttpServletRequest request,
							HttpSession session,
							Model model) {
		Long userId = (Long)session.getAttribute("userId");
		
		if(userId == null) {
			return "redirect:/user/login";
		}
		
		String title = request.getParameter("title");
		String description = request.getParameter("description");
		String startDate = request.getParameter("startDate");
		String dueDate = request.getParameter("dueDate");
		
		
		
		Project project = new Project();
		project.setOwnerId(userId);
		project.setTitle(title);
		project.setDescription(description);
		try {
			project.setStartDate(startDate == null || startDate.isBlank()
					? null : LocalDate.parse(startDate));

			project.setDueDate(dueDate == null || dueDate.isBlank()
					? null : LocalDate.parse(dueDate));
			
		} catch(DateTimeParseException e) {
			model.addAttribute("error","날짜 형식이 올바르지 않습니다.");
			
			return "project/create";
		}
		
		if (project.getStartDate() != null
		        && project.getDueDate() != null
		        && project.getStartDate().isAfter(project.getDueDate())) {

		    model.addAttribute("project", project);
		    model.addAttribute("error", "마감일은 시작일보다 빠를 수 없습니다.");
		    return "project/create";
		}

		int cnt = projectService.addProject(project);
		
		if(cnt == 1) {
			return "redirect:/";
		}
			
		model.addAttribute("error", "프로젝트 생성에 실패했습니다.");
		
		return "project/create";
	}
	
	@GetMapping("/detail")
	public String detail(HttpServletRequest request,
						 HttpSession session,
						 Model model) {
		
		Long userId = (Long)session.getAttribute("userId");
		
		
		if (userId == null) {
		    return "redirect:/user/login";
		}
		
		try {
			long projectId = Long.parseLong(request.getParameter("projectId"));
			Project project = projectService.getProject(projectId, userId);
			
			String role = projectService.getMemberRole(projectId, userId);

			String roleName = switch (role == null ? "" : role) {
			    case "0" -> "관리자";
			    case "1" -> "QA";
			    case "2" -> "개발자";
			    default -> "미등록";
			};

			model.addAttribute("myRoleName", roleName);
			
			if(project == null) {
				return "redirect:/";
			}
			model.addAttribute("project",project);
			List<Issue> issues = issueService.getIssueList(projectId, userId);
			model.addAttribute("issues", issues);
            model.addAttribute("testCases", testCaseService.getTestCaseList(projectId, userId));
            model.addAttribute("canEditTc", testCaseService.canEdit(projectId, userId));
			
			// 프로젝트 멤버 목록
			List<ProjectMember> memberList =
			        projectMemberService.getMemberList(projectId, userId);

			model.addAttribute("memberList", memberList);
			
			// 멤버 관리 권한
			model.addAttribute("canManage",
			        projectService.canManageProject(projectId, userId));
            model.addAttribute("isCompleted", "1".equals(project.getStatus()));
			
		} catch (NumberFormatException e) {
			return "redirect:/";
		}
		return "project/detail";
	}

	
	@GetMapping("/edit")
    public String update(HttpServletRequest request,
    					 HttpSession session,
    					 Model model) {
		
		Long userId = (Long)session.getAttribute("userId");
		
		if(userId == null) {
			return "redirect:/user/login";
		}
		
		long projectId;
		
		try {
	        projectId = Long.parseLong(request.getParameter("projectId"));
	    } catch (NumberFormatException e) {
	        return "redirect:/";
	    }
		
		Project project = projectService.getProject(projectId, userId);
		
		if(project == null) {
			return "redirect:/";
		}
		
		// 프로젝트 생성자 아니면 수정 막기
		if (project.getOwnerId() != userId) {
		    return "redirect:/project/detail?projectId=" + projectId;
		}
		
		model.addAttribute("project", project);
		
        return "project/edit";
    }
	
	@PostMapping("/edit")
	public String updatePro(HttpServletRequest request,
							HttpSession session,
							Model model) {
		Long userId = (Long)session.getAttribute("userId");
		
		if(userId == null) {
			return "redirect:/user/login";
		}
		
		long projectId;
		
		try {
	        projectId = Long.parseLong(request.getParameter("projectId"));
	    } catch (NumberFormatException e) {
	        return "redirect:/";
	    }
		
		// 소유자 권한 및 프로젝트 존재 여부 확인(이중확인!)
	    Project existingProject =
	            projectService.getProject(projectId, userId);

	    if (existingProject == null) {
	        return "redirect:/";
	    }
	    
	    // 프로젝트 생성자 아니면 수정 막기
	    if (existingProject.getOwnerId() != userId) {
	        return "redirect:/project/detail?projectId=" + projectId;
	    }
		
		String title = request.getParameter("title");
		String description = request.getParameter("description");
		String startDate = request.getParameter("startDate");
		String dueDate = request.getParameter("dueDate");
		
		
		
		Project project = new Project();
		project.setProjectId(projectId);
		project.setTitle(title);
		project.setDescription(description);
		try {
			project.setStartDate(startDate == null || startDate.isBlank()
					? null : LocalDate.parse(startDate));

			project.setDueDate(dueDate == null || dueDate.isBlank()
					? null : LocalDate.parse(dueDate));
			
		} catch(DateTimeParseException e) {
			model.addAttribute("project", project);
			model.addAttribute("error","날짜 형식이 올바르지 않습니다.");
			
			return "project/edit";
		}
		
		if (title == null || title.isBlank()) {
		    model.addAttribute("project", project);
		    model.addAttribute("error", "프로젝트 제목을 입력해주세요.");
		    return "project/edit";
		}

		if (project.getStartDate() != null
		        && project.getDueDate() != null
		        && project.getStartDate().isAfter(project.getDueDate())) {

		    model.addAttribute("project", project);
		    model.addAttribute("error", "마감일은 시작일보다 빠를 수 없습니다.");
		    return "project/edit";
		}

		int cnt = projectService.updateProject(project, userId);
		
		if(cnt == 1) {
			return "redirect:/project/detail?projectId=" + projectId;
		}
			
		model.addAttribute("project", project);
		model.addAttribute("error", "프로젝트 수정에 실패했습니다.");
		
		return "project/edit";
	}
	
	@PostMapping("/delete")
    public String delete(HttpServletRequest request,
    					 HttpSession session,
    					 Model model) {
		
		Long userId = (Long)session.getAttribute("userId");
		
		if(userId == null) {
			return "redirect:/user/login";
		}
		
		long projectId;
		
		try {
	        projectId = Long.parseLong(request.getParameter("projectId"));
	    } catch (NumberFormatException e) {
	        return "redirect:/";
	    }
		
		int cnt = projectService.deleteProject(projectId, userId);
		
		if(cnt == 1) {
			return "redirect:/";
		}
		
		return "redirect:/project/detail?projectId=" + projectId;
    }
    @PostMapping("/status")
    public String changeStatus(HttpServletRequest request, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return "redirect:/user/login";
        try {
            long projectId = Long.parseLong(request.getParameter("projectId"));
            projectService.updateProjectStatus(projectId, userId, request.getParameter("status"));
            return "redirect:/project/detail?projectId=" + projectId;
        } catch (NumberFormatException e) { return "redirect:/"; }
    }
}
