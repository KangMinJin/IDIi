
package com.idii.project_idii.main;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.idii.project_idii.project.Project;
import com.idii.project_idii.project.ProjectService;

import jakarta.servlet.http.HttpSession;

@Controller
public class MainController {

    private final ProjectService projectService;

    public MainController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping("/")
    public String index(HttpSession session, Model model, @RequestParam(name="tab", required=false) String requestedTab,
                        @RequestParam(name="page", defaultValue="1") int requestedPage) {

        Long userId = (Long) session.getAttribute("userId");

        model.addAttribute("isLogin", userId != null);

        if (userId != null) {
            List<Project> allProjects = projectService.getProjectList(userId);
            List<Project> activeProjects = allProjects.stream().filter(p -> !"1".equals(p.getStatus())).toList();
            List<Project> completedProjects = allProjects.stream().filter(p -> "1".equals(p.getStatus())).toList();
            String tab = "completed".equals(requestedTab) ? "completed" : "active";
            List<Project> tabProjects = "completed".equals(tab) ? completedProjects : activeProjects;
            final int pageSize = 6;
            int totalPages = Math.max(1, (tabProjects.size() + pageSize - 1) / pageSize);
            int page = Math.max(1, Math.min(requestedPage, totalPages));
            int start = (page - 1) * pageSize;
            List<Project> projectList = tabProjects.subList(start, Math.min(start + pageSize, tabProjects.size()));
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("hasPreviousPage", page > 1);
            model.addAttribute("hasNextPage", page < totalPages);
            model.addAttribute("projectTab", tab);
            model.addAttribute("activeProjectCount", activeProjects.size());
            model.addAttribute("completedProjectCount", completedProjects.size());

            model.addAttribute("projectList", projectList);

            // 프로젝트별 현재 로그인한 사용자의 역할
            Map<Long, String> projectRoles = new HashMap<>();

            for (Project project : allProjects) {
                String role = projectService.getMemberRole(
                    project.getProjectId(), userId
                );

                String roleName = switch (role == null ? "" : role) {
                    case "0" -> "관리자";
                    case "1" -> "QA";
                    case "2" -> "개발자";
                    default -> "미등록";
                };

                projectRoles.put(project.getProjectId(), roleName);
            }

            model.addAttribute("projectRoles", projectRoles);
            
         // 프로젝트별 통계
            Map<Long, Map<String, Long>> projectStats =
                projectService.getProjectStats(userId);

            model.addAttribute("projectStats", projectStats);

            // 전체 프로젝트 합산
            long totalIssues = 0;
            long totalProgress = 0;
            long totalWaiting = 0;
            long totalReview = 0;
            long totalDone = 0;
            long totalTc = 0;
            long totalPass = 0;
            long totalFail = 0;
            long totalBlock = 0;
            long totalNa = 0;
            long totalNoRun = 0;

            for (Project active : activeProjects) {
                Map<String, Long> stat = projectStats.get(active.getProjectId());
                if (stat == null) continue;
                totalIssues += stat.get("issues");
                totalProgress += stat.get("progress");
                totalWaiting += stat.get("waiting");
                totalReview += stat.get("review");
                totalDone += stat.get("done");
                totalTc += stat.get("tc");
                totalPass += stat.get("pass");
                totalFail += stat.get("fail");
                totalBlock += stat.get("block");
                totalNa += stat.get("na");
                totalNoRun += stat.get("noRun");
            }

            model.addAttribute("totalIssues", totalIssues);
            model.addAttribute("totalProgress", totalProgress);
            model.addAttribute("totalWaiting", totalWaiting);
            model.addAttribute("totalReview", totalReview);
            model.addAttribute("issueCompletionRate", totalIssues > 0 ? totalDone * 100 / totalIssues : 0);
            model.addAttribute("totalDone", totalDone);
            model.addAttribute("totalTc", totalTc);
            model.addAttribute("totalPass", totalPass);
            model.addAttribute("totalFail", totalFail);
            model.addAttribute("totalBlock", totalBlock);
            model.addAttribute("totalNa", totalNa);
            model.addAttribute("totalNoRun", totalNoRun);
        }

        return "index";
    }
}
