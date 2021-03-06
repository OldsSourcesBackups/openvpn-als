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

package org.alfresco.jlan.util;

import java.util.Vector;

/**
 * Name/Value Pair List Class
 *
 * @author gkspencer
 */
public class NameValueList {

	//	List of name/value pairs
	
	private Vector<NameValue> m_list;
	
	/**
	 * Default constructor
	 */
	public NameValueList() {
		m_list = new Vector<NameValue>();
	}
	
	/**
	 * Add a name/value pair to the list
	 * 
	 * @param nameVal NameValue
	 */
	public final void addItem(NameValue nameVal) {
		m_list.add(nameVal);
	}
	
	/**
	 * Return the count of items in the list
	 * 
	 * @return int
	 */
	public final int numberOfItems() {
		return m_list.size();
	}

	/**
	 * Return the specified item
	 * 
	 * @param idx
	 * @return NameValue
	 */
	public final NameValue getItemAt(int idx) {
		if ( idx < 0 || idx >= m_list.size())
			return null;
		return m_list.get(idx);
	}
		
	/**
	 * Find an item in the list
	 * 
	 * @param name String
	 * @return NameValue
	 */
	public final NameValue findItem(String name) {
		for ( int i = 0; i < m_list.size(); i++) {
			NameValue nameVal = m_list.get(i);
			if ( nameVal.getName().compareTo(name) == 0)
				return nameVal;
		}
		return null;
	}

	/**
	 * Find all items with the specified name and return as a new list
	 * 
	 * @param name String
	 * @return NameValueList
	 */
	public final NameValueList findAllItems(String name) {
		
		//	Allocate the list to hold the matching items
		
		NameValueList list = new NameValueList();
		
		//	Find the matching items
		
		for ( int i = 0; i < m_list.size(); i++) {
			NameValue nameVal = m_list.get(i);
			if ( nameVal.getName().compareTo(name) == 0)
				list.addItem(nameVal);
		}
		
		//	Check if the list is empty, return the list
		
		if ( list.numberOfItems() == 0)
			list = null;
		return list;
	}
	
	/**
	 * Find an item in the list using a caseless search
	 * 
	 * @param name String
	 * @return NameValue
	 */
	public final NameValue findItemCaseless(String name) {
		for ( int i = 0; i < m_list.size(); i++) {
			NameValue nameVal = m_list.get(i);
			if ( nameVal.getName().equalsIgnoreCase(name))
				return nameVal;
		}
		return null;
	}
	
	/**
	 * Remote all items from the list
	 */
	public final void removeAllItems() {
		m_list.removeAllElements();
	}
  
  /**
   * Return the name/value list as a string
   * 
   * @return String
   */
  public String toString() {
    StringBuffer str = new StringBuffer(256);
    
    str.append("[");
    for ( int i = 0; i < numberOfItems(); i++) {
      if ( str.length() > 1)
        str.append( ",");
      NameValue nameVal = getItemAt( i);
      
      str.append(nameVal.getName());
      str.append( "=");
      str.append( nameVal.getValue());
    }
    str.append( "]");
    
    return str.toString();
  }
}
