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

package org.rubato.xml;

import org.w3c.dom.Element;

/**
 * An XMLElementReader reads one kind of top-level element of a Rubato file,
 * such as networks or macro rubettes, that the core library cannot read
 * itself. The application provides them, either in a
 * <code>META-INF/services/org.rubato.xml.XMLElementReader</code> file, which
 * XMLReader reads through java.util.ServiceLoader, or by calling
 * {@link XMLReader#registerElementReader}.
 * XMLReader keeps what each reader returns, in document order, and returns
 * it from {@link XMLReader#getElementObjects}.
 */
public interface XMLElementReader {

    /**
     * Returns the name of the top-level element this reader reads,
     * for example "Network".
     */
    public String getElementName();
    
    
    /**
     * Reads the given element. Report problems through
     * {@link XMLReader#setError(String, Object...)}.
     * @return the object read from the element, or null if there is none
     */
    public Object fromXML(XMLReader reader, Element element);
}
