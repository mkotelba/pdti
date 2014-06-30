/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.hhs.onc.pdti.statistics.entity;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 *
 * @author Sai Valluripalli
 */
@Entity
@Table(name = "pdtiauditlog")
public class PDTIStatisticsEntity implements Serializable {

    @Column(name = "sourcedomain", length = 50, nullable = true)
    private String sourceDomain;

    @Column(name = "basedn", length = 50, nullable = true)
    private String baseDn;

    @Column(name = "pdrequesttype", length = 50, nullable = true)
    private String pdRequestType;

    @Column(name = "status", length = 15, nullable = false)
    private String status;

    @Column(name = "creationdate", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date creationDate;

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;

    /**
     *
     * @param id
     * @param sourceDomain
     * @param baseDn
     * @param pdRequestType
     * @param status
     * @param creationDate
     */
    public PDTIStatisticsEntity(Long id, String sourceDomain, String baseDn, String pdRequestType, String status, Date creationDate) {
        this.id = id;
        this.sourceDomain = sourceDomain;
        this.baseDn = baseDn;
        this.pdRequestType = pdRequestType;
        this.status = status;
        this.creationDate = creationDate;
    }

    /**
     * Constructor
     */
    public PDTIStatisticsEntity() {

    }

    /**
     *
     * @return String
     */
    public String getSourceDomain() {
        return sourceDomain;
    }

    /**
     *
     * @param sourceDomain
     */
    public void setSourceDomain(String sourceDomain) {
        this.sourceDomain = sourceDomain;
    }

    /**
     *
     * @return String
     */
    public String getBaseDn() {
        return baseDn;
    }

    /**
     *
     * @param baseDn
     */
    public void setBaseDn(String baseDn) {
        this.baseDn = baseDn;
    }

    /**
     *
     * @return String
     */
    public String getPdRequestType() {
        return pdRequestType;
    }

    /**
     *
     * @param pdRequestType
     */
    public void setPdRequestType(String pdRequestType) {
        this.pdRequestType = pdRequestType;
    }

    /**
     *
     * @return String
     */
    public String getStatus() {
        return status;
    }

    /**
     *
     * @param status
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     *
     * @return Date
     */
    public Date getCreationDate() {
        return creationDate;
    }

    /**
     *
     * @param creationDate
     */
    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    /**
     *
     * @return Long
     */
    public Long getId() {
        return id;
    }

    /**
     *
     * @param id
     */
    public void setId(Long id) {
        this.id = id;
    }
}
