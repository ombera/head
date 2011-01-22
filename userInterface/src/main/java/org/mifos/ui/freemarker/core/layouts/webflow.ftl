[#ftl]
[#--
* Copyright (c) 2005-2010 Grameen Foundation USA
*  All rights reserved.
*
*  Licensed under the Apache License, Version 2.0 (the "License");
*  you may not use this file except in compliance with the License.
*  You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing, software
*  distributed under the License is distributed on an "AS IS" BASIS,
*  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
*  implied. See the License for the specific language governing
*  permissions and limitations under the License.
*
*  See also http://www.apache.org/licenses/LICENSE-2.0.html for an
*  explanation of the license and how it is applied.
--]

[#macro webflow states currentState]
[@mifos.header "title" /]
[@mifos.topNavigationNoSecurity currentTab="ClientsAndAccounts" /]

<div class="content">
	<!-- flow progress indicator -->
	<div class="bread-crumb bordered">
		<ul>
		[#assign currentStateEncountered = false]
		[#list states as state]
			[#assign cssClass = "incomplete"]
			[#if state == currentState]
				[#assign currentStateEncountered = true]
				[#assign cssClass = "active"]
			[#elseif currentStateEncountered == false]
				[#assign cssClass = "completed"]
			[/#if]
			<li class="${cssClass}">[@spring.message state /]</li>
		[/#list]
		</ul>
	</div>
	<div class="clearfix"/>
	
	<!-- flow content -->
	<div class="flow-content bordered">
		[#nested]
	</div>
</div>

[@mifos.footer /]
[/#macro]
