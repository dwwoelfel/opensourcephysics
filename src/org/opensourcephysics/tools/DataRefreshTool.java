/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.tools;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.opensourcephysics.controls.XMLControlElement;
import org.opensourcephysics.display.Data;
import org.opensourcephysics.display.Dataset;
import org.opensourcephysics.display.DatasetManager;

/**
 * This tool sends data to any tool that requests it.
 *
 * @author Douglas Brown
 * @version 1.0
 */
public class DataRefreshTool implements Tool {
  private static Map<Data, DataRefreshTool> tools = new HashMap<Data, DataRefreshTool>();
  private Data data; // data source 

  /**
   * Returns a DataRefreshTool for the specified data object.
   *
   * @param data the data
   * @return the tool
   */
  public static DataRefreshTool getTool(Data data) {
    DataRefreshTool tool = tools.get(data);
    if(tool==null) {
      tool = new DataRefreshTool(data);
      tools.put(data, tool);
    }
    return tool;
  }

  /**
   * Constructs a DataRefreshTool for the specified data object.
   *
   * @param data the data
   */
  private DataRefreshTool(Data data) {
    this.data = data;
  }

  /**
   * Sends a job to this tool and specifies a tool to reply to.
   * The job xml defines the Data object requesting a refresh ("requestData").
   * The requestData is compared with this tool's Data ("localData") as follows:
   * 1. If the requestData ID matches the localData ID, then the localData
   *    is sent back to the requester.
   * 2. If not, then the requestData ID is compared with the IDs of all Data objects
   *    in the list returned by DataTool.getDataList(localData). If a match is found,
   *    the matching Data is sent back to the requester.
   * 3. If not, then the requestData ID is compared with the IDs of Datasets
   *    in the list returned by DataTool.getDatasets(localData). If a match is found,
   *    the matching Dataset is sent back to the requester.
   * 4. If not, then every Dataset ID in the list returned by
   *    DataTool.getDatasets(requestData) is compared with the IDs of Datasets
   *    in the list returned by DataTool.getDatasets(localData).
   *    All matching Datasets that are found are sent back to the requester.
   *
   * @param job the Job
   * @param replyTo the tool requesting refreshed data
   * @throws RemoteException
   */
  public void send(Job job, Tool replyTo) throws RemoteException {
    XMLControlElement control = new XMLControlElement(job.getXML());
    if(control.failedToRead()||(replyTo==null)||!Data.class.isAssignableFrom(control.getObjectClass())) {
      return;
    }
    Data request = (Data) control.loadObject(null, true, true);
    // check for matching ID with localData
    if(request.getID()==data.getID()) {
      control = new XMLControlElement(data);
      job.setXML(control.toXML());
      replyTo.send(job, this);
      return;
    }
    // check for matching ID with DataTool.getDataList(localData)
    for(Data next : DataTool.getSelfContainedData(data)) {
      if(request.getID()==next.getID()) {
        control = new XMLControlElement(next);
        job.setXML(control.toXML());
        replyTo.send(job, this);
        return;
      }
    }
    // check for matching ID with local datasets
    ArrayList<Dataset> localDatasets = DataTool.getDatasets(data);
    for(Dataset next : localDatasets) {
      if(request.getID()==next.getID()) {
        control = new XMLControlElement(next);
        job.setXML(control.toXML());
        replyTo.send(job, this);
        return;
      }
    }
    // collect all request datasets that match a local dataset
    DatasetManager reply = new DatasetManager();
    // set name of reply to that of request
    reply.setName(request.getName());
    // find datasets that match requested ids
    for(Dataset next : DataTool.getDatasets(request)) {
      if(next==null) {
        continue;
      }
      Dataset match = getMatch(next.getID(), localDatasets);
      if((match!=null)&&!reply.getDatasets().contains(match)) {
        reply.addDataset(match);
      }
    }
    // send datasets to requesting tool
    if(!reply.getDatasets().isEmpty()) {
      control = new XMLControlElement(reply);
      job.setXML(control.toXML());
      replyTo.send(job, this);
    }
  }

  private Dataset getMatch(int id, ArrayList<Dataset> datasets) {
    for(Dataset next : datasets) {
      if(next==null) {
        continue;
      }
      if(id==next.getID()) {
        return next;
      }
    }
    return null;
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
