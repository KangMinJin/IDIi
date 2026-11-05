package com.idii.project_idii.testcase;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/testcase")
public class TestCaseController {
    private final TestCaseService testCaseService;
    
    public TestCaseController(TestCaseService testCaseService) {
    	this.testCaseService=testCaseService;
    	}
    private long id(HttpServletRequest request,String key) {
        try {
        		return Long.parseLong(request.getParameter(key));
    		
        } catch(Exception e){
    			return -1;
		}
    }
    private String projectUrl(long projectId) {
    	return "redirect:/project/detail?projectId="+projectId;
    	}
    
    private TestCase form(HttpServletRequest request) {
        TestCase tc = new TestCase();
    
        tc.setProjectId(id(request,"projectId"));
        tc.setTestCaseId(id(request,"testCaseId"));
        tc.setCaseCode(request.getParameter("caseCode"));
        tc.setTitle(request.getParameter("title"));
        tc.setPrecondition(request.getParameter("precondition")); 
        tc.setTestSteps(request.getParameter("testSteps"));
        tc.setExpectedResult(request.getParameter("expectedResult")); 
        tc.setResult(request.getParameter("result"));
        tc.setRemarks(request.getParameter("remarks")); 
        
        return tc;
    }
    @GetMapping("/create")
    public String create(HttpServletRequest request,HttpSession session,Model model) {
        Long userId = (Long)session.getAttribute("userId");
        
        if(userId == null) {
        	return "redirect:/user/login";
        }
        
        long projectId = id(request,"projectId");
        
        if(!testCaseService.canEdit(projectId,userId)) {
        	return "redirect:/";
        }
        
        TestCase tc = new TestCase(); 
        tc.setProjectId(projectId); tc.setResult("4");
        
        model.addAttribute("tc",tc);
        
        model.addAttribute("editing",false); 
        
        return "testcase/form";
    }
    @PostMapping("/create")
    public String createPro(HttpServletRequest request,HttpSession session,Model model,RedirectAttributes redirect) {
        
    	Long userId = (Long)session.getAttribute("userId"); 
        
        if(userId == null) {
        	return "redirect:/user/login";
        	}
        
        TestCase tc = form(request);
        
        if(!testCaseService.canEdit(tc.getProjectId(),userId)) {
        	return "redirect:/";
        }
        int count = testCaseService.addTestCase(tc,userId);
        if(count == 1) {
        	return projectUrl(tc.getProjectId());
        }
        
        model.addAttribute("tc",tc); 
        
        model.addAttribute("editing",false);
        
        model.addAttribute("error",count == -1 ? "이 프로젝트에 이미 등록된 TC 코드입니다." : "등록에 실패했습니다. 입력값을 확인해 주세요.");
        
        return "testcase/form";
    }
    @GetMapping("/detail")
    public String detail(HttpServletRequest request,HttpSession session,Model model) {
        Long userId = (Long)session.getAttribute("userId");
        
        if(userId == null) {
        	return "redirect:/user/login";
        }
        
        TestCase tc = testCaseService.getTestCase(id(request,"testCaseId"),userId);
        
        if(tc == null) {
        	return "redirect:/";
        }
        
        model.addAttribute("tc",tc);
        
        model.addAttribute("canEditTc",testCaseService.canEdit(tc.getProjectId(),userId));
        
        return "testcase/detail";
    }
    
    @GetMapping("/edit")
    public String edit(HttpServletRequest request,HttpSession session,Model model) {
        Long userId = (Long)session.getAttribute("userId");
        
        if(userId == null) {
        	return "redirect:/user/login";
        }
        
        TestCase tc = testCaseService.getTestCase(id(request,"testCaseId"),userId);
        
        if(tc == null || !testCaseService.canEdit(tc.getProjectId(),userId)) {
        	return "redirect:/";
        }
        model.addAttribute("tc",tc);
        model.addAttribute("editing",true);
        
    	return "testcase/form";
    }
    
    @PostMapping("/edit")
    public String editPro(HttpServletRequest request,HttpSession session,Model model) {
        Long userId = (Long)session.getAttribute("userId"); 
        
        if(userId == null) {
        	return "redirect:/user/login";
        }
        
        TestCase existing = testCaseService.getTestCase(id(request,"testCaseId"),userId);
        
        if(existing == null || !testCaseService.canEdit(existing.getProjectId(),userId)) {
        	return "redirect:/";
        }
        
        TestCase tc = form(request); tc.setProjectId(existing.getProjectId()); 
        tc.setCaseCode(existing.getCaseCode());
        
        if(testCaseService.updateTestCase(tc,userId) == 1) {
        	return "redirect:/testcase/detail?testCaseId="+tc.getTestCaseId();
        }
        
        model.addAttribute("tc",tc);
        model.addAttribute("editing",true);
        model.addAttribute("error","수정에 실패했습니다. 입력값을 확인해 주세요.");
        
        return "testcase/form";
    }
    
    @PostMapping("/delete")
    public String delete(HttpServletRequest request,HttpSession session) {
        Long userId = (Long)session.getAttribute("userId"); 
        
        if(userId == null) {
        	return "redirect:/user/login";
        }
        TestCase tc=testCaseService.getTestCase(id(request,"testCaseId"),userId);
        
        if(tc == null) {
        	return "redirect:/";
        }
        
        testCaseService.deleteTestCase(tc.getTestCaseId(),tc.getProjectId(),userId);
        
        return projectUrl(tc.getProjectId());
    }

    @PostMapping("/result")
    public String updateResult(HttpServletRequest request, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return "redirect:/user/login";

        long testCaseId = id(request, "testCaseId");
        TestCase existing = testCaseService.getTestCase(testCaseId, userId);
        if (existing == null || !testCaseService.canEdit(existing.getProjectId(), userId)) {
            return "redirect:/";
        }

        testCaseService.updateTestCaseResult(
            testCaseId, request.getParameter("result"), userId);
        return projectUrl(existing.getProjectId());
    }

}
