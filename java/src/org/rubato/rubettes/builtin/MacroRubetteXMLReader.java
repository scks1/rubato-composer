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

package org.rubato.rubettes.builtin;

import static org.rubato.xml.XMLConstants.RUBETTE;

import java.util.ArrayList;
import java.util.List;

import org.rubato.base.Rubette;
import org.rubato.xml.XMLElementReader;
import org.rubato.xml.XMLReader;
import org.w3c.dom.Element;

/**
 * Reads the toplevel (macro) rubette elements of a Rubato file for
 * XMLReader, which finds this class through META-INF/services.
 */
public class MacroRubetteXMLReader implements XMLElementReader {

    public String getElementName() {
        return RUBETTE;
    }
    
    
    public Object fromXML(XMLReader reader, Element rubetteNode) {
        String name = rubetteNode.getAttribute("name"); //$NON-NLS-1$
        if (name.length() == 0) {
            reader.setError("Rubette must have a name");
            return null;
        }
        MacroRubette rubette = new MacroRubette();
        MacroRubette nrubette = (MacroRubette)rubette.fromXML(reader, rubetteNode);
        nrubette.setName(name);
        return nrubette;
    }
    
    
    /**
     * Returns the macro rubettes the given reader parsed, in document order.
     */
    public static List<Rubette> getRubettes(XMLReader reader) {
        List<Rubette> rubettes = new ArrayList<Rubette>();
        for (Object object : reader.getElementObjects(RUBETTE)) {
            rubettes.add((Rubette)object);
        }
        return rubettes;
    }
}
