package gov.hhs.onc.pdti.statistics.service.impl;

import gov.hhs.onc.pdti.statistics.dao.PDTIStatisticsDAO;
import gov.hhs.onc.pdti.statistics.entity.PDTIStatisticsEntity;
import gov.hhs.onc.pdti.statistics.service.PdtiAuditLog;
import java.util.List;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class PdtiAuditLogImpl implements PdtiAuditLog {

    private static PDTIStatisticsDAO pdtiStatisticsDAO;
    private static ConfigurableApplicationContext context;

    /**
     *
     * @return PdtiAuditLog
     */
    public static PdtiAuditLog getInstance() {
        if (context == null) {
            context = new ClassPathXmlApplicationContext("ApplicationContext.xml");
        }

        pdtiStatisticsDAO = (PDTIStatisticsDAO) context.getBean("pdtiStatisticsDAO");
        return new PdtiAuditLogImpl();
    }

    @Override
    public void save(PDTIStatisticsEntity oPDTIStatisticsEntity) {
        pdtiStatisticsDAO.save(oPDTIStatisticsEntity);
    }

    @Override
    public void update(PDTIStatisticsEntity ogetPDTIStatisticsEntityId) {
        pdtiStatisticsDAO.update(ogetPDTIStatisticsEntityId);
    }

    @Override
    public void delete(String strPDTIStatisticsEntityId) {
        pdtiStatisticsDAO.delete(strPDTIStatisticsEntityId);
    }

    @Override
    public List<PDTIStatisticsEntity> getAllPDTIStatisticsEntity() {
        return pdtiStatisticsDAO.getAllPDTIStatisticsEntity();
    }

    @Override
    public PDTIStatisticsEntity getPDTIStatisticsEntityById(String PDTIStatisticsEntityId) {
        return pdtiStatisticsDAO.getPDTIStatisticsEntityById(PDTIStatisticsEntityId);
    }
}
