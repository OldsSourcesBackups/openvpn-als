/*
 * Copyright (C) 2006-2008 Alfresco Software Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 * As a special exception to the terms and conditions of version 2.0 of 
 * the GPL, you may redistribute this Program in connection with Free/Libre 
 * and Open Source Software ("FLOSS") applications as described in Alfresco's 
 * FLOSS exception.  You should have recieved a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */

package org.alfresco.jlan.server.auth.acl;

import java.util.StringTokenizer;

import org.alfresco.jlan.server.SrvSession;
import org.alfresco.jlan.server.core.SharedDevice;

/**
 * Access Control Base Class
 * 
 * <p>Controls access to a shared filesystem.
 *
 * @author gkspencer
 */
public abstract class AccessControl {

	//	Access control type/status
	
	public final static int NoAccess	= 0;
	public final static int ReadOnly	= 1;
	public final static int ReadWrite	= 2;
	
	public final static int MaxLevel	= 2;

	//	Default access status, indicates that the access conrol did not apply
		
	public final static int Default		= -1;
	
	//	Access type strings
	
	private final static String[] _accessType = { "None", "Read", "Write" };
	
	//	Access control name and type
	
	private String m_name;
	private String m_type;
	
	//	Access type
	
	private int m_access;
	
    /**
     * Default constructor to allow container initialization.
     */
    protected AccessControl()
    {        
    }

    /**
	 * Class constructor
	 * 
	 * @param name String
	 * @param type String
	 * @param access int
	 */
	protected AccessControl(String name, String type, int access) {
		setName(name);
		setType(type);
		
		m_access = access;
	}
	
	/**
	 * Return the access control name
	 *
	 * @return String
	 */
	public final String getName() {
		return m_name;
	}
	
	/**
	 * Return the access control type
	 * 
	 * @return String
	 */
	public final String getType() {
		return m_type;
	}
	
	/**
	 * Return the access control check type
	 * 
	 * @return int
	 */
	public final int getAccess() {
		return m_access;
	}
	
	/**
	 * Return the access control check type as a string
	 * 
	 * @return String
	 */
	public final String getAccessString() {
		return _accessType[m_access];
	}
	
	/**
	 * Check if the specified session has access to the shared device.
	 *
	 * @param sess SrvSession
	 * @param share SharedDevice
	 * @param mgr AccessControlManager
	 * @return int
	 */
	public abstract int allowsAccess(SrvSession sess, SharedDevice share, AccessControlManager mgr);

	/**
	 * Return the index of a value from a list of valid values, or 01 if not valid
	 * 
	 * @param val String
	 * @param valid String[]
	 * @param caseSensitive boolean
	 * @return int
	 */	
	protected final static int indexFromList(String val, String[] valid, boolean caseSensitive) {

		//	Check if the value is valid
		
		if ( val == null || val.length() == 0)
			return -1;
					
		//	Search for the matching value in the valid list
		
		for ( int i = 0; i < valid.length; i++) {
			
			// Check the current value in the valid list
			
			if ( caseSensitive) {
				if ( valid[i].equals(val))
					return i;
			}
			else if ( valid[i].equalsIgnoreCase(val))
				return i;	
		}
		
		//	Value does not match any of the valid values
		
		return -1;
	}

	/**
	 * Create a list of valid strings from a comma delimeted list
	 * 
	 * @param str String
	 * @return String[]
	 */	
	protected final static String[] listFromString(String str) {
		
		//	Check if the string is valid
		
		if ( str == null || str.length() == 0)
			return null;
			
		//	Split the comma delimeted string into an array of strings
		
		StringTokenizer token = new StringTokenizer(str, ",");
		int numStrs = token.countTokens();
		if ( numStrs == 0)
			return null;
			
		String[] list = new String[numStrs];
		
		//	Parse the string into a list of strings
		
		int i = 0;
		
		while ( token.hasMoreTokens())
			list[i++] = token.nextToken();
			
		//	Return the string list
		
		return list;			
	}

	/**
	 * Set the access control type
	 * 
	 * @param typ String
	 */
	public final void setType(String typ) {
		m_type = typ;
	}
	
	/**
	 * Set the access control name
	 * 
	 * @param name String
	 */
	public final void setName(String name) {
		m_name = name;	
	}
	
	/**
     * Set the access level from a String
     * 
     * @param accessLevel
     * @throws ACLParseException
     */
    public void setAccess(String accessLevel) throws ACLParseException
    {
        this.m_access = AccessControlParser.parseAccessTypeString(accessLevel);
    }

    /**
	 * Return the access control type as a string
	 *
	 * @param access int 
	 * @return String
	 */
	public static final String asAccessString(int access) {
		if ( access == Default)
			return "Default";
		return _accessType[access];
	}
	
	/**
	 * Return the access control as a string
	 * 
	 * @return String
	 */
	public String toString() {
		StringBuffer str = new StringBuffer();
		
		str.append("[");
		str.append(getType());
		str.append(":");
		str.append(getName());
		str.append(",");
		str.append(getAccessString());
		str.append("]");
		
		return str.toString();
	}
}
