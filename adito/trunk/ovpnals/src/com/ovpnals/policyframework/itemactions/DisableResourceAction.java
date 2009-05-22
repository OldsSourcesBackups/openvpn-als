
				/*
 *  OpenVPN-ALS
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
			
package com.ovpnals.policyframework.itemactions;

import com.ovpnals.policyframework.NoPermissionException;
import com.ovpnals.policyframework.Permission;
import com.ovpnals.policyframework.PolicyConstants;
import com.ovpnals.policyframework.Resource;
import com.ovpnals.policyframework.ResourceItem;
import com.ovpnals.policyframework.ResourceUtil;
import com.ovpnals.table.AvailableTableItemAction;

/**
 */
public class DisableResourceAction extends AbstractPathAction {
    /**
     */
    public static final String TABLE_ITEM_ACTION_ID = "disable";
    
    /**
     * Constructor
     * @param navigationContext
     * @param messageResourcesKey
     */
    public DisableResourceAction(int navigationContext, String messageResourcesKey) {
        this(navigationContext, messageResourcesKey, "{2}.do?actionTarget=disable&selectedResource={0}");
    }
    
    /**
     * @param navigationContext
     * @param messageResourcesKey
     * @param requiredPath
     */
    public DisableResourceAction(int navigationContext, String messageResourcesKey, String requiredPath) {
        super(TABLE_ITEM_ACTION_ID, messageResourcesKey, 175, false, navigationContext, requiredPath);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ovpnals.table.TableItemAction#isEnabled(com.ovpnals.table.AvailableTableItemAction)
     */
    public boolean isEnabled(AvailableTableItemAction availableItem) {
        try {
            ResourceItem item = (ResourceItem) availableItem.getRowItem();
            Permission[] permissions = new Permission[] {  PolicyConstants.PERM_CREATE_EDIT_AND_ASSIGN,  PolicyConstants.PERM_EDIT_AND_ASSIGN, PolicyConstants.PERM_ASSIGN };
            ResourceUtil.checkResourceManagementRights(item.getResource(), availableItem.getSessionInfo(), permissions);
            return isEnabled(item.getResource());
        }
        catch(NoPermissionException e) {
            return false;
        }
    }

    /**
     * @param resource
     * @return boolean
     */
    public boolean isEnabled(Resource resource) {
        return true;
    }
}