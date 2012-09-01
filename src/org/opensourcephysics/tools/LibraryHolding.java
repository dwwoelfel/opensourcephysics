/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.tools;
import java.util.ArrayList;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.opensourcephysics.controls.XML;
import org.opensourcephysics.controls.XMLControl;
import org.opensourcephysics.media.core.VideoIO;

/**
 * This represents a digital library record with content.
 *
 * @author Douglas Brown
 * @version 1.0
 */
public class LibraryHolding extends LibraryRecord {
	
  protected static Icon htmlIcon, videoIcon, trackerIcon;
  static {
    String imageFile = "/org/opensourcephysics/resources/tools/images/html.gif";        //$NON-NLS-1$
    htmlIcon = new ImageIcon(LibraryRecord.class.getResource(imageFile));
    imageFile = "/org/opensourcephysics/resources/tools/images/video.gif";        //$NON-NLS-1$
    videoIcon = new ImageIcon(LibraryRecord.class.getResource(imageFile));
    imageFile = "/org/opensourcephysics/resources/tools/images/trackericon.gif"; //$NON-NLS-1$
    trackerIcon = new ImageIcon(LibraryRecord.class.getResource(imageFile));
  }
	
	protected ArrayList<String> filePaths = new ArrayList<String>(); // file paths
	
	public LibraryHolding(String name) {
		super(name);
	}
	
	@Override
  public void setName(String theName) {
		name = theName==null? ToolsRes.getString("LibraryHolding.Name.Default"): theName; //$NON-NLS-1$
	}
	
	public void addContent(String filePath) {
		if (!filePaths.contains(filePath))
			filePaths.add(filePath);
	}
	
	public boolean removeContent(String filePath) {
		return filePaths.remove(filePath);
	}
	
	public String[] getContents() {
		return filePaths.toArray(new String[filePaths.size()]);
	}
  	
	public Icon getIcon() {
		for (String next: getContents()) {
			if (next.endsWith(".trk")) { //$NON-NLS-1$
				return trackerIcon;
			}
		}
		String[] extensions = VideoIO.getVideoExtensions();
		for (String next: getContents()) {
			for (String ext: extensions) {
				if (next.endsWith("."+ext)) { //$NON-NLS-1$
					return videoIcon;
				}
			}
		}
		for (String next: getContents()) {
			if (next.endsWith(".html")) { //$NON-NLS-1$
				return htmlIcon;
			}
		}
		if (getInfoPath()!=null) {
			return htmlIcon;
		}
		return super.getIcon();
	}

	public String getDescription() {
		String desc = super.getDescription();
		if (desc!=null)
			return desc;
		Icon icon = getIcon();
		return icon==trackerIcon? ToolsRes.getString("LibraryHolding.TRK.Description"): //$NON-NLS-1$
			icon==videoIcon? ToolsRes.getString("LibraryHolding.Video.Description"): //$NON-NLS-1$
				icon==htmlIcon? ToolsRes.getString("LibraryHolding.HTML.Description"): //$NON-NLS-1$
					ToolsRes.getString("LibraryHolding.Unknown.Description"); //$NON-NLS-1$
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
    	XML.getLoader(LibraryRecord.class).saveObject(control, obj);
    	LibraryHolding holding = (LibraryHolding)obj;
    	if (!holding.filePaths.isEmpty())
    		control.setValue("contents", holding.getContents()); //$NON-NLS-1$
    }
    
    /**
     * Creates a new object.
     *
     * @param control the XMLControl with the object data
     * @return the newly created object
     */
    public Object createObject(XMLControl control){
    	String name = control.getString("name"); //$NON-NLS-1$
      return new LibraryHolding(name);
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
    	LibraryHolding holding = (LibraryHolding)obj;
    	String[] paths = (String[])(control.getObject("contents")); //$NON-NLS-1$    	
    	if (paths!=null) {
    		holding.filePaths.clear();
    		for (String next: paths) {
    			holding.addContent(next);
    		}
    	}
    	return holding;
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
