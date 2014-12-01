package gov.hhs.onc.pdti.statistics.service;

import gov.hhs.onc.pdti.statistics.entity.PDTIStatisticsEntity;

import java.util.Date;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations="classpath:test-context.xml")
public class PdtiAuditServiceTest {
    @Autowired
    PdtiAuditService auditService;
    PDTIStatisticsEntity pdtiStatEnt;
    
	@Before
	public void buildTestData(){
		pdtiStatEnt = new PDTIStatisticsEntity();
		pdtiStatEnt.setBaseDn("testdn");
		pdtiStatEnt.setCreationDate(new Date());
		pdtiStatEnt.setPdRequestType("requestType");
		pdtiStatEnt.setStatus("success");
	}

	@Test
    @Transactional
	public void shouldSaveANewPost() {
        Assert.assertNotNull(auditService.savePdtiStatisticsEntity(pdtiStatEnt));
	}
}
