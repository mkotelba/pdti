package gov.hhs.onc.pdti.statistics.service;

import gov.hhs.onc.pdti.statistics.entity.PDTIStatisticsEntity;
import java.util.List;

public interface PdtiAuditLog {

    public void save(PDTIStatisticsEntity oPDTIStatisticsEntity);

    public void update(PDTIStatisticsEntity ogetPDTIStatisticsEntityId);

    public void delete(String strPDTIStatisticsEntityId);

    public List<PDTIStatisticsEntity> getAllPDTIStatisticsEntity();

    public PDTIStatisticsEntity getPDTIStatisticsEntityById(String PDTIStatisticsEntityId);
}
