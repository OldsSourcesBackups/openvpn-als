/*
 * Copyright (C) 2005-2007 Alfresco Software Limited.
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
package org.alfresco.util;

/**
 * Utility class providing helper methods for various types of <code>equals</code> functionality
 * 
 * @author Derek Hulley
 */
public class EqualsHelper
{
    /**
     * Performs an equality check <code>left.equals(right)</code> after checking for null values
     * 
     * @param left the Object appearing in the left side of an <code>equals</code> statement
     * @param right the Object appearing in the right side of an <code>equals</code> statement
     * @return Return true or false even if one or both of the objects are null
     */
	public static boolean nullSafeEquals(Object left, Object right)
    {
        return (left == right) || (left != null && right != null && left.equals(right));
    }
}
