package gov.hhs.onc.pdti.testtool.actions;


import gov.hhs.onc.pdti.testtool.DirectoryTypes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.convention.annotation.InterceptorRef;
import org.apache.struts2.convention.annotation.InterceptorRefs;
import org.apache.struts2.convention.annotation.ParentPackage;
import org.apache.struts2.convention.annotation.Result;
import org.apache.xmlbeans.XmlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eviware.soapui.impl.settings.XmlBeansSettingsImpl;
import com.eviware.soapui.impl.wsdl.WsdlProject;
import com.eviware.soapui.impl.wsdl.teststeps.WsdlTestRequestStepResult;
import com.eviware.soapui.model.testsuite.TestCase;
import com.eviware.soapui.model.testsuite.TestCaseRunner;
import com.eviware.soapui.model.testsuite.TestStepResult;
import com.eviware.soapui.model.testsuite.TestSuite;
import com.eviware.soapui.settings.HttpSettings;
import com.eviware.soapui.settings.WsdlSettings;
import com.eviware.soapui.support.SoapUIException;
import com.opensymphony.xwork2.ActionSupport;
import com.opensymphony.xwork2.validator.annotations.RequiredFieldValidator;
import com.opensymphony.xwork2.validator.annotations.Validations;
import com.opensymphony.xwork2.validator.annotations.ValidatorType;

@InterceptorRefs({ @InterceptorRef("defaultStack") })
@ParentPackage("json-default")
public class Execute extends ActionSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(Execute.class);
    private static final String WSDL_URL_ATTRIBUTE_PARAM_NAME = "wsdlUrl";
    private static final String BASE_DN_STRING_PARAM_NAME = "baseDn";
    private static final String DIRECTORY_TYPE_PARAM_NAME = "typeOfDirectory";
    private static final String REQUIRED_FIELD_MESSAGE = "This field is required.";
    private static final String IHE_SOAPUI_PROJECT_FILE = "soapui-project.xml";
    private static final String URL_PROPERTY = "project.test.server.wsdl.url";
    private static final String BASE_DN_PROPERTY = "project.test.server.dsml.dn.base";
    private static final String[] SKIP_TEST_CASE_NAME_PATTERNS = new String[] { "^dup_req_id_federation_loop_[^$]+$" };
    private static final String BAD_DIRECTORY_TYPE_MESSAGE_FRAGMENT = " is not a recognized directory type.";
    private static final String FINISHED = "FINISHED";
    private static final String PASSED = "PASSED";
    private static final String FAILED = "FAILED";
    private static final String NONE = "none";
    private static final String STARTING = "starting...";
    private static final String SLASH = "/";
    private static final String SOAPUI_PROJECT_FILE_ERROR_FRAGMENT =
            "Unable to create SoapUI WSDL project from SoapUI project file: ";
    private static final Map<WsdlProject, Map<String, Map<String, String>>> PROJECTS_MAP =
            new HashMap<WsdlProject, Map<String, Map<String, String>>>();
    private static final WsdlProject IHE_WSDL_PROJECT = getWsdlProject(IHE_SOAPUI_PROJECT_FILE);
    
    private static final String SETTINGS_FILE_NAME = "soapui-settings.xml";

    private WsdlProject wsdlProject;
    private String wsdlUrl;
    private String baseDn;
    private List<Object[]> testResults = new ArrayList<Object[]>();
    private Map<String, Object> testResultsJson = new HashMap<String, Object>();
    private String status = STARTING;
    private String typeOfDirectory;
    
    private static String getFileUrl(final String fileName) {
        return Execute.class.getClassLoader().getResource(fileName).toString();
    }
    
    private static WsdlProject getWsdlProject(final String projectFile) {
        WsdlProject wsdlProject = null;
        try {
            wsdlProject = new WsdlProject(getFileUrl(projectFile));
            XmlBeansSettingsImpl xmlBeansSettingsImpl = wsdlProject.getSettings();
            xmlBeansSettingsImpl.setString(HttpSettings.CLOSE_CONNECTIONS, Boolean.TRUE.toString());
            xmlBeansSettingsImpl.setString(HttpSettings.INCLUDE_REQUEST_IN_TIME_TAKEN, Boolean.TRUE.toString());
            xmlBeansSettingsImpl.setString(HttpSettings.INCLUDE_RESPONSE_IN_TIME_TAKEN, Boolean.TRUE.toString());
            xmlBeansSettingsImpl.setString(WsdlSettings.CACHE_WSDLS, Boolean.FALSE.toString());
            xmlBeansSettingsImpl.setString(WsdlSettings.PRETTY_PRINT_PROJECT_FILES, Boolean.TRUE.toString());
            xmlBeansSettingsImpl.setString(WsdlSettings.PRETTY_PRINT_RESPONSE_MESSAGES, Boolean.TRUE.toString());
            buildTestcaseDescriptionsMap(wsdlProject);
        } catch (XmlException | IOException | SoapUIException e) {
            LOGGER.error(SOAPUI_PROJECT_FILE_ERROR_FRAGMENT + projectFile, e);
        }
        return wsdlProject;
    }
    
    private static void buildTestcaseDescriptionsMap(final WsdlProject wsdlProject) {
        Map<String, Map<String, String>> testSuiteToTestCasesDescriptionsMap =
                new HashMap<String,Map<String, String>>();
        Map<String, String> testCasesDescriptionsMap = new HashMap<String, String>();
        List<TestSuite> testSuitesList = wsdlProject.getTestSuiteList();
        for(TestSuite testSuite : testSuitesList) {
            List<TestCase> testCaseList = testSuite.getTestCaseList();
            for(TestCase testCase : testCaseList) {
                testCasesDescriptionsMap.put(testCase.getName(), testCase.getDescription());
            }
            testSuiteToTestCasesDescriptionsMap.put(testSuite.getName(), testCasesDescriptionsMap);
        }
        PROJECTS_MAP.put(wsdlProject, testSuiteToTestCasesDescriptionsMap);
    }

    @Validations(requiredFields = {
            @RequiredFieldValidator(type = ValidatorType.SIMPLE, fieldName = WSDL_URL_ATTRIBUTE_PARAM_NAME, message = REQUIRED_FIELD_MESSAGE),
            @RequiredFieldValidator(type = ValidatorType.SIMPLE, fieldName = BASE_DN_STRING_PARAM_NAME, message = REQUIRED_FIELD_MESSAGE),
            @RequiredFieldValidator(type = ValidatorType.SIMPLE, fieldName = DIRECTORY_TYPE_PARAM_NAME, message = REQUIRED_FIELD_MESSAGE) })
    @Action(value="testResultsJson", results = { @Result(name="success", type="json") })
    public String execute() {
        if (typeOfDirectory.equals(DirectoryTypes.IHE.toString())) {
            wsdlProject = IHE_WSDL_PROJECT;
        } else {
            String errorMessage = typeOfDirectory + BAD_DIRECTORY_TYPE_MESSAGE_FRAGMENT;
            LOGGER.error(errorMessage);
            return ERROR;
        }
        wsdlProject.setPropertyValue(URL_PROPERTY, getWsdlUrl());
        wsdlProject.setPropertyValue(BASE_DN_PROPERTY, getBaseDn());
        executeTests();
        return SUCCESS;
    }

    private void executeTests() {
        List<TestSuite> testSuites = wsdlProject.getTestSuiteList();
        int numberOfTestCases = countTestCases(testSuites);
        int testCaseCounter = 0;
        for (TestSuite testSuite : testSuites) {
            Map<String, String> testCasesDescriptionsMap = PROJECTS_MAP.get(wsdlProject).get(testSuite.getName());
            for (TestCase testCase : testSuite.getTestCaseList()) {
                if (!isSkippedTestCase(testCase.getName())) {
                    testCaseCounter++;
                    TestCaseRunner testCaseRunner = testCase.run(null, false);
                    String testStatus = FAILED;
                    if(FINISHED.equals(testCaseRunner.getStatus().toString())) {
                        testStatus = PASSED;
                    }
                    List<TestStepResult> testStepResultList = testCaseRunner.getResults();
                    List<String> messagesList = new ArrayList<String>();
                    String requestContent = null;
                    String responseContent = null;
                    for(TestStepResult testStepResult : testStepResultList) {
                        WsdlTestRequestStepResult wsdlTestRequestStepResult = null;
                        if(testStepResult instanceof WsdlTestRequestStepResult) {
                            wsdlTestRequestStepResult = (WsdlTestRequestStepResult)testStepResult;
                            requestContent = wsdlTestRequestStepResult.getRequestContentAsXml();
                            responseContent = wsdlTestRequestStepResult.getResponseContentAsXml();
                        }
                        String[] messages = testStepResult.getMessages();
                        for(String message : messages) {
                            messagesList.add(message);
                        }
                        if(0 == messagesList.size()) {
                            messagesList.add(NONE);
                        }
                    }
                    Object[] testResultData = new Object[] {testSuite.getName(), testCase.getName(), testStatus,
                            testCasesDescriptionsMap.get(testCase.getName()), messagesList, requestContent,
                            responseContent};
                    testResults.add(testResultData);
                    status = testCaseCounter + SLASH + numberOfTestCases;
                }
            }
        }
        testResultsJson.put("testResults", testResults);
    }

    private static boolean isSkippedTestCase(String testCaseName) {
        for (String skipTestCaseNamePattern : SKIP_TEST_CASE_NAME_PATTERNS) {
            if (testCaseName.matches(skipTestCaseNamePattern)) {
                return true;
            }
        }
        return false;
    }

    private int countTestCases(final List<TestSuite> testSuites) {
        int numberOfTestCases = 0;
        for (TestSuite testSuite : testSuites) {
            for (TestCase testCase : testSuite.getTestCaseList()) {
                if (!isSkippedTestCase(testCase.getName())) {
                    numberOfTestCases++;
                }
            }
        }
        return numberOfTestCases;
    }

    public String getWsdlUrl() {
        return wsdlUrl;
    }

    public void setWsdlUrl(final String wsdlUrl) {
        this.wsdlUrl = wsdlUrl;
    }

    public String getBaseDn() {
        return baseDn;
    }

    public void setBaseDn(final String baseDn) {
        this.baseDn = baseDn;
    }

    public List<Object[]> getTestResults() {
        return new ArrayList<>(testResults);
    }

    public String getStatus() {
        return this.status;
    }

    public String getTypeOfDirectory() {
        return typeOfDirectory;
    }

    public void setTypeOfDirectory(String typeOfDirectory) {
        this.typeOfDirectory = typeOfDirectory;
    }

    public Map<String, Object> getTestResultsJson() {
        return testResultsJson;
    }

}
