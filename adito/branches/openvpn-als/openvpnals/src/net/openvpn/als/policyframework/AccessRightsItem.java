
				/*
 *  OpenVPNALS
 *
 *  Copyright (C) 2003-2006 3SP LTD. All Rights Reserved
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2 of
 *  the License, or (at your option) any later version.
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
			
package net.openvpn.als.policyframework;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import net.openvpn.als.core.CoreUtil;


/**
 * Implementation of a {@link net.openvpn.als.policyframework.ResourceItem}
 * which represents a {@link net.openvpn.als.policyframework.AccessRights}.
 */
public class AccessRightsItem extends ResourceItem<AccessRights> {

    /**
     * Construct a new access rights item with the specified access rights.
     * 
     * @param resource the access rights this item represents.
     * @param policies the policies attached to this item.
     */
    public AccessRightsItem(AccessRights resource, List<Policy> policies) {
        super(resource, policies);
    }

    public Object getColumnValue(int col) {
        if(col == 2) {
           return getResource().getAccessRightsClass();
        }
        return super.getColumnValue(col);
    }

    @Override
    public String getSmallIconPath(HttpServletRequest request) {
        return CoreUtil.getThemePath(request.getSession()) + "/images/actions/" + getResource().getAccessRightsClass() + ".gif";
    }
}