
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
			
package net.openvpn.als.networkplaces.store.http;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.openvpn.als.networkplaces.AbstractNetworkPlaceMount;
import net.openvpn.als.networkplaces.AbstractNetworkPlaceStore;
import net.openvpn.als.policyframework.LaunchSession;
import net.openvpn.als.vfs.utils.URI;
import net.openvpn.als.vfs.utils.URI.MalformedURIException;
import net.openvpn.als.vfs.webdav.DAVUtilities;


/**
 * <i>Network Place</i> store type that supports connections to HTTP 
 * servers.
 */
public class HTTPStore extends AbstractNetworkPlaceStore {
    final static Log log = LogFactory.getLog(HTTPStore.class);

	/**
	 * HTTP scheme name
	 */
	public final static String HTTP_SCHEME = "http";

	/**
	 * HTTPS scheme name
	 */
	public final static String HTTPS_SCHEME = "https";
    
    
    /**
     * Constructor.
     */
    public HTTPStore() {
        super(HTTP_SCHEME, "UTF-8");
    }

    /* (non-Javadoc)
     * @see net.openvpn.als.vfs.VFSStore#createURIFromPath(java.lang.String)
     */
    public URI createURIFromPath(String path) throws IllegalArgumentException {
		try {
			URI uri = new URI(DAVUtilities.processAndEncodeURI(path, getRepository().getSession()));
			if (HTTP_SCHEME.equals(uri.getScheme()) || HTTPS_SCHEME.equals(uri.getScheme())){
				return uri;
			}
		} 
		catch (MalformedURIException e) {
		}
        throw new IllegalArgumentException();
	}

    /* (non-Javadoc)
     * @see net.openvpn.als.vfs.AbstractNetworkPlaceStore#createMount(net.openvpn.als.policyframework.LaunchSession)
     */
    protected AbstractNetworkPlaceMount createMount(LaunchSession launchSession) throws Exception {
        return new HTTPMount(launchSession, this);
    }
}