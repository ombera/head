/*
 * Copyright (c) 2005-2009 Grameen Foundation USA
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
 
package org.mifos.test.acceptance.framework;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dbunit.Assertion;
import org.dbunit.DataSourceDatabaseTester;
import org.dbunit.DatabaseUnitException;
import org.dbunit.IDatabaseTester;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.SortedTable;
import org.dbunit.dataset.filter.DefaultColumnFilter;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.dbunit.operation.DatabaseOperation;
import org.dbunit.util.TableFormatter;
import org.mifos.core.MifosRuntimeException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testng.Assert;

/**
 * Utility methods for operating on DbUnit data.
 */
public class DbUnitUtilities {
    private static final Log LOG = LogFactory.getLog(DbUnitUtilities.class);
    
    static Map<String, String[]> columnsToIgnoreWhenVerifyingTables = new HashMap<String, String[]>();

    public DbUnitUtilities() {
        initialize();
    }
    
    private void initialize() {
        columnsToIgnoreWhenVerifyingTables.put("ACCOUNT_PAYMENT", new String[] { "payment_id","payment_date" });
        columnsToIgnoreWhenVerifyingTables.put("ACCOUNT_TRXN", new String[] { "account_trxn_id","created_date","action_date","payment_id", "due_date", "installment_id" });        
        columnsToIgnoreWhenVerifyingTables.put("CUSTOMER_ATTENDANCE", new String[] { "id", "meeting_date" });        
        columnsToIgnoreWhenVerifyingTables.put("FINANCIAL_TRXN", new String[] { "trxn_id","action_date", "account_trxn_id","balance_amount","posted_date", "debit_credit_flag", "fin_action_id" });        
        columnsToIgnoreWhenVerifyingTables.put("LOAN_ACTIVITY_DETAILS", new String[] { "id","created_date" });        
        columnsToIgnoreWhenVerifyingTables.put("LOAN_SCHEDULE", new String[] { "id","payment_date" });        
        columnsToIgnoreWhenVerifyingTables.put("LOAN_TRXN_DETAIL", new String[] { "account_trxn_id" });        
        columnsToIgnoreWhenVerifyingTables.put("CUSTOMER_FEE_SCHEDULE", new String[] { "account_fees_detail_id" });        
        columnsToIgnoreWhenVerifyingTables.put("FEE_TRXN_DETAIL", new String[] { "account_trxn_id", "account_fee_id", "fee_trxn_detail_id" });
        columnsToIgnoreWhenVerifyingTables.put("CUSTOMER_ACCOUNT_ACTIVITY", new String[] { "customer_account_activity_id", "created_date" });        
        columnsToIgnoreWhenVerifyingTables.put("CUSTOMER_TRXN_DETAIL", new String[] { "account_trxn_id" });        
        columnsToIgnoreWhenVerifyingTables.put("LOAN_SUMMARY", new String[] { "account_id" });        
        columnsToIgnoreWhenVerifyingTables.put("LOAN_SCHEDULE", new String[] { "action_date", "payment_date" });        
        columnsToIgnoreWhenVerifyingTables.put("ACCOUNT_STATUS_CHANGE_HISTORY", new String[] { "changed_date" });        
        
    }

    /** 
     * Compare two tables using DbUnit DataSets 
     * @param tableName
     * @param databaseDataSet
     * @param expectedDataSet
     * @throws DataSetException
     * @throws DatabaseUnitException
     */
    @SuppressWarnings({"PMD.SystemPrintln"}) // make sure we capture output independent of logging
    public void verifyTable(String tableName, IDataSet databaseDataSet, IDataSet expectedDataSet) throws DataSetException,
            DatabaseUnitException {
        
        Assert.assertNotNull(columnsToIgnoreWhenVerifyingTables.get(tableName), "Didn't find requested table [" + tableName + "] in columnsToIgnoreWhenVerifyingTables map.");
        ITable expectedTable = expectedDataSet.getTable(tableName);
        ITable actualTable = databaseDataSet.getTable(tableName);
        actualTable = DefaultColumnFilter.includedColumnsTable(actualTable, 
                expectedTable.getTableMetaData().getColumns());

        try {
            Assertion.assertEqualsIgnoreCols(expectedTable, actualTable, columnsToIgnoreWhenVerifyingTables.get(tableName));
        } catch (AssertionError e) {
            TableFormatter formatter = new TableFormatter();
            System.out.println("---Expected Table---");
            System.out.println(formatter.format(expectedTable));
            System.out.println("---Actual Table---");
            System.out.println(formatter.format(actualTable));
            throw e;
        }
    }

    /**
     * Convenience method for comparing multiple tables using DbUnit DataSets
     * @param tableNames
     * @param databaseDataSet
     * @param expectedDataSet
     * @throws DataSetException
     * @throws DatabaseUnitException
     */
    
    public void verifyTables(String[] tableNames, IDataSet databaseDataSet, IDataSet expectedDataSet) throws DataSetException,
    DatabaseUnitException {
        for (String tableName : tableNames) {
            this.verifyTable(tableName, databaseDataSet, expectedDataSet);
            
        }
     }

    public void verifySortedTable(String tableName, IDataSet databaseDataSet, 
            IDataSet expectedDataSet, String[] sortingColumns) throws DataSetException, DatabaseUnitException {
        Boolean actualDBSortFlag = true;
        Boolean expectedDBSortFlag = true;        
        verifySortedTableWithOrdering(tableName, databaseDataSet, expectedDataSet, sortingColumns, actualDBSortFlag, expectedDBSortFlag);                 
    }

    @SuppressWarnings({"PMD.SystemPrintln"}) // make sure we capture output independent of logging
    public void verifySortedTableWithOrdering(String tableName, IDataSet databaseDataSet, 
            IDataSet expectedDataSet, String[] sortingColumns, Boolean actualDBComparableFlag, Boolean expectedDBComparableFlag) throws DataSetException, DatabaseUnitException {

        Assert.assertNotNull(columnsToIgnoreWhenVerifyingTables.get(tableName), "Didn't find requested table [" + tableName + "] in columnsToIgnoreWhenVerifyingTables map.");
        ITable expectedTable = expectedDataSet.getTable(tableName);
        ITable actualTable = databaseDataSet.getTable(tableName);
        actualTable = DefaultColumnFilter.includedColumnsTable(actualTable, 
                expectedTable.getTableMetaData().getColumns());
        SortedTable sortedExpectedTable = new SortedTable(expectedTable, sortingColumns);
        sortedExpectedTable.setUseComparable(expectedDBComparableFlag);
        expectedTable = sortedExpectedTable;
        SortedTable sortedActualTable = new SortedTable(actualTable, sortingColumns);
        sortedActualTable.setUseComparable(actualDBComparableFlag);
        actualTable = sortedActualTable;
        
        if (LOG.isDebugEnabled()) {
            printTable(expectedTable);
            printTable(actualTable);
        }
        
        try {
            Assertion.assertEqualsIgnoreCols(expectedTable, actualTable, columnsToIgnoreWhenVerifyingTables.get(tableName));
        } catch (AssertionError e) {
            TableFormatter formatter = new TableFormatter();
            System.out.println("---Expected Table---");
            System.out.println(formatter.format(expectedTable));
            System.out.println("---Actual Table---");
            System.out.println(formatter.format(actualTable));
            throw e;
        }
    }
    
    public void printTable(ITable table) throws DataSetException {
       TableFormatter formatter = new TableFormatter();
       LOG.debug(formatter.format(table));
    }    
    
    public void loadDataFromFile(String filename, DriverManagerDataSource dataSource) 
    throws DatabaseUnitException, SQLException, IOException, URISyntaxException {
        Connection jdbcConnection = null;
        IDataSet dataSet = getDataSetFromFile(filename);
        try {
            jdbcConnection = DataSourceUtils.getConnection(dataSource);
            IDatabaseConnection databaseConnection = new DatabaseConnection(jdbcConnection);
            DatabaseOperation.CLEAN_INSERT.execute(databaseConnection, dataSet);
        }
        finally {
            if (jdbcConnection != null) {
                jdbcConnection.close();
            }
            DataSourceUtils.releaseConnection(jdbcConnection, dataSource);
        }
    }

    public IDataSet getDataSetFromFile(String filename)
    throws IOException, DataSetException, URISyntaxException {
        boolean enableColumnSensing = true;
        ClassPathResource resource = new ClassPathResource("/dataSets/" + filename);
        File file = resource.getFile();
        if (file == null) {
            throw new MifosRuntimeException("Couldn't find file:" + filename);
        }
        return new FlatXmlDataSet(
                getUncompressed(file), false, enableColumnSensing);
    }
    
    /**
     * Convenience method to get a DbUnit DataSet of only one table.
     * @param driverManagerDataSource 
     * @param tableName
     * @return
     * @throws Exception 
     * @throws Exception
     * @throws SQLException
     * @throws DataSetException
     */
    @SuppressWarnings("PMD.SignatureDeclareThrowsException") // one of the dependent methods throws Exception
    public IDataSet getDataSetForTable(DriverManagerDataSource driverManagerDataSource, String tableName) throws Exception  {
        return this.getDataSetForTables(driverManagerDataSource, new String[] { tableName });
    }

    /**
     * Returns a DbUnit DataSet for several tables.
     * @param driverManagerDataSource TODO
     * @param tableNames
     * @return
     * @throws Exception 
     */
    @SuppressWarnings("PMD.SignatureDeclareThrowsException") // one of the dependent methods throws Exception
    public IDataSet getDataSetForTables(DriverManagerDataSource driverManagerDataSource, String[] tableNames) throws Exception  {
        Connection jdbcConnection = null;
        IDataSet databaseDataSet = null;
        try {
            jdbcConnection = DataSourceUtils.getConnection(driverManagerDataSource);
            IDatabaseTester databaseTester = new DataSourceDatabaseTester(driverManagerDataSource);
            IDatabaseConnection databaseConnection = databaseTester.getConnection();
            databaseDataSet = databaseConnection.createDataSet(tableNames);
        }
        finally {
            jdbcConnection.close();
            DataSourceUtils.releaseConnection(jdbcConnection, driverManagerDataSource);
        }
        return databaseDataSet;
    }    
    
    private URL getUncompressed(File file) throws IOException {
        /* Buffer size based on this tutorial:
         * http://java.sun.com/developer/technicalArticles/Programming/compression/
         */
        int bufferSize = 2048;
        byte data[] = new byte[bufferSize];
        File tempFile = File.createTempFile("mifosDbUnitTempfile", ".tmp");
        tempFile.deleteOnExit();
        
        ZipFile zipFile = new ZipFile(file);
        Enumeration<? extends ZipEntry> zipEntries = zipFile.entries(); 
        ZipEntry zipEntry = zipEntries.nextElement();
        if (zipEntries.hasMoreElements()) {
            throw new MifosRuntimeException(
                    "only expecting one file entry per dataSet zip archive");
        }
        BufferedInputStream src =
            new BufferedInputStream(zipFile.getInputStream(zipEntry));
        BufferedOutputStream dest =
            new BufferedOutputStream(new FileOutputStream(tempFile), bufferSize);

        int count;
        while (true) {
            count = src.read(data, 0, bufferSize);
            if (count == -1) {
                break;
            }
            dest.write(data, 0, count);
        }
        dest.close();
        src.close();
        zipFile.close();
        
        return tempFile.toURI().toURL();
    }
    
}
