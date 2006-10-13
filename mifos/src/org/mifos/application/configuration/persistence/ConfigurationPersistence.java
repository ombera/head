/**

 * ConfigurationPersistence.java    version: 1.0

 

 * Copyright (c) 2005-2006 Grameen Foundation USA

 * 1029 Vermont Avenue, NW, Suite 400, Washington DC 20005

 * All rights reserved.

 

 * Apache License 
 * Copyright (c) 2005-2006 Grameen Foundation USA 
 * 

 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 
 *

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the 

 * License. 
 * 
 * See also http://www.apache.org/licenses/LICENSE-2.0.html for an explanation of the license 

 * and how it is applied. 

 *

 */
package org.mifos.application.configuration.persistence;

import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.mifos.application.NamedQueryConstants;
import org.mifos.application.master.business.LookUpLabelEntity;
import org.mifos.application.master.business.LookUpValueEntity;
import org.mifos.application.master.business.LookUpValueLocaleEntity;
import org.mifos.application.master.business.MifosLookUpEntity;
import org.mifos.application.master.business.SupportedLocalesEntity;
import org.mifos.framework.hibernate.helper.HibernateUtil;
import org.mifos.framework.persistence.Persistence;

/**
 * 
 *
 */
public class ConfigurationPersistence extends Persistence {
	
	
	
	public List<MifosLookUpEntity> getLookupEntities(){
		
		List<MifosLookUpEntity> entities=null;
		try
		{
		Session session = HibernateUtil.getSessionTL();
		 entities = session.getNamedQuery(
				NamedQueryConstants.GET_ENTITIES).list();
		 
			for (MifosLookUpEntity entity : entities) {
				Set<LookUpLabelEntity> labels = entity.getLookUpLabels();
				entity.getEntityType();
				for (LookUpLabelEntity label : labels) {
					 label.getLabelName();
					 label.getLocaleId();
				}
			}
		} finally {
			HibernateUtil.closeSession();	
		}
		
		return entities;
	}
	public List<LookUpValueEntity> getLookupValues(){
		List<LookUpValueEntity> values=null;
		try{
		Session session = HibernateUtil.getSessionTL();
		 values = session.getNamedQuery(
				NamedQueryConstants.GET_LOOKUPVALUES).list();
		 
			for (LookUpValueEntity value : values) {
				Set<LookUpValueLocaleEntity> localeValues = value
						.getLookUpValueLocales();
				value.getLookUpName();
				for (LookUpValueLocaleEntity locale : localeValues) {

					 locale.getLookUpValue();
					 locale.getLocaleId();
				}

			}

		}
		finally{
			HibernateUtil.closeSession();
		}
		return values;
	}
	
	public List<SupportedLocalesEntity> getSupportedLocale(){
		List<SupportedLocalesEntity> locales=null;
		try{
		Session session = HibernateUtil.getSessionTL();

		 locales = session.getNamedQuery(
				NamedQueryConstants.SUPPORTED_LOCALE_LIST).list();
		 
			for (SupportedLocalesEntity locale : locales) {

				locale.getLanguage().getLanguageShortName();
				locale.getCountry().getCountryShortName();
				locale.getLocaleId();

			}
		} finally{
			HibernateUtil.closeSession();

		}
		
		return locales;

	}
}
