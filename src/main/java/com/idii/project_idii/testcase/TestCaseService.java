package com.idii.project_idii.testcase;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.idii.project_idii.project.ProjectService;

@Service
public class TestCaseService {
    
	private final ProjectService projectService;
    
	public TestCaseService(ProjectService projectService) { this.projectService=projectService; }
    
	@Value("${db.url}") 
	private String jdbcURL;
    
	@Value("${db.username}") 
	private String dbId;
    
	@Value("${db.password}") 
	private String dbPw;

    public Connection getConnection() throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        return DriverManager.getConnection(jdbcURL, dbId, dbPw);
    }
    
    public void closeDatabase(Connection conn, PreparedStatement pstmt, ResultSet rs) {
        if(rs != null) try {rs.close();} catch(Exception e){e.printStackTrace();}
        if(pstmt != null) try {pstmt.close();} catch(Exception e){e.printStackTrace();}
        if(conn != null) try {conn.close();} catch(Exception e){e.printStackTrace();}
    }
    
    public boolean canEdit(long projectId, long userId) {
        return projectService.canEditContent(projectId, userId);
    }
    
    private boolean valid(TestCase tc, boolean create) {
        if(tc == null || tc.getTitle() == null || tc.getTitle().isBlank() || tc.getTitle().length()>200) {
        	return false;
        }
        if(create && (tc.getCaseCode() == null || tc.getCaseCode().isBlank() || tc.getCaseCode().length() > 30)) {
        	return false;
        }
        return tc.getResult() != null && List.of("0", "1", "2", "3", "4").contains(tc.getResult());
    }
    
    private TestCase map(ResultSet rs) throws Exception {
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
        tc.setCreatedAt(rs.getObject("created_at",LocalDateTime.class));
        tc.setUpdatedAt(rs.getObject("updated_at",LocalDateTime.class));
        tc.setExecutedAt(rs.getObject("executed_at",LocalDateTime.class));
        tc.setAuthorNickname(rs.getString("author_nickname"));
        
        return tc;
    }
    
    public List<TestCase> getTestCaseList(long projectId,long userId) {
        
    	List<TestCase> list = new ArrayList<>();
        
    	if(!projectService.canAccessProject(projectId,userId)) {
        	return list;
        }
        
        Connection conn = null;
        PreparedStatement pstmt = null; 
        ResultSet rs = null;
        
        try {
            conn = getConnection(); 
            
            String sql = "SELECT tc.*, u.nickname AS author_nickname "
            		   + "FROM test_case tc LEFT JOIN users u "
            		   + "ON u.user_id = tc.created_by "
            		   + "WHERE tc.project_id=? "
            		   + "ORDER BY tc.test_case_id DESC";
            
            pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1,projectId); rs=pstmt.executeQuery();
            
            while(rs.next()) {
            	list.add(map(rs));
            }
            
        } catch(Exception e){
        	throw new IllegalStateException("TC 목록 조회 실패",e);
    	} finally {
    		closeDatabase(conn,pstmt,rs);
    	} 
        return list;
    }
    
    public TestCase getTestCase(long testCaseId,long userId) {
        
    	Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
	            conn=getConnection();
	            
	            String sql = "SELECT tc.*, u.nickname AS author_nickname "
	            		   + "FROM test_case tc LEFT JOIN users u "
	            		   + "ON u.user_id = tc.created_by WHERE tc.test_case_id = ?";
	            
	            pstmt=conn.prepareStatement(sql);
	            pstmt.setLong(1,testCaseId);
	            
	            rs=pstmt.executeQuery();
	            
	            if(rs.next()) {
	        		TestCase tc = map(rs);
	                if(projectService.canAccessProject(tc.getProjectId(),userId)) {
	            	return tc;
	            	}
	        	}
	            
        } catch(Exception e) {
        	throw new IllegalStateException("TC 조회 실패",e);
        	
        } finally {
        	closeDatabase(conn,pstmt,rs);
        } 
        
        return null;
    }
    
    public int addTestCase(TestCase tc,long userId) {
        
    	if(!valid(tc,true) || !canEdit(tc.getProjectId(),userId)) {
        	return 0;
        }
    	
        Connection conn=null;
        PreparedStatement pstmt=null;
        
        try {
	            conn=getConnection();
	            
	            String sql = "INSERT INTO test_case (project_id, created_by, case_code, title, precondition, test_steps, expected_result, result, remarks, executed_at) "
	            		   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CASE WHEN ? ='4' THEN NULL ELSE CURRENT_TIMESTAMP END)";
	            
	            pstmt = conn.prepareStatement(sql);
	            
	            pstmt.setLong(1,tc.getProjectId());
	            pstmt.setLong(2,userId);
	            pstmt.setString(3,tc.getCaseCode().trim()); 
	            pstmt.setString(4,tc.getTitle().trim());
	            pstmt.setString(5,tc.getPrecondition()); 
	            pstmt.setString(6,tc.getTestSteps());
	            pstmt.setString(7,tc.getExpectedResult()); 
	            pstmt.setString(8,tc.getResult());
	            pstmt.setString(9,tc.getRemarks()); 
	            pstmt.setString(10,tc.getResult()); 
	            
	            return pstmt.executeUpdate();
            
        } catch(SQLIntegrityConstraintViolationException e){
        	return -1;
        	} catch(Exception e){
        		e.printStackTrace();
        		return 0;
        	
        	} finally {
        		closeDatabase(conn,pstmt,null);
        		}
    }
    
    public int updateTestCase(TestCase tc,long userId) {
        
    	if(!valid(tc,false) || !canEdit(tc.getProjectId(),userId)) {
        	return 0;
        }
        
    	Connection conn = null; 
    	PreparedStatement pstmt=null;
        
        try {
	            conn = getConnection();
	            
	            String sql = "UPDATE test_case SET title = ?, precondition = ?, test_steps = ?, "
	                    + "expected_result = ?, executed_at = CASE WHEN ? = '4' THEN NULL ELSE CURRENT_TIMESTAMP END, "
	                    + "result = ?, remarks = ? WHERE test_case_id = ? AND project_id = ?";
	            
	            pstmt = conn.prepareStatement(sql);
	            
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
            
        } catch (Exception e) {
            e.printStackTrace();
            return 0;

        } finally {
            closeDatabase(conn, pstmt, null);
        }
    }
    
    public int deleteTestCase(long testCaseId,long projectId,long userId) {
        
    	if(!canEdit(projectId,userId)) {
        	return 0;
        }
        
        Connection conn = null;
        PreparedStatement pstmt=null;
        
        try {
	            conn = getConnection();
	            
	            String sql = "DELETE FROM test_case WHERE test_case_id=? AND project_id=?";
	            
	            pstmt = conn.prepareStatement(sql);
	            pstmt.setLong(1,testCaseId);
	            pstmt.setLong(2,projectId);
	            
	            return pstmt.executeUpdate();
	            
        } catch(Exception e){
        	
        		e.printStackTrace();
        		return 0;
        		
        } finally {
        	
        	closeDatabase(conn,pstmt,null);
        }
    }

    public int updateTestCaseResult(long testCaseId, String result, long userId) {

    	// 프로젝트 멤버만 수정 가능하게
    	if (result == null || !List.of("0", "1", "2", "3", "4").contains(result)) {
        	return 0;
        }

        TestCase existing = getTestCase(testCaseId, userId);

        if (existing == null || !canEdit(existing.getProjectId(), userId)) {
        	return 0;
        }

        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            conn = getConnection();
            
            String sql = "UPDATE test_case SET "
            		   + "executed_at = CASE WHEN ? = '4' THEN NULL ELSE CURRENT_TIMESTAMP END, "
                       + "result = ? "
                       + "WHERE test_case_id = ? AND project_id = ?";

            pstmt = conn.prepareStatement(sql);

            pstmt.setString(1, result);
            pstmt.setString(2, result);
            pstmt.setLong(3, testCaseId);
            pstmt.setLong(4, existing.getProjectId());

            return pstmt.executeUpdate();

        } catch (Exception e) {
        	
            e.printStackTrace();
            return 0;
            
        } finally {
        	
            closeDatabase(conn, pstmt, null);
        }
    }

}
