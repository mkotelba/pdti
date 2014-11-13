package gov.hhs.onc.pdti.statistics.service.impl;

import gov.hhs.onc.pdti.statistics.entity.PDTIStatisticsEntity;
import gov.hhs.onc.pdti.statistics.service.PdtiAuditLog;
import java.util.Date;

public class PdtiAuditLogImplTest {

    public static void main(String args[]) {
        try {
            PdtiAuditLog pdtiAuditLogService = PdtiAuditLogImpl.getInstance();
            if (pdtiAuditLogService != null) {
                PDTIStatisticsEntity entity = new PDTIStatisticsEntity();
                entity.setBaseDn("testDn");
                entity.setCreationDate(new Date());
                entity.setPdRequestType("BatchRequest");
                entity.setStatus("Success");
                pdtiAuditLogService.save(entity);
                System.out.println("Spring Context Bean Initialized...");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
