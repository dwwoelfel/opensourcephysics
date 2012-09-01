/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.tools;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.opensourcephysics.controls.XML;
import org.opensourcephysics.controls.XMLControl;
import org.opensourcephysics.controls.XMLControlElement;

/**
 * This represents a catalog of digital library records.
 *
 * @author Douglas Brown
 * @version 1.0
 */
public class LibraryCatalog extends LibraryRecord {
	
	protected static Icon folderIcon;
	protected static String infoFileName = "catalog_info.html"; //$NON-NLS-1$
	
	static {
    String imageFile = "/org/opensourcephysics/resources/tools/images/whitefolder.gif";        //$NON-NLS-1$
    folderIcon = new ImageIcon(LibraryCatalog.class.getResource(imageFile));
	}
	
  private ArrayList<LibraryRecord> records = new ArrayList<LibraryRecord>();
    
  public LibraryCatalog(String name) {
  	super(name);
  }
    
	@Override
  public void setName(String theName) {
		name = theName==null? ToolsRes.getString("LibraryCatalog.Name.Default"): theName; //$NON-NLS-1$
	}
	
	public Icon getIcon() {
		return folderIcon;
	}

	public void addRecord(LibraryRecord record) {
  	if (!records.contains(record)) {
  		records.add(record);
  	}
  }
  
	public void removeRecord(LibraryRecord record) {
  	records.remove(record);
  }
  
	public LibraryRecord[] getRecords() {
		return records.toArray(new LibraryRecord[records.size()]);
	}
	
	public String getDescription() {
		String desc = super.getDescription();
		if (desc!=null)
			return desc;
		return ToolsRes.getString("LibraryCatalog.Description"); //$NON-NLS-1$
	}
  	
  public static String getCatalogName(String path) {
  	if (path==null)
  		return ToolsRes.getString("LibraryCatalog.Name.Default"); //$NON-NLS-1$
  	// first try to read XMLControl and get name of root
  	XMLControl control = new XMLControlElement(path);
  	if (!control.failedToRead() && control.getObjectClass()==LibraryCatalog.class) {
  		LibraryCatalog catalog = (LibraryCatalog)control.loadObject(null);
  		if (catalog.getName()!=null && !catalog.getName().trim().equals("")) //$NON-NLS-1$
  			return catalog.getName();
  	}
  	// try to get name from title of html info page, if any
  	String basePath = XML.getDirectoryPath(path);
  	String name = null;
  	String base = LibraryRecord.getURI(basePath);
  	String infoPath = XML.getResolvedPath(infoFileName, base);
  	Resource res = ResourceLoader.getResource(infoPath);
    if (res!=null) try {
      BufferedReader in = new BufferedReader(res.openReader());
      String line = in.readLine();
      while(line!=null) {
      	int n = line.indexOf("<title>"); //$NON-NLS-1$
      	if (n>-1) {
      		name = line.substring(n+7);
        	n = name.indexOf("</title>"); //$NON-NLS-1$
        	if (n>-1) {
        		name = name.substring(0, n);
        	}
      		break;
      	}
        line = in.readLine();
      }
      in.close();
    } catch(IOException ex) {
    }
    if (name==null) {
    	name = basePath;
    }
  	return name;
  }

  /**
   * Returns an ObjectLoader to save and load data for this class.
   *
   * @return the object loader
   */
  public static XML.ObjectLoader getLoader() {
    return new Loader();
  }

  /**
   * The ObjectLoader class to save and load LibraryCatalog data.
   */
  static class Loader implements XML.ObjectLoader {

    /**
     * Saves an object's data to an XMLControl.
     *
     * @param control the control to save to
     * @param obj the object to save
     */
    public void saveObject(XMLControl control, Object obj) {
    	XML.getLoader(LibraryRecord.class).saveObject(control, obj);
    	LibraryCatalog catalog = (LibraryCatalog)obj;
    	if (!catalog.records.isEmpty()) {
	    	control.setValue("records", catalog.getRecords()); //$NON-NLS-1$
    	}
    }
    
    /**
     * Creates a new object.
     *
     * @param control the XMLControl with the object data
     * @return the newly created object
     */
    public Object createObject(XMLControl control){
    	String name = control.getString("name"); //$NON-NLS-1$
      return new LibraryCatalog(name);
    }

    /**
     * Loads an object with data from an XMLControl.
     *
     * @param control the control
     * @param obj the object
     * @return the loaded object
     */
    public Object loadObject(XMLControl control, Object obj) {
    	XML.getLoader(LibraryRecord.class).loadObject(control, obj);
    	LibraryCatalog catalog = (LibraryCatalog)obj;
    	LibraryRecord[] records = (LibraryRecord[])control.getObject("records"); //$NON-NLS-1$
    	if (records!=null) {
    		for (LibraryRecord next: records) {
    			catalog.addRecord(next);
    		}
    	}
    	return catalog;
    }
  }
  
}

/*
 * Open Source Physics software is free software; you can redistribute
 * it and/or modify it under the terms of the GNU General Public License (GPL) as
 * published by the Free Software Foundation; either version 2 of the License,
 * or(at your option) any later version.

 * Code that uses any portion of the code in the org.opensourcephysics package
 * or any subpackage (subdirectory) of this package must must also be be released
 * under the GNU GPL license.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston MA 02111-1307 USA
 * or view the license online at http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2007  The Open Source Physics project
 *                     http://www.opensourcephysics.org
 */
