package gov.hhs.onc.pdti.statistics.dao;

import gov.hhs.onc.pdti.statistics.entity.PDTIStatisticsEntity;
import java.util.List;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.SessionFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Scope("prototype")
@Transactional
public class PDTIStatisticsDAO {
    
    private SessionFactory sessionFactory;

    /**
     * 
     * @return 
     */
    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    /**
     * 
     * @param sessionFactory 
     */
    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    /**
     *
     * @param PDTIStatisticsEntityId
     * @return PDTIStatisticsEntity
     */
    public PDTIStatisticsEntity getPDTIStatisticsEntityById(String PDTIStatisticsEntityId) {
        return (PDTIStatisticsEntity) this.sessionFactory.getCurrentSession().get(PDTIStatisticsEntity.class,
                PDTIStatisticsEntityId);
    }

    /**
     *
     * @return List
     */
    public List<PDTIStatisticsEntity> getAllPDTIStatisticsEntity() {
        Criteria criteria = this.sessionFactory.getCurrentSession().createCriteria(
                PDTIStatisticsEntity.class);
        criteria.setFetchMode("status", FetchMode.JOIN);
        return criteria.list();
    }

    /**
     *
     * @param oPDTIStatisticsEntity
     */
    public void save(PDTIStatisticsEntity oPDTIStatisticsEntity) {
        this.sessionFactory.getCurrentSession().save(oPDTIStatisticsEntity);
    }

    /**
     *
     * @param ogetPDTIStatisticsEntityId
     */
    public void update(PDTIStatisticsEntity ogetPDTIStatisticsEntityId) {
        this.sessionFactory.getCurrentSession().saveOrUpdate(ogetPDTIStatisticsEntityId);
    }

    /**
     *
     * @param strPDTIStatisticsEntityId
     */
    public void delete(String strPDTIStatisticsEntityId) {
        PDTIStatisticsEntity oPDTIStatisticsEntityId = getPDTIStatisticsEntityById(strPDTIStatisticsEntityId);
        this.sessionFactory.getCurrentSession().saveOrUpdate(oPDTIStatisticsEntityId);
    }
}
