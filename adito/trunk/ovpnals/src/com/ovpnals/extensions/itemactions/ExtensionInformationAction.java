/**
 * 
 */
package com.ovpnals.extensions.itemactions;

import com.ovpnals.extensions.ExtensionBundleItem;
import com.ovpnals.policyframework.Permission;
import com.ovpnals.policyframework.PolicyConstants;
import com.ovpnals.security.SessionInfo;
import com.ovpnals.table.AvailableTableItemAction;
import com.ovpnals.table.TableItemAction;

public final class ExtensionInformationAction extends TableItemAction {

    public ExtensionInformationAction() {
        super("extensionInformation", "extensions", 400, "", true, SessionInfo.MANAGEMENT_CONSOLE_CONTEXT,
                        PolicyConstants.EXTENSIONS_RESOURCE_TYPE, new Permission[] { PolicyConstants.PERM_CHANGE });
    }

    public boolean isEnabled(AvailableTableItemAction availableItem) {
        ExtensionBundleItem item = (ExtensionBundleItem)availableItem.getRowItem();
        return item.getBundle().getInstructionsURL()!=null && !item.getBundle().getInstructionsURL().equals("") && !item.getSubFormName().equals("updateableExtensionsForm");
    }

    public String getOnClick(AvailableTableItemAction availableItem) {
        ExtensionBundleItem item = (ExtensionBundleItem)availableItem.getRowItem();
        return "window.open('" + item.getBundle().getInstructionsURL() + "')";
    }
}