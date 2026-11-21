/*
 * Copyright (C) 2026 Simon Koch-Sultan
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of version 2 of the GNU General Public
 * License as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 *
 */

package org.rubato.composer.network;

import static org.rubato.xml.XMLConstants.NETWORK;

import java.util.ArrayList;
import java.util.List;

import org.rubato.xml.XMLElementReader;
import org.rubato.xml.XMLReader;
import org.w3c.dom.Element;

/**
 * Reads the toplevel network elements of a Rubato file for XMLReader,
 * which finds this class through META-INF/services.
 */
public class NetworkXMLReader implements XMLElementReader {

    public String getElementName() {
        return NETWORK;
    }
    
    
    public Object fromXML(XMLReader reader, Element element) {
        return NetworkModel.fromXML(reader, element);
    }
    
    
    /**
     * Returns the networks the given reader parsed, in document order.
     */
    public static List<NetworkModel> getNetworks(XMLReader reader) {
        List<NetworkModel> networks = new ArrayList<NetworkModel>();
        for (Object object : reader.getElementObjects(NETWORK)) {
            networks.add((NetworkModel)object);
        }
        return networks;
    }
}
