/*
* JBoss, a division of Red Hat
* Copyright 2006, Red Hat Middleware, LLC, and individual contributors as indicated
* by the @authors tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
*
* This is free software; you can redistribute it and/or modify it
* under the terms of the GNU Lesser General Public License as
* published by the Free Software Foundation; either version 2.1 of
* the License, or (at your option) any later version.
*
* This software is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this software; if not, write to the Free
* Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
* 02110-1301 USA, or see the FSF site: http://www.fsf.org.
*/
package org.gatein.sso.agent.opensso;

import java.util.Properties;

import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Cookie;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.exoplatform.web.security.Credentials;
import org.gatein.sso.agent.GenericSSOAgent;

/**
 * @author <a href="mailto:sshah@redhat.com">Sohil Shah</a>
 */
public class OpenSSOAgent
{
	private static Logger log = Logger.getLogger(OpenSSOAgent.class);
	private static OpenSSOAgent singleton;
	
	private String cookieName;	
	private String openSSOUrl;
	
	private OpenSSOAgent()
	{
		//TODO: make this part of externally configured properties
		this.cookieName = "iPlanetDirectoryPro";
		this.openSSOUrl = "http://localhost:8888/opensso";
	}
	
	public static OpenSSOAgent getInstance()
	{
		if(OpenSSOAgent.singleton == null)
		{
			synchronized(OpenSSOAgent.class)
			{
				if(OpenSSOAgent.singleton == null)
				{										
					OpenSSOAgent.singleton = new OpenSSOAgent();
				}
			}
		}
		return OpenSSOAgent.singleton;
	}
		
	public String getCookieName()
	{
		return cookieName;
	}

	public void setCookieName(String cookieName)
	{
		this.cookieName = cookieName;
	}
	
	public String getOpenSSOUrl()
	{
		return openSSOUrl;
	}

	public void setOpenSSOUrl(String openSSOUrl)
	{
		this.openSSOUrl = openSSOUrl;
	}

	public void validateTicket(HttpServletRequest httpRequest) throws Exception
	{						
		String token = null;
		Cookie[] cookies = httpRequest.getCookies();
		for(Cookie cookie: cookies)
		{
			if(cookie.getName().equals(this.cookieName))
			{
				token = cookie.getValue();
				break;
			}
		}
						
		if(token != null)
		{
			boolean isValid = this.isTokenValid(token);
			
			if(!isValid)
			{
				throw new IllegalStateException("OpenSSO Token is not valid!!");
			}
		
			String subject = this.getSubject(token);			
			if(subject != null)
			{
				Credentials credentials = new Credentials(subject, "");
				httpRequest.getSession().setAttribute(GenericSSOAgent.CREDENTIALS, credentials);
			}
		}
	}	
	
	private boolean isTokenValid(String token) throws Exception
	{
		HttpClient client = new HttpClient();
		PostMethod post = null;
		try
		{			
			String url = this.openSSOUrl+"/identity/isTokenValid";
			post = new PostMethod(url);
			post.addParameter("tokenid", token);
			
			int status = client.executeMethod(post);
			String response = post.getResponseBodyAsString();
			
			log.info("-------------------------------------------------------");
			log.info("Status: "+status);
			log.info("Response: "+response);
			log.info("-------------------------------------------------------");
			
			if(response.contains(Boolean.TRUE.toString()))
			{
				return true;
			}
			
			return false;
		}
		finally
		{
			if(post != null)
			{
				post.releaseConnection();
			}
		}
	}	
	
	private String getSubject(String token) throws Exception
	{
		HttpClient client = new HttpClient();
		PostMethod post = null;
		try
		{			
			String url = this.openSSOUrl+"/identity/attributes";
			post = new PostMethod(url);
			post.addParameter("subjectid", token);
			post.addParameter("attributes_names", "uid");
			
			int status = client.executeMethod(post);
			String response = post.getResponseBodyAsString();
			
			log.debug("Must Just Read the uid attribute-------------------------------------------------------");
			log.debug("Status: "+status);
			log.debug("Response: "+response);
			
			
			return "demo";
		}
		finally
		{
			if(post != null)
			{
				post.releaseConnection();
			}
		}		
	}
}
