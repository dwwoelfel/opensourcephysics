/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.tools;

import javax.swing.Icon;

import org.opensourcephysics.controls.XML;
import org.opensourcephysics.controls.XMLControl;

/**
 * This represents a digital library record.
 *
 * @author Douglas Brown
 * @version 1.0
 */
public class LibraryRecord {
	
	protected String name;
	protected String infoPath; // name or path to html that describes this record
	protected String basePath; // base path for contents and/or info
	protected String description;
	
	public LibraryRecord(String name) {
		setName(name);
	}
	
	public void setName(String aName) {
		name = aName==null? null: aName.trim();
	}
	
	public String getName() {
		return name;
	}
	
	public void setBasePath(String path) {
		basePath = path==null || path.trim().equals("")? null: path.trim(); //$NON-NLS-1$
	}
	
	public String getBasePath() {
		return basePath;
	}
	
	public void setInfoPath(String path) {
		infoPath = path==null? null: path.trim();
	}
	
	public String getInfoPath() {
		return infoPath;
	}
	
	public Icon getIcon() {
		return null;
	}
  	
	public String getDescription() {
		return description;
	}
  	
	public void setDescription(String desc) {
		description = desc;
	}
  	
  public static String getURI(String path) {
		// trim and change backslashes to forward slashes
		path = XML.forwardSlash(path.trim());
		// add forward slash at end if needed
		if (!path.equals("")  //$NON-NLS-1$
				&& XML.getExtension(path)==null
				&& !path.endsWith("/")) //$NON-NLS-1$
			path += "/"; //$NON-NLS-1$
    // replace spaces with "%20"
    int i = path.indexOf(" ");                       //$NON-NLS-1$
    while(i>-1) {
      String s = path.substring(0, i);
      path = s+"%20"+path.substring(i+1);            //$NON-NLS-1$
      i = path.indexOf(" ");                         //$NON-NLS-1$
    }
    // add file URI protocol if not a web path
		if (!path.equals("") && !path.startsWith("http:") && !path.startsWith("file://")) {  			 //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			path = "file:///"+path; //$NON-NLS-1$
		}
  	return path;
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
   * The ObjectLoader class to save and load LibraryRecord data.
   */
  static class Loader implements XML.ObjectLoader {

    /**
     * Saves an object's data to an XMLControl.
     *
     * @param control the control to save to
     * @param obj the object to save
     */
    public void saveObject(XMLControl control, Object obj) {
    	LibraryRecord record = (LibraryRecord)obj;
    	control.setValue("name", record.name); //$NON-NLS-1$
    	control.setValue("description", record.description); //$NON-NLS-1$
    	control.setValue("base_path", record.basePath); //$NON-NLS-1$
    	control.setValue("info_path", record.infoPath); //$NON-NLS-1$
    }
    
    /**
     * Creates a new object.
     *
     * @param control the XMLControl with the object data
     * @return the newly created object
     */
    public Object createObject(XMLControl control){
    	// subclasses should load name in this method
      return null;
    }

    /**
     * Loads an object with data from an XMLControl.
     *
     * @param control the control
     * @param obj the object
     * @return the loaded object
     */
    public Object loadObject(XMLControl control, Object obj) {
    	LibraryRecord record = (LibraryRecord)obj;
    	// subclasses should load name in createObject() method
    	record.description = control.getString("description"); //$NON-NLS-1$
    	record.basePath = control.getString("base_path"); //$NON-NLS-1$
    	record.infoPath = control.getString("info_path"); //$NON-NLS-1$
    	return record;
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
