package com.idii.project_idii.issue;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.idii.project_idii.project.ProjectService;
import com.idii.project_idii.project.ProjectMemberService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/issue")
public class IssueController {
	private final IssueService issueService;
	private final IssueCommentService issueCommentService;
	private final ProjectService projectService;
    private final ProjectMemberService projectMemberService;

	public IssueController(IssueService issueService,
	                       IssueCommentService issueCommentService,
	                       ProjectService projectService, ProjectMemberService projectMemberService) {

	    this.issueService = issueService;
	    this.issueCommentService = issueCommentService;
	    this.projectService = projectService;
        this.projectMemberService = projectMemberService;
	}
	
	@GetMapping("/create")
	public String create (HttpServletRequest request, HttpSession session, Model model) {
		
		Long userId = (Long)session.getAttribute("userId");
		
		if(userId == null) {
			return "redirect:/user/login";
		}
		
		try {
			long projectId = Long.parseLong(request.getParameter("projectId"));
			
			if (!projectService.canEditContent(projectId, userId)) {
			    return "redirect:/";
			}
			
			model.addAttribute("projectId", projectId);
            model.addAttribute("memberList", projectMemberService.getMemberList(projectId, userId));
			
		} catch (NumberFormatException e) {
			e.printStackTrace();
			return "redirect:/";
		} 
		return "issue/create";
	}
	
	@PostMapping("/create")
	public String createPro (HttpServletRequest request,
							 HttpSession session,
							 Model model) {
		
		Long userId = (Long)session.getAttribute("userId");
		
		if(userId == null) {
			return "redirect:/user/login";
		}
		
		try {
	        // 1. 프로젝트 ID 가져오기 (URL 파라미터)
	        long projectId = Long.parseLong(
	                request.getParameter("projectId"));

	        // 2. 사용자가 입력한 값 가져오기
	        String title = request.getParameter("title");
	        String content = request.getParameter("content");
	        String type = request.getParameter("type");
	        String priority = request.getParameter("priority");
	        String dueDate = request.getParameter("dueDate");

	        // 3. Issue 객체 생성
	        Issue issue = new Issue();

	        issue.setProjectId(projectId);
	        issue.setTitle(title);
	        issue.setContent(content);
	        issue.setType(type);
	        issue.setPriority(priority);
            issue.setAssigneeId(parseAssignee(request.getParameter("assigneeId")));

	        // 4. 마감일이 입력된 경우에만 날짜 변환
	        if (dueDate != null && !dueDate.isBlank()) {
	            issue.setDueDate(LocalDate.parse(dueDate));
	        }

	        // 5. DB에 저장
        int result = issueService.addIssue(issue, userId);
        if (result > 0) return "redirect:/project/detail?projectId=" + projectId;
        return "redirect:/issue/create?projectId=" + projectId;

    } catch (NumberFormatException | DateTimeParseException e) {
	        return "redirect:/";
	    }
	}
	
	@GetMapping("/detail")
	public String detail(HttpServletRequest request,
	                     HttpSession session,
	                     Model model) {

	    Long userId = (Long) session.getAttribute("userId");

	    if (userId == null) {
	        return "redirect:/user/login";
	    }

	    try {
	        long issueId = Long.parseLong(request.getParameter("issueId"));

	        Issue issue = issueService.getIssue(issueId, userId);

	        if (issue == null) {
	            return "redirect:/";
	        }
	        
	        model.addAttribute("issue", issue);
            model.addAttribute("projectCompleted", !projectService.canEditContent(issue.getProjectId(), userId));
            model.addAttribute("memberList", projectMemberService.getMemberList(issue.getProjectId(), userId));

	        List<IssueComment> comments =
	                issueCommentService.getCommentList(issueId, userId);

	        model.addAttribute("comments", comments);

	    } catch (NumberFormatException e) {
	        return "redirect:/";
	    }

	    return "issue/detail";
	}
	
	@GetMapping("/edit")
	public String edit(HttpServletRequest request,
	                     HttpSession session,
	                     Model model) {

	    Long userId = (Long) session.getAttribute("userId");

	    if (userId == null) {
	        return "redirect:/user/login";
	    }

	    try {
	        long issueId = Long.parseLong(request.getParameter("issueId"));

	        Issue issue = issueService.getIssue(issueId, userId);

	        if (issue == null || !projectService.canEditContent(issue.getProjectId(), userId)) {
	            return "redirect:/";
	        }

	        model.addAttribute("issue", issue);
            model.addAttribute("memberList", projectMemberService.getMemberList(issue.getProjectId(), userId));

	    } catch (NumberFormatException e) {
	        return "redirect:/";
	    }

	    return "issue/edit";
	}
	
	@PostMapping("/edit")
	public String updatePro(HttpServletRequest request,
	                        HttpSession session,
	                        Model model) {
	
	    Long userId = (Long) session.getAttribute("userId");
	
	    if (userId == null) {
	        return "redirect:/user/login";
	    }
	
	    long issueId;
	
	    try {
	        issueId = Long.parseLong(request.getParameter("issueId"));
	    } catch (NumberFormatException e) {
	        return "redirect:/";
	    }
	
	    // 기존 이슈 조회 및 접근 권한 확인
	    Issue existingIssue = issueService.getIssue(issueId, userId);
	
	    if (existingIssue == null) {
	        return "redirect:/";
	    }
	
	    // 수정할 값 가져오기
	    String title = request.getParameter("title");
	    String content = request.getParameter("content");
	    String type = request.getParameter("type");
	    String dueDate = request.getParameter("dueDate");
	
	    // 기존 이슈 객체를 사용해 수정할 값만 변경
	    Issue issue = existingIssue;
	
	    issue.setTitle(title);
	    issue.setContent(content);
	    issue.setType(type);
        issue.setAssigneeId(parseAssignee(request.getParameter("assigneeId")));
	
	    try {
	        issue.setDueDate(
	            dueDate == null || dueDate.isBlank()
	                ? null : LocalDate.parse(dueDate)
	        );
	    } catch (DateTimeParseException e) {
	        model.addAttribute("issue", issue);
            model.addAttribute("memberList", projectMemberService.getMemberList(issue.getProjectId(), userId));
	        model.addAttribute("error", "날짜 형식이 올바르지 않습니다.");
	        return "issue/edit";
	    }
	
	    // 제목 검사
	    if (title == null || title.isBlank() || title.length() > 100) {
	        model.addAttribute("issue", issue);
            model.addAttribute("memberList", projectMemberService.getMemberList(issue.getProjectId(), userId));
	        model.addAttribute("error", "제목은 1~100자로 입력해주세요.");
	        return "issue/edit";
	    }
	
	    // 유형 검사
	    if (!"0".equals(type) && !"1".equals(type)) {
	        model.addAttribute("issue", issue);
            model.addAttribute("memberList", projectMemberService.getMemberList(issue.getProjectId(), userId));
	        model.addAttribute("error", "올바르지 않은 이슈 유형입니다.");
	        return "issue/edit";
	    }
	
	    int cnt = issueService.updateIssue(issue, userId);
	
	    if (cnt == 1) {
	        return "redirect:/issue/detail?issueId=" + issueId;
	    }
	
	    model.addAttribute("issue", issue);
            model.addAttribute("memberList", projectMemberService.getMemberList(issue.getProjectId(), userId));
	    model.addAttribute("error", "이슈 수정에 실패했습니다.");
	
	    return "issue/edit";
	}
	
	@PostMapping("/delete")
	public String delete(HttpServletRequest request,
	                     HttpSession session) {

	    Long userId = (Long) session.getAttribute("userId");

	    if (userId == null) {
	        return "redirect:/user/login";
	    }

	    long issueId;

	    try {
	        issueId = Long.parseLong(request.getParameter("issueId"));
	    } catch (NumberFormatException e) {
	        return "redirect:/";
	    }

	    Issue issue = issueService.getIssue(issueId, userId);

	    if (issue == null) {
	        return "redirect:/";
	    }

	    int cnt = issueService.deleteIssue(issueId, userId);

	    if (cnt == 1) {
	        return "redirect:/project/detail?projectId="
	                + issue.getProjectId();
	    }

	    return "redirect:/issue/detail?issueId=" + issueId;
	}
	


	@PostMapping("/comment/create")
	public String addComment(HttpServletRequest request,
	                         HttpSession session,
	                         RedirectAttributes redirectAttributes) {
	
	    Long userId = (Long) session.getAttribute("userId");
	
	    if (userId == null) {
	        return "redirect:/user/login";
	    }
	
	    long issueId;
	
	    try {
	        issueId = Long.parseLong(request.getParameter("issueId"));
	    } catch (NumberFormatException e) {
	        return "redirect:/";
	    }
	
	    String content = request.getParameter("content");
	
	    // 댓글 유효성 검사
	    if (content == null || content.isBlank()) {
	        redirectAttributes.addFlashAttribute(
	            "commentError", "댓글 내용을 입력해주세요."
	        );
	
	        return "redirect:/issue/detail?issueId=" + issueId + "#comments";
	    }
	
	    if (content.length() > 500) {
	        redirectAttributes.addFlashAttribute(
	            "commentError", "댓글은 500자 이내로 입력해주세요."
	        );
	
	        return "redirect:/issue/detail?issueId=" + issueId + "#comments";
	    }
	
	    int cnt = issueCommentService.addComment(issueId, userId, content);
	
	    if (cnt == 1) {
	        return "redirect:/issue/detail?issueId=" + issueId + "#comments";
	    }
	
	    // DB 저장 실패
	    redirectAttributes.addFlashAttribute(
	        "commentError", "댓글 등록에 실패했습니다."
	    );
	
	    return "redirect:/issue/detail?issueId=" + issueId + "#comments";
	}

    @PostMapping("/status")
    public String changeStatus(HttpServletRequest request, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return "redirect:/user/login";
        try {
            long issueId = Long.parseLong(request.getParameter("issueId"));
            Issue issue = issueService.getIssue(issueId, userId);
            if (issue == null) return "redirect:/";
            issueService.updateIssueStatus(issueId, request.getParameter("status"), userId);
            return "redirect:/project/detail?projectId=" + issue.getProjectId() + "#issues-title";
        } catch (NumberFormatException e) {
            return "redirect:/";
        }
    }

    @PostMapping("/priority")
    public String changePriority(HttpServletRequest request, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return "redirect:/user/login";
        try {
            long issueId = Long.parseLong(request.getParameter("issueId"));
            Issue issue = issueService.getIssue(issueId, userId);
            if (issue == null) return "redirect:/";
            issueService.updateIssuePriority(issueId, request.getParameter("priority"), userId);
            return "redirect:/project/detail?projectId=" + issue.getProjectId() + "#issues-title";
        } catch (NumberFormatException e) {
            return "redirect:/";
        }
    }
    private Long parseAssignee(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try { return Long.parseLong(raw); }
        catch (NumberFormatException e) { return -1L; }
    }

}
