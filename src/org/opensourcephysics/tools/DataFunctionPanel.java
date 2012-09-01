/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.tools;
import java.beans.PropertyChangeEvent;
import java.util.Iterator;
import org.opensourcephysics.display.DataFunction;
import org.opensourcephysics.display.DatasetManager;

/**
 * This is a FunctionPanel for DataFunctions.
 *
 * @author Douglas Brown
 */
public class DataFunctionPanel extends FunctionPanel {
  /**
   * Constructor with input data.
   *
   * @param input the input DatasetManager
   */
  public DataFunctionPanel(DatasetManager input) {
    this(new DataFunctionEditor(input));
  }

  /**
   * Constructor with function editor.
   *
   * @param editor a DataFunctionEditor
   */
  public DataFunctionPanel(DataFunctionEditor editor) {
    super(editor);
    String name = editor.getData().getName();
    setName(name.equals("") ? "data" : name); //$NON-NLS-1$ //$NON-NLS-2$
  }

  /**
   * Returns the DatasetManager.
   *
   * @return the DatasetManager
   */
  public DatasetManager getData() {
    return((DataFunctionEditor) functionEditor).getData();
  }

  /**
   * Gets a label for the FunctionTool spinner.
   *
   * @return a label string
   */
  public String getLabel() {
    return ToolsRes.getString("DataFunctionPanel.SpinnerLabel"); //$NON-NLS-1$
  }

  /**
   * Listens for property changes "edit" and "function"
   *
   * @param e the event
   */
  public void propertyChange(PropertyChangeEvent e) {
    if(e.getPropertyName().equals("edit")) {                                         //$NON-NLS-1$
      super.propertyChange(e);
    } else if(e.getPropertyName().equals("function")) {                              //$NON-NLS-1$
      // function has been added or removed
      if(e.getNewValue()!=null) {                                                    // added
        DataFunction f = (DataFunction) e.getNewValue();
        getData().addDataset(f);
      } else if(e.getOldValue()!=null) {                                             // removed
        DataFunction f = (DataFunction) e.getOldValue();
        int i = getData().getDatasetIndex(f.getYColumnName());
        getData().removeDataset(i);
      }
      refreshFunctions();
      refreshGUI();
      functionTool.firePropertyChange("function", e.getOldValue(), e.getNewValue()); //$NON-NLS-1$
    }
  }

  /**
   * Refreshes the functions.
   */
  protected void refreshFunctions() {
    // set the constant values in the data
    for (String name: getData().getConstantNames()) {
    	getData().clearConstant(name);
    }
    Iterator<Object> it = paramEditor.getObjects().iterator();
    while(it.hasNext()) {
      Parameter p = (Parameter) it.next();
      String name = p.getName();
      double val = p.getValue();
      getData().setConstant(name, val, p.getExpression());
    }
    // evaluate the functions 
    functionEditor.evaluateAll();
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
