/*
 * Copyright (c) 2005-2010 Grameen Foundation USA
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * See also http://www.apache.org/licenses/LICENSE-2.0.html for an
 * explanation of the license and how it is applied.
 */

package org.mifos.accounts.productdefinition.business;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.joda.time.DateTime;
import org.mifos.accounts.financial.business.GLCodeEntity;
import org.mifos.accounts.productdefinition.exceptions.ProductDefinitionException;
import org.mifos.accounts.productdefinition.persistence.SavingsPrdPersistence;
import org.mifos.accounts.productdefinition.util.helpers.ApplicableTo;
import org.mifos.accounts.productdefinition.util.helpers.InterestCalcType;
import org.mifos.accounts.productdefinition.util.helpers.PrdStatus;
import org.mifos.accounts.productdefinition.util.helpers.RecommendedAmountUnit;
import org.mifos.accounts.productdefinition.util.helpers.SavingsType;
import org.mifos.application.meeting.business.MeetingBO;
import org.mifos.application.meeting.util.helpers.MeetingType;
import org.mifos.dto.domain.ProductDetailsDto;
import org.mifos.dto.domain.SavingsProductDto;
import org.mifos.framework.components.logger.LoggerConstants;
import org.mifos.framework.components.logger.MifosLogManager;
import org.mifos.framework.components.logger.MifosLogger;
import org.mifos.framework.exceptions.PersistenceException;
import org.mifos.framework.util.helpers.Money;
import org.mifos.security.util.UserContext;

public class SavingsOfferingBO extends PrdOfferingBO {

    private static final MifosLogger prdLogger = MifosLogManager.getLogger(LoggerConstants.PRDDEFINITIONLOGGER);

    private Money recommendedAmount;
    private Money maxAmntWithdrawl;
    private Money minAmntForInt;
    private Double interestRate;
    private RecommendedAmntUnitEntity recommendedAmntUnit;
    private SavingsTypeEntity savingsType;
    private InterestCalcTypeEntity interestCalcType;
    private final GLCodeEntity depositGLCode;
    private final GLCodeEntity interestGLCode;

    private final Set<PrdOfferingMeetingEntity> savingsOfferingMeetings;

    /**
     * default constructor for hibernate usuage
     */
    public SavingsOfferingBO() {
        this(null, null, null, new HashSet<PrdOfferingMeetingEntity>());
    }

    public static SavingsOfferingBO createNew(Integer userId, String systemId, String name, String shortName, String description,
            ProductCategoryBO productCategory, DateTime startDate, DateTime endDate,
            PrdApplicableMasterEntity applicableToEntity, PrdStatusEntity selectedStatus, SavingsTypeEntity savingsTypeEntity, RecommendedAmntUnitEntity recommendedAmntUnitEntity, Money amountForDeposit, Money maxWithdrawal,
            BigDecimal interestRate, InterestCalcTypeEntity interestCalcTypeEntity, MeetingBO interestCalculationMeeting, MeetingBO interestPostingMeeting,
            Money minAmountForInterestCalculation, GLCodeEntity depositGlEntity, GLCodeEntity interestGlEntity) {

        SavingsOfferingBO savingsProduct = new SavingsOfferingBO(userId, systemId, name, shortName, productCategory, selectedStatus, applicableToEntity, startDate, savingsTypeEntity, interestRate, interestCalcTypeEntity, interestCalculationMeeting, interestPostingMeeting, depositGlEntity, interestGlEntity);
        savingsProduct.setDescription(description);
        if (endDate != null) {
            savingsProduct.setEndDate(endDate.toDate());
        }
        savingsProduct.setRecommendedAmntUnit(recommendedAmntUnitEntity);
        savingsProduct.setRecommendedAmount(amountForDeposit);
        savingsProduct.setMaxAmntWithdrawl(maxWithdrawal);
        savingsProduct.setMinAmntForInt(minAmountForInterestCalculation);

        return savingsProduct;
    }

    /**
     * @deprecated - get builders to use factory methods
     */
    @Deprecated
    public static SavingsOfferingBO createInstanceForTest(final Short prdOfferingId) {
        return new SavingsOfferingBO(prdOfferingId, null, null, new HashSet<PrdOfferingMeetingEntity>());
    }

    /**
     * minimal legal constructor for creating savings products
     * @param interestPostingMeeting
     */
    public SavingsOfferingBO(Integer userId, String systemId, String name, String shortName,
            ProductCategoryBO productCategory, PrdStatusEntity status,
            PrdApplicableMasterEntity applicableToEntity, DateTime startDate, SavingsTypeEntity savingsTypeEntity,
            BigDecimal interestRate, InterestCalcTypeEntity interestCalcTypeEntity, MeetingBO interestCalculationMeeting, MeetingBO interestPostingMeeting, GLCodeEntity depositGlEntity, GLCodeEntity interestGlEntity) {
        super(userId, systemId, name, shortName, productCategory, status, applicableToEntity, startDate);

        this.savingsType = savingsTypeEntity;
        this.interestRate = interestRate.doubleValue();
        this.interestCalcType = interestCalcTypeEntity;
        this.depositGLCode = depositGlEntity;
        this.interestGLCode = interestGlEntity;
        this.savingsOfferingMeetings = new HashSet<PrdOfferingMeetingEntity>();
        setTimePerForInstcalc(new PrdOfferingMeetingEntity(interestCalculationMeeting, this, MeetingType.SAVINGS_INTEREST_CALCULATION_TIME_PERIOD));
        setFreqOfPostIntcalc(new PrdOfferingMeetingEntity(interestPostingMeeting, this, MeetingType.SAVINGS_INTEREST_POSTING));
    }


    /**
     * @deprecated - get builders to use factory methods
     */
    @Deprecated
    public SavingsOfferingBO(final SavingsType savingsType, final String name, final String shortName,
            final String globalProductNumber, final Date startDate, final ApplicableTo applicableToCustomer,
            final ProductCategoryBO category, final PrdStatusEntity productStatus,
            final InterestCalcType interestCalcType, final Double interestRate, final Money maxAmountOfWithdrawal,
            final GLCodeEntity depositGLCode, final GLCodeEntity interestGLCode, final Date createdDate,
            final Short createdByUserId) {
        super(name, shortName, globalProductNumber, startDate, applicableToCustomer, category, productStatus,
                createdDate, createdByUserId);
        this.maxAmntWithdrawl = maxAmountOfWithdrawal;
        this.interestRate = interestRate;
        this.savingsType = new SavingsTypeEntity(savingsType);
        this.interestCalcType = new InterestCalcTypeEntity(interestCalcType);
        this.depositGLCode = depositGLCode;
        this.interestGLCode = interestGLCode;
        this.savingsOfferingMeetings = new HashSet<PrdOfferingMeetingEntity>();
    }

    /**
     * @deprecated - get builders to use factory methods
     */
    @Deprecated
    public SavingsOfferingBO(final UserContext userContext, final String prdOfferingName, final String prdOfferingShortName,
            final ProductCategoryBO prdCategory, final PrdApplicableMasterEntity prdApplicableMaster, final Date startDate,
            final SavingsTypeEntity savingsType, final InterestCalcTypeEntity interestCalcType, final MeetingBO timePerForInstcalc,
            final MeetingBO freqOfPostIntcalc, final Money recommendedAmount, final Double interestRate, final GLCodeEntity depositGLCode,
            final GLCodeEntity interestGLCode) throws ProductDefinitionException {
        this(userContext, prdOfferingName, prdOfferingShortName, prdCategory, prdApplicableMaster, startDate, null,
                null, null, savingsType, interestCalcType, timePerForInstcalc, freqOfPostIntcalc, recommendedAmount,
                null, null, interestRate, depositGLCode, interestGLCode);
    }

    /**
     * @deprecated - get builders to use factory methods
     */
    @Deprecated
    public SavingsOfferingBO(final UserContext userContext, final String prdOfferingName, final String prdOfferingShortName,
            final ProductCategoryBO prdCategory, final PrdApplicableMasterEntity prdApplicableMaster, final Date startDate, final Date endDate,
            final String description, final RecommendedAmntUnitEntity recommendedAmntUnit, final SavingsTypeEntity savingsType,
            final InterestCalcTypeEntity interestCalcType, final MeetingBO timePerForInstcalc, final MeetingBO freqOfPostIntcalc,
            final Money recommendedAmount, final Money maxAmntWithdrawl, final Money minAmntForInt, final Double interestRate,
            final GLCodeEntity depositGLCode, final GLCodeEntity interestGLCode) throws ProductDefinitionException {
        super(userContext, prdOfferingName, prdOfferingShortName, prdCategory, prdApplicableMaster, startDate, endDate,
                description);
        prdLogger.debug("creating savings product offering");
        validate(savingsType, interestCalcType, recommendedAmntUnit, timePerForInstcalc, freqOfPostIntcalc,
                recommendedAmount, interestRate, depositGLCode, interestGLCode);
        setCreateDetails();
        this.savingsType = savingsType;
        this.interestCalcType = interestCalcType;
        this.recommendedAmntUnit = recommendedAmntUnit;

        savingsOfferingMeetings = new HashSet<PrdOfferingMeetingEntity>();
        setTimePerForInstcalc(new PrdOfferingMeetingEntity(timePerForInstcalc, this,
                MeetingType.SAVINGS_INTEREST_CALCULATION_TIME_PERIOD));
        setFreqOfPostIntcalc(new PrdOfferingMeetingEntity(freqOfPostIntcalc, this, MeetingType.SAVINGS_INTEREST_POSTING));
        this.recommendedAmount = recommendedAmount;
        this.interestRate = interestRate;
        this.maxAmntWithdrawl = maxAmntWithdrawl;
        this.minAmntForInt = minAmntForInt;
        this.depositGLCode = depositGLCode;
        this.interestGLCode = interestGLCode;
        prdLogger.debug("creating savings product offering done :" + getGlobalPrdOfferingNum());
    }

    /**
     * @deprecated - get builders to use factory methods
     */
    @Deprecated
    public SavingsOfferingBO(final Short prdOfferingId, final GLCodeEntity depositGLCode, final GLCodeEntity interestGLCode,
            final HashSet<PrdOfferingMeetingEntity> savingsOfferingMeetings) {
        super(prdOfferingId);
        this.depositGLCode = depositGLCode;
        this.interestGLCode = interestGLCode;
        this.savingsOfferingMeetings = savingsOfferingMeetings;
    }

    private Set<PrdOfferingMeetingEntity> getSavingsOfferingMeetings() {
        return savingsOfferingMeetings;
    }

    public PrdOfferingMeetingEntity getFreqOfPostIntcalc() {
        return getPrdOfferingMeeting(MeetingType.SAVINGS_INTEREST_POSTING);
    }

    public void setFreqOfPostIntcalc(final PrdOfferingMeetingEntity freqOfPostIntcalc) {
        this.savingsOfferingMeetings.add(freqOfPostIntcalc);
    }

    public PrdOfferingMeetingEntity getTimePerForInstcalc() {
        return getPrdOfferingMeeting(MeetingType.SAVINGS_INTEREST_CALCULATION_TIME_PERIOD);
    }

    public void setTimePerForInstcalc(final PrdOfferingMeetingEntity timePerForInstcalc) {
        this.savingsOfferingMeetings.add(timePerForInstcalc);
    }

    public Double getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(final Double interestRate) {
        this.interestRate = interestRate;
    }

    public Money getMaxAmntWithdrawl() {
        return maxAmntWithdrawl;
    }

    public void setMaxAmntWithdrawl(final Money maxAmntWithdrawl) {
        this.maxAmntWithdrawl = maxAmntWithdrawl;
    }

    public Money getMinAmntForInt() {
        return minAmntForInt;
    }

    public void setMinAmntForInt(final Money minAmntForInt) {
        this.minAmntForInt = minAmntForInt;
    }

    public Money getRecommendedAmount() {
        return recommendedAmount;
    }

    void setRecommendedAmount(final Money recommendedAmount) {
        this.recommendedAmount = recommendedAmount;
    }

    public InterestCalcTypeEntity getInterestCalcType() {
        return interestCalcType;
    }

    public void setInterestCalcType(final InterestCalcTypeEntity interestCalcType) {
        this.interestCalcType = interestCalcType;
    }

    public RecommendedAmntUnitEntity getRecommendedAmntUnit() {
        return recommendedAmntUnit;
    }

    void setRecommendedAmntUnit(final RecommendedAmntUnitEntity recommendedAmntUnit) {
        this.recommendedAmntUnit = recommendedAmntUnit;
    }

    public void setRecommendedAmntUnit(final RecommendedAmountUnit unit) {
        setRecommendedAmntUnit(new RecommendedAmntUnitEntity(unit));
    }

    public SavingsTypeEntity getSavingsType() {
        return savingsType;
    }

    public SavingsType getSavingsTypeAsEnum() {
        return SavingsType.fromInt(savingsType.getId());
    }

    public void setSavingsType(final SavingsTypeEntity savingsType) {
        this.savingsType = savingsType;
    }

    public GLCodeEntity getDepositGLCode() {
        return depositGLCode;
    }

    public GLCodeEntity getInterestGLCode() {
        return interestGLCode;
    }

    /**
     * @deprecated - extract persistence
     */
    @Deprecated
    public void save() throws ProductDefinitionException {
        prdLogger.debug("creating the saving offering ");
        try {
            new SavingsPrdPersistence().createOrUpdate(this);

        } catch (PersistenceException e) {
            throw new ProductDefinitionException(e);
        }
        prdLogger.debug("creating the saving offering Done : " + getPrdOfferingName());
    }

    @Override
    public boolean isActive() {
        return getStatus() == PrdStatus.SAVINGS_ACTIVE;
    }

    /**
     * @deprecated - extract persistence
     */
    @Deprecated
    public void update(final Short userId, final String prdOfferingName, final String prdOfferingShortName,
            final ProductCategoryBO prdCategory, final PrdApplicableMasterEntity prdApplicableMaster, final Date startDate, final Date endDate,
            final String description, final PrdStatus prdStatus, final RecommendedAmntUnitEntity recommendedAmntUnit,
            final SavingsTypeEntity savingsType, final InterestCalcTypeEntity interestCalcType, final MeetingBO timePerForInstcalc,
            final MeetingBO freqOfPostIntcalc, final Money recommendedAmount, final Money maxAmntWithdrawl, final Money minAmntForInt,
            final Double interestRate) throws ProductDefinitionException {
        super.update(userId, prdOfferingName, prdOfferingShortName, prdCategory, prdApplicableMaster, startDate,
                endDate, description, prdStatus);
        validate(savingsType, interestCalcType, recommendedAmntUnit, timePerForInstcalc, freqOfPostIntcalc,
                recommendedAmount, interestRate, depositGLCode, interestGLCode);
        setUpdateDetails(userId);
        this.savingsType = savingsType;
        this.interestCalcType = interestCalcType;
        this.recommendedAmntUnit = recommendedAmntUnit;
        this.savingsOfferingMeetings.clear();
        setTimePerForInstcalc(new PrdOfferingMeetingEntity(timePerForInstcalc, this,
                MeetingType.SAVINGS_INTEREST_CALCULATION_TIME_PERIOD));
        setFreqOfPostIntcalc(new PrdOfferingMeetingEntity(freqOfPostIntcalc, this, MeetingType.SAVINGS_INTEREST_POSTING));
        this.recommendedAmount = recommendedAmount;
        this.interestRate = interestRate;
        this.maxAmntWithdrawl = maxAmntWithdrawl;
        this.minAmntForInt = minAmntForInt;
        try {
            new SavingsPrdPersistence().createOrUpdate(this);

        } catch (PersistenceException e) {
            throw new ProductDefinitionException(e);
        }
        prdLogger.debug("updated savings product offering done :" + getGlobalPrdOfferingNum());
    }

    private PrdOfferingMeetingEntity getPrdOfferingMeeting(final MeetingType meetingType) {

        PrdOfferingMeetingEntity matchingEntity = null;
        if (getSavingsOfferingMeetings() != null && getSavingsOfferingMeetings().size() > 0) {
            for (PrdOfferingMeetingEntity prdOfferingMeeting : getSavingsOfferingMeetings()) {
                if (prdOfferingMeeting.getprdOfferingMeetingType().equals(meetingType)) {
                    matchingEntity = prdOfferingMeeting;
                }
            }
        }
        return matchingEntity;
    }

    private void validate(final SavingsTypeEntity savingsType, final InterestCalcTypeEntity interestCalcType,
            final RecommendedAmntUnitEntity recommendedAmntUnit, final MeetingBO timePerForInstcalc, final MeetingBO freqOfPostIntcalc,
            final Money recommendedAmount, final Double interestRate, final GLCodeEntity depositGLCode, final GLCodeEntity interestGLCode)
            throws ProductDefinitionException {
        prdLogger.debug("Validating the fields in savings Offering");
        if (savingsType == null || interestCalcType == null || timePerForInstcalc == null || freqOfPostIntcalc == null
                || interestRate == null || depositGLCode == null || interestGLCode == null
                || savingsType.getId().equals(SavingsType.MANDATORY.getValue()) && recommendedAmount == null
                || getPrdApplicableMasterEnum() == ApplicableTo.GROUPS && recommendedAmntUnit == null) {
            throw new ProductDefinitionException("errors.create");
        }
        prdLogger.debug("Validating the fields in savings Offering done");
    }

    public SavingsProductDto toFullDto() {
        ProductDetailsDto details = super.toDetailsDto();
        Integer groupSavingsType = Integer.valueOf(0);
        boolean groupSavingsAccount = false;
        if (this.recommendedAmntUnit != null) {
            groupSavingsType = this.recommendedAmntUnit.getId().intValue();
            groupSavingsAccount = true;
        }

        PrdOfferingMeetingEntity interestCalculationPeriod = getTimePerForInstcalc();
        PrdOfferingMeetingEntity interestPosting = getFreqOfPostIntcalc();


        SavingsProductDto savingsProductDto = new SavingsProductDto(details, groupSavingsAccount, this.savingsType.getId().intValue(), groupSavingsType,
                this.recommendedAmount.getAmountDoubleValue(), this.maxAmntWithdrawl.getAmountDoubleValue(),
                BigDecimal.valueOf(this.interestRate), this.interestCalcType.getId().intValue(), interestCalculationPeriod.getMeeting().getRecurAfter().intValue(),
                interestCalculationPeriod.getMeeting().getRecurrenceType().getValue().intValue(), interestPosting.getMeeting().getRecurAfter().intValue(),
                this.minAmntForInt.getAmount(), this.depositGLCode.getGlcodeId().intValue(), this.interestGLCode.getGlcodeId().intValue());
        savingsProductDto.setDepositGlCodeValue(this.depositGLCode.getGlcode());
        savingsProductDto.setInterestGlCodeValue(this.interestGLCode.getGlcode());

        return savingsProductDto;
    }

    public void updateSavingsDetails(SavingsTypeEntity savingsType, Money recommendedAmount,
            RecommendedAmntUnitEntity recommendedAmntUnit, Money maxAmntWithdrawl,
            Double interestRate, InterestCalcTypeEntity interestCalcType,
            PrdOfferingMeetingEntity timePerForInstcalc, PrdOfferingMeetingEntity freqOfPostIntcalc,
            Money minAmntForInt) {
        this.savingsType = savingsType;
        this.recommendedAmount = recommendedAmount;
        this.recommendedAmntUnit = recommendedAmntUnit;
        this.maxAmntWithdrawl = maxAmntWithdrawl;
        this.interestRate = interestRate;
        this.interestCalcType = interestCalcType;
        this.minAmntForInt = minAmntForInt;

        this.savingsOfferingMeetings.remove(getTimePerForInstcalc());
        timePerForInstcalc.setPrdOffering(this);
        setTimePerForInstcalc(timePerForInstcalc);

        this.savingsOfferingMeetings.remove(getFreqOfPostIntcalc());
        freqOfPostIntcalc.setPrdOffering(this);
        setFreqOfPostIntcalc(freqOfPostIntcalc);
    }
}