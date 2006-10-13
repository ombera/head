package org.mifos.application.accounts.loan.struts.action;

import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.mifos.application.accounts.business.AccountActionDateEntity;
import org.mifos.application.accounts.business.AccountBO;
import org.mifos.application.accounts.loan.business.LoanBO;
import org.mifos.application.accounts.loan.business.LoanSummaryEntity;
import org.mifos.application.accounts.loan.util.helpers.LoanConstants;
import org.mifos.application.accounts.util.helpers.AccountStates;
import org.mifos.application.customer.business.CustomerBO;
import org.mifos.application.meeting.business.MeetingBO;
import org.mifos.application.productdefinition.business.LoanOfferingBO;
import org.mifos.framework.MifosMockStrutsTestCase;
import org.mifos.framework.hibernate.helper.HibernateUtil;
import org.mifos.framework.security.util.UserContext;
import org.mifos.framework.util.helpers.Constants;
import org.mifos.framework.util.helpers.Flow;
import org.mifos.framework.util.helpers.FlowManager;
import org.mifos.framework.util.helpers.Money;
import org.mifos.framework.util.helpers.ResourceLoader;
import org.mifos.framework.util.helpers.SessionUtils;
import org.mifos.framework.util.helpers.TestObjectFactory;

public class TestRepayLoanAction extends MifosMockStrutsTestCase {

	protected AccountBO accountBO = null;

	private CustomerBO center = null;

	private CustomerBO group = null;
	private UserContext userContext;
	private String flowKey;
	
	// success
	protected void setUp() throws Exception {
		super.setUp();
		try {

			setServletConfigFile(ResourceLoader.getURI("WEB-INF/web.xml")
					.getPath());
			setConfigFile(ResourceLoader.getURI(
					"org/mifos/application/accounts/struts-config.xml")
					.getPath());
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		userContext = TestObjectFactory.getContext();
		request.getSession().setAttribute(Constants.USERCONTEXT, userContext);
		addRequestParameter("recordLoanOfficerId", "1");
		addRequestParameter("recordOfficeId", "1");
		request.getSession(false).setAttribute("ActivityContext", TestObjectFactory.getActivityContext());
		Flow flow = new Flow();
		flowKey = String.valueOf(System.currentTimeMillis());
		FlowManager flowManager = new FlowManager();
		flowManager.addFLow(flowKey, flow);
		request.getSession(false).setAttribute(Constants.FLOWMANAGER,
				flowManager);	
		accountBO = getLoanAccount();
		HibernateUtil.getSessionTL().flush();
		HibernateUtil.closeSession();
		accountBO=(AccountBO)HibernateUtil.getSessionTL().get(AccountBO.class,accountBO.getAccountId());
	}
	
	protected void tearDown() throws Exception {
		accountBO=(AccountBO)HibernateUtil.getSessionTL().get(AccountBO.class,accountBO.getAccountId());
		group=(CustomerBO)HibernateUtil.getSessionTL().get(CustomerBO.class,group.getCustomerId());
		center=(CustomerBO)HibernateUtil.getSessionTL().get(CustomerBO.class,center.getCustomerId());
		TestObjectFactory.cleanUp(accountBO);
		TestObjectFactory.cleanUp(group);
		TestObjectFactory.cleanUp(center);
		HibernateUtil.closeSession();
		super.tearDown();
	}
	
	public void testLoadRepayment() throws Exception{
		request.setAttribute(Constants.CURRENTFLOWKEY, flowKey);
		setRequestPathInfo("/repayLoanAction");
		addRequestParameter("method", "loadRepayment");
		addRequestParameter("globalAccountNum", accountBO.getGlobalAccountNum());
		addRequestParameter(Constants.CURRENTFLOWKEY, (String) request.getAttribute(Constants.CURRENTFLOWKEY));
		actionPerform();
		verifyForward(Constants.LOAD_SUCCESS);
		Money amount =(Money)SessionUtils.getAttribute(LoanConstants.TOTAL_REPAYMENT_AMOUNT,request);
		assertEquals(amount,((LoanBO)accountBO).getTotalEarlyRepayAmount());		
	}
	
	public void testRepaymentPreview(){
		request.setAttribute(Constants.CURRENTFLOWKEY, flowKey);
		setRequestPathInfo("/repayLoanAction");
		addRequestParameter("method", "preview");
		addRequestParameter(Constants.CURRENTFLOWKEY, (String) request.getAttribute(Constants.CURRENTFLOWKEY));
		actionPerform();
		verifyForward(Constants.PREVIEW_SUCCESS);
	}
	
	public void testRepaymentPrevious(){
		request.setAttribute(Constants.CURRENTFLOWKEY, flowKey);
		setRequestPathInfo("/repayLoanAction");
		addRequestParameter("method", "previous");
		addRequestParameter(Constants.CURRENTFLOWKEY, (String) request.getAttribute(Constants.CURRENTFLOWKEY));
		actionPerform();
		verifyForward(Constants.PREVIOUS_SUCCESS);
	}
	
	public void testMakeRepaymentForCurrentDateSameAsInstallmentDate(){
		request.setAttribute(Constants.CURRENTFLOWKEY, flowKey);
		Money amount=((LoanBO)accountBO).getTotalEarlyRepayAmount();
		setRequestPathInfo("/repayLoanAction");
		addRequestParameter("method", "makeRepayment");
		addRequestParameter("globalAccountNum",accountBO.getGlobalAccountNum());
		addRequestParameter("paymentTypeId","1");
		addRequestParameter(Constants.CURRENTFLOWKEY, (String) request.getAttribute(Constants.CURRENTFLOWKEY));
		actionPerform();
		verifyForward(Constants.UPDATE_SUCCESS);
		
		assertEquals(accountBO.getAccountState().getId(),new Short(AccountStates.LOANACC_OBLIGATIONSMET));
		
		LoanSummaryEntity loanSummaryEntity=((LoanBO)accountBO).getLoanSummary();
		assertEquals(amount,loanSummaryEntity.getPrincipalPaid().add(loanSummaryEntity.getFeesPaid()).add(loanSummaryEntity.getInterestPaid()).add(loanSummaryEntity.getPenaltyPaid()));
		
	}
	
	public void testMakeRepaymentForCurrentDateLiesBetweenInstallmentDates(){
		request.setAttribute(Constants.CURRENTFLOWKEY, flowKey);
		changeFirstInstallmentDate(accountBO);
		
		Money amount=((LoanBO)accountBO).getTotalEarlyRepayAmount();
	
		setRequestPathInfo("/repayLoanAction");
		addRequestParameter("method", "makeRepayment");
		addRequestParameter("globalAccountNum",accountBO.getGlobalAccountNum());
		addRequestParameter("paymentTypeId","1");
		addRequestParameter(Constants.CURRENTFLOWKEY, (String) request.getAttribute(Constants.CURRENTFLOWKEY));
		actionPerform();
		verifyForward(Constants.UPDATE_SUCCESS);
		
		assertEquals(accountBO.getAccountState().getId(),new Short(AccountStates.LOANACC_OBLIGATIONSMET));
		
		LoanSummaryEntity loanSummaryEntity=((LoanBO)accountBO).getLoanSummary();
		assertEquals(amount,loanSummaryEntity.getPrincipalPaid().add(loanSummaryEntity.getFeesPaid()).add(loanSummaryEntity.getInterestPaid()).add(loanSummaryEntity.getPenaltyPaid()));
		
	}
	
	private void changeFirstInstallmentDate(AccountBO accountBO){
		Calendar currentDateCalendar = new GregorianCalendar();
		int year = currentDateCalendar.get(Calendar.YEAR);
		int month = currentDateCalendar.get(Calendar.MONTH);
		int day = currentDateCalendar.get(Calendar.DAY_OF_MONTH-1);
		currentDateCalendar = new GregorianCalendar(year, month, day);
		for(AccountActionDateEntity accountActionDateEntity:accountBO.getAccountActionDates()){
			accountActionDateEntity.setActionDate(new java.sql.Date(currentDateCalendar.getTimeInMillis()));
			break;
		}
	}
	
	private AccountBO getLoanAccount() {
		MeetingBO meeting = TestObjectFactory.createMeeting(TestObjectFactory
				.getMeetingHelper(1, 1, 4, 2));
		center = TestObjectFactory.createCenter("Center", Short.valueOf("13"),
				"1.1", meeting, new Date(System.currentTimeMillis()));
		group = TestObjectFactory.createGroup("Group", Short.valueOf("9"),
				"1.1.1", center, new Date(System.currentTimeMillis()));
		LoanOfferingBO loanOffering = TestObjectFactory.createLoanOffering(
				"Loan", Short.valueOf("2"),
				new Date(System.currentTimeMillis()), Short.valueOf("1"),
				300.0, 1.2, Short.valueOf("3"), Short.valueOf("1"), Short
						.valueOf("1"), Short.valueOf("1"), Short.valueOf("1"),
				Short.valueOf("1"), meeting);
		return TestObjectFactory.createLoanAccount("42423142341", group, Short
				.valueOf("5"), new Date(System.currentTimeMillis()),
				loanOffering);
	}


}
