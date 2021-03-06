
				/*
 *  Adito
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
			
package com.adito.networkplaces.store.jar;

import com.adito.networkplaces.NetworkPlacePlugin;
import com.adito.networkplaces.NetworkPlaceVFSProvider;
import com.adito.vfs.DefaultVFSProvider;
import com.adito.vfs.VFSProvider;

public class JarProvider extends DefaultVFSProvider implements NetworkPlaceVFSProvider {

    /**
     * Constructor.
     * 
     */
    public JarProvider() {
        super(JarStore.JAR_SCHEME,
            true,
            true,
            VFSProvider.ELEMENT_NOT_APPLICABLE,
            VFSProvider.ELEMENT_NOT_APPLICABLE,
            VFSProvider.ELEMENT_NOT_APPLICABLE,
            VFSProvider.ELEMENT_REQUIRED,
            JarStore.class,
            NetworkPlacePlugin.MESSAGE_RESOURCES_KEY);
    }

}
