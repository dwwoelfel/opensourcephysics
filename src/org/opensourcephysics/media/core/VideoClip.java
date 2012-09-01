/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

/*
 * The org.opensourcephysics.media.core package defines the Open Source Physics
 * media framework for working with video and other media.
 *
 * Copyright (c) 2004  Douglas Brown and Wolfgang Christian.
 *
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
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
 * For additional information and documentation on Open Source Physics,
 * please see <http://www.opensourcephysics.org/>.
 */
package org.opensourcephysics.media.core;
import java.awt.Frame;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import javax.swing.JOptionPane;
import javax.swing.event.SwingPropertyChangeSupport;
import org.opensourcephysics.controls.OSPLog;
import org.opensourcephysics.controls.XML;
import org.opensourcephysics.controls.XMLControl;
import org.opensourcephysics.controls.XMLControlElement;
import org.opensourcephysics.tools.ResourceLoader;

/**
 * This defines a subset of video frames called steps.
 *
 * @author Douglas Brown
 * @version 1.0
 */
public class VideoClip {
  // instance fields
  private int startFrame = 0;
  private int stepSize = 1;
  private int stepCount = 10;   // default stepCount is 10 if video is null
  private int frameCount = stepCount; // default frameCount same as stepCount
  private int maxFrameCount = 1000;
  private double startTime = 0; // start time in milliseconds
  protected boolean isDefaultStartTime = true;
  protected Video video = null;
  private int[] stepFrames;
  ClipInspector inspector;
  private PropertyChangeSupport support;
  public boolean playAllSteps = true;
  private boolean isDefaultState;
  private boolean isAdjusting = false;
  private int endFrame;
  protected String readoutType;

  /**
   * Constructs a VideoClip.
   *
   * @param video the video
   */
  public VideoClip(Video video) {
    support = new SwingPropertyChangeSupport(this);
    this.video = video;
    if(video!=null) {
      video.setProperty("videoclip", this); //$NON-NLS-1$
      setStartFrameNumber(video.getStartFrameNumber());
      if(video.getFrameCount()>1) {
        setStepCount(video.getEndFrameNumber()-startFrame+1);
      }
    }
    updateArray();
    isDefaultState = true;
  }

  /**
   * Gets the video.
   *
   * @return the video
   */
  public Video getVideo() {
    return video;
  }

  /**
   * Sets the start frame number.
   *
   * @param start the desired start frame number
   */
  public boolean setStartFrameNumber(int start) {
  	return setStartFrameNumber(start, getFrameCount()-1);
  }

  /**
   * Sets the start frame number.
   *
   * @param start the desired start frame number
   */
  public boolean setStartFrameNumber(int start, int maxEndFrame) {
  	isDefaultState = false;
    start = Math.abs(start);
    int endFrame = getEndFrameNumber();
    start = Math.min(start, endFrame);
    if(startFrame==start) {
      return false;
    }
    if((video!=null)&&(video.getFrameCount()>1)) {
      video.setEndFrameNumber(video.getFrameCount()-1);
      video.setStartFrameNumber(start);
      startFrame = video.getStartFrameNumber();
      int max = Math.max(video.getFrameCount()-startFrame-1, 1);
      if(max<stepSize) {
        stepSize = max;
      }
    } else {
      startFrame = start;
      updateArray();
    }
    // reset end frame
    setEndFrameNumber(endFrame, maxEndFrame);
    support.firePropertyChange("startframe", null, new Integer(start)); //$NON-NLS-1$
    return true;
  }

  /**
   * Gets the start frame number.
   *
   * @return the start frame number
   */
  public int getStartFrameNumber() {
    return startFrame;
  }

  /**
   * Sets the step size.
   *
   * @param size the desired step size
   */
  public void setStepSize(int size) {
  	isDefaultState = false;
    if(size==0) {
      return;
    }
    size = Math.abs(size);
    if((video!=null)&&(video.getFrameCount()>1)) {
      int maxSize = Math.max(video.getFrameCount()-startFrame-1, 1);
      size = Math.min(size, maxSize);
    }
    if(stepSize!=size) {
      // get current end frame
      int endFrame = getEndFrameNumber();
      stepSize = size;
      // set stepCount to near value
      stepCount = 1+(endFrame-getStartFrameNumber())/stepSize;
      updateArray();
      support.firePropertyChange("stepsize", null, new Integer(size)); //$NON-NLS-1$
      // reset end frame
      setEndFrameNumber(endFrame);
    }
    trimFrameCount();
  }

  /**
   * Gets the step size.
   *
   * @return the step size
   */
  public int getStepSize() {
    return stepSize;
  }

  /**
   * Sets the step count.
   *
   * @param count the desired number of steps
   */
  public void setStepCount(int count) {
    if(count==0) {
      return;
    }
    count = Math.abs(count);
    if(video!=null) {
      if(video.getFrameCount()>1) {
        int end = video.getFrameCount()-1;
        int maxCount = 1+(int) ((end-startFrame)/(1.0*stepSize));
        count = Math.min(count, maxCount);
      }
      int end = startFrame+(count-1)*stepSize;
      if(end!=video.getEndFrameNumber()) {
        video.setEndFrameNumber(end);
      }
    }
    else {
    	count = Math.min(count, frameToStep(maxFrameCount-1)+1);
    }
    Integer prev = new Integer(stepCount);
    stepCount = Math.max(count, 1);
    updateArray();
    support.firePropertyChange("stepcount", prev, new Integer(stepCount)); //$NON-NLS-1$
  }

  /**
   * Gets the step count.
   *
   * @return the number of steps
   */
  public int getStepCount() {
    return stepCount;
  }

  /**
   * Gets the frame count.
   *
   * @return the number of frames
   */
  public int getFrameCount() {
    if(video!=null && video.getFrameCount()>1) {
    	return video.getFrameCount();
    }
    int frames = getEndFrameNumber()+1;
    frameCount = Math.max(frameCount, frames);
    frameCount = Math.min(frameCount, maxFrameCount);
    return frameCount;
  }

  /**
   * Sets the start time.
   *
   * @param t0 the start time in milliseconds
   */
  public void setStartTime(double t0) {
  	isDefaultState = false;
    if(startTime==t0) {
      return;
    }
    isDefaultStartTime = Double.isNaN(t0);
    startTime = Double.isNaN(t0) ? 0.0 : t0;
    support.firePropertyChange("starttime", null, new Double(startTime)); //$NON-NLS-1$
  }

  /**
 * Gets the start time.
 *
 * @return the start time in milliseconds
 */
  public double getStartTime() {
    return startTime;
  }

  /**
   * Gets the end frame number.
   *
   * @return the end frame
   */
  public int getEndFrameNumber() {
  	endFrame = startFrame+stepSize*(stepCount-1);
    return endFrame;
  }

  /**
   * Sets the end frame number.
   *
   * @param end the desired end frame
   * @return true if the end frame number was changed
   */
  public boolean setEndFrameNumber(int end) {
  	return setEndFrameNumber(end, 1000000);
  }

  /**
   * Sets the end frame number.
   *
   * @param end the desired end frame
   * @return true if the end frame number was changed
   */
  public boolean setEndFrameNumber(int end, int max) {
  	isDefaultState = false;
  	int prev = getEndFrameNumber();
  	if (prev==end)
  		return false;
    end = Math.max(end, startFrame);
    // determine step count needed for desired end frame
    int rem = (end-startFrame)%stepSize;
    int count = (end-startFrame)/stepSize;
    if(rem*1.0/stepSize>0.5) {
      count++;
    }
    else if(stepSize>1 && startFrame%2==0) {
      count++;
    }
    while (stepToFrame(count) > max) {
    	count--;
    }
    // set step count
    setStepCount(count+1);
    return prev!=end;
  }

  /**
   * Converts step number to frame number.
   *
   * @param stepNumber the step number
   * @return the frame number
   */
  public int stepToFrame(int stepNumber) {
    return startFrame+stepNumber*stepSize;
  }

  /**
   * Converts frame number to step number. A frame number that falls
   * between two steps maps to the previous step.
   *
   * @param n the frame number
   * @return the step number
   */
  public int frameToStep(int n) {
    return(int) ((n-startFrame)/(1.0*stepSize));
  }

  /**
   * Determines whether the specified frame is a step frame.
   *
   * @param n the frame number
   * @return <code>true</code> if the frame is a step frame
   */
  public boolean includesFrame(int n) {
    for(int i = 0; i<stepCount; i++) {
      if(stepFrames[i]==n) {
        return true;
      }
    }
    return false;
  }

  /**
   * Gets the clip inspector.
   *
   * @return the clip inspector
   */
  public ClipInspector getClipInspector() {
    return inspector;
  }

  /**
   * Gets the clip inspector with access to the specified ClipControl.
   *
   * @param control the clip control
   * @param frame the owner of the inspector
   * @return the clip inspector
   */
  public ClipInspector getClipInspector(ClipControl control, Frame frame) {
    if(inspector==null) {
      inspector = new ClipInspector(this, control, frame);
    }
    return inspector;
  }

  /**
   * Hides the clip inspector.
   */
  public void hideClipInspector() {
    if(inspector!=null) {
      inspector.setVisible(false);
    }
  }
  
  /**
   * Returns true if no properties have been set or reviewed by the user.
   * 
   * @return true if in a default state
   */
  public boolean isDefaultState() {
  	return isDefaultState && inspector==null;
  }

  /**
   * Sets the adjusting flag.
   *
   * @param adjusting true if adjusting
   */
  public void setAdjusting(boolean adjusting) {
  	if (isAdjusting==adjusting)
  		return;
  	isAdjusting = adjusting;
		support.firePropertyChange("adjusting", null, adjusting); //$NON-NLS-1$
  }

  /**
   * Gets the adjusting flag.
   *
   * @return true if adjusting
   */
  public boolean isAdjusting() {
  	return isAdjusting;
  }

  /**
   * Adds a PropertyChangeListener to this video clip.
   *
   * @param listener the object requesting property change notification
   */
  public void addPropertyChangeListener(PropertyChangeListener listener) {
    support.addPropertyChangeListener(listener);
  }

  /**
   * Adds a PropertyChangeListener to this video clip.
   *
   * @param property the name of the property of interest to the listener
   * @param listener the object requesting property change notification
   */
  public void addPropertyChangeListener(String property, PropertyChangeListener listener) {
    support.addPropertyChangeListener(property, listener);
  }

  /**
   * Removes a PropertyChangeListener from this video clip.
   *
   * @param listener the listener requesting removal
   */
  public void removePropertyChangeListener(PropertyChangeListener listener) {
    support.removePropertyChangeListener(listener);
  }

  /**
   * Removes a PropertyChangeListener for a specified property.
   *
   * @param property the name of the property
   * @param listener the listener to remove
   */
  public void removePropertyChangeListener(String property, PropertyChangeListener listener) {
    support.removePropertyChangeListener(property, listener);
  }

  /**
   * Trims unneeded frames after end frame (null videos only).
   */
  protected void trimFrameCount() {
    if(video==null || video.getFrameCount()==1) {
    	frameCount = getEndFrameNumber()+1;
      support.firePropertyChange("framecount", null, new Integer(frameCount)); //$NON-NLS-1$
    }
  }
  
  /**
   * Updates the list of step frames.
   */
  private void updateArray() {
    stepFrames = new int[stepCount];
    for(int i = 0; i<stepCount; i++) {
      stepFrames[i] = stepToFrame(i);
    }
  }

  /**
   * Returns an XML.ObjectLoader to save and load data for this class.
   *
   * @return the object loader
   */
  public static XML.ObjectLoader getLoader() {
    return new Loader();
  }

  /**
   * A class to save and load data for this class.
   */
  static class Loader implements XML.ObjectLoader {
    /**
     * Saves object data in an XMLControl.
     *
     * @param control the control to save to
     * @param obj the object to save
     */
    public void saveObject(XMLControl control, Object obj) {
      VideoClip clip = (VideoClip) obj;
      Video video = clip.getVideo();
      if(video!=null) {
        if(video instanceof ImageVideo) {
          ImageVideo vid = (ImageVideo) video;
          if(vid.isFileBased()) {
            control.setValue("video", video); //$NON-NLS-1$
          }
        } else {
          control.setValue("video", video);   //$NON-NLS-1$
        }
      }
      control.setValue("startframe", clip.getStartFrameNumber()); //$NON-NLS-1$
      control.setValue("stepsize", clip.getStepSize());           //$NON-NLS-1$
      control.setValue("stepcount", clip.getStepCount());         //$NON-NLS-1$
      control.setValue("starttime", clip.getStartTime());         //$NON-NLS-1$
      control.setValue("readout", clip.readoutType);         //$NON-NLS-1$
    }

    /**
     * Creates a new object.
     *
     * @param control the XMLControl with the object data
     * @return the newly created object
     */
    public Object createObject(XMLControl control) {
      // load the video and return a new clip
      boolean hasVideo = control.getPropertyNames().contains("video"); //$NON-NLS-1$
      if(!hasVideo) {
        return new VideoClip(null);
      }
      ResourceLoader.addSearchPath(control.getString("basepath")); //$NON-NLS-1$
      XMLControl child = control.getChildControl("video"); //$NON-NLS-1$
      // check for QTVideo and substitute Xuggle if available and preferred
      String className = child.getObjectClassName();
      String path = child.getString("path"); //$NON-NLS-1$
      if (className.endsWith("QTVideo") //$NON-NLS-1$
      		&& !VideoIO.isQTPreferred()
      		&& VideoIO.getVideoType("xuggle", null)!=null //$NON-NLS-1$
      		&& child instanceof XMLControlElement) {
      	Class<?> xuggleVideoClass;
				try {
					xuggleVideoClass = Class.forName("org.opensourcephysics.media.xuggle.XuggleVideo"); //$NON-NLS-1$
	      	((XMLControlElement)child).setObjectClass(xuggleVideoClass);
				} catch (ClassNotFoundException e) {}
      }
      // check for XuggleVideo and substitute QTVideo if available and preferred
      else if (className.endsWith("XuggleVideo") //$NON-NLS-1$
      		&& VideoIO.isQTPreferred()
      		&& child instanceof XMLControlElement) {
        File file = new File(XML.getName(path));
      	VideoFileFilter[] filters = VideoIO.getVideoType("qt", null).getFileFilters(); //$NON-NLS-1$
      	for (VideoFileFilter next: filters) {
      		if (next.accept(file)) {
          	Class<?> qtVideoClass;
    				try {
    					qtVideoClass = Class.forName("org.opensourcephysics.media.quicktime.QTVideo"); //$NON-NLS-1$
    	      	((XMLControlElement)child).setObjectClass(qtVideoClass);
    				} catch (ClassNotFoundException e) {}
      			break;
      		}
      	}
      }
      Video video = null;
      boolean loaded = true;
      try {
        video = (Video) control.getObject("video"); //$NON-NLS-1$
      } catch(Exception ex) {
      	ex.printStackTrace();
        loaded = false;
      } catch(Error er) {
        loaded = false;
      }
      // inform if video failed to load
      if(!loaded) {
        OSPLog.info("\""+path+"\" could not be opened");                                                  //$NON-NLS-1$ //$NON-NLS-2$
        JOptionPane.showMessageDialog(null, 
        		MediaRes.getString("VideoClip.Dialog.BadVideo.Message")+path, //$NON-NLS-1$
        		MediaRes.getString("VideoClip.Dialog.BadVideo.Title"),                                          //$NON-NLS-1$
            JOptionPane.WARNING_MESSAGE);
      } 
      else if((video==null)&&(path!=null)) {
        int i = JOptionPane.showConfirmDialog(null, "\""+path+"\" "                                       //$NON-NLS-1$ //$NON-NLS-2$
          +MediaRes.getString("VideoClip.Dialog.VideoNotFound.Message"),                                  //$NON-NLS-1$
            MediaRes.getString("VideoClip.Dialog.VideoNotFound.Title"),                                   //$NON-NLS-1$
              JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if(i==JOptionPane.YES_OPTION) {
        	VideoIO.getChooser().setAccessory(VideoIO.videoEnginePanel);
    	    VideoIO.videoEnginePanel.reset();
          VideoIO.getChooser().setSelectedFile(new File(path));
          java.io.File[] files = VideoIO.getChooserFiles("open video");                                         //$NON-NLS-1$
          if(files!=null && files.length>0) {
            VideoType selectedType = VideoIO.videoEnginePanel.getSelectedVideoType();
          	path = XML.getAbsolutePath(files[0]);
            video = VideoIO.getVideo(path, selectedType);
          }
        }
      }
      VideoClip clip = new VideoClip(video);
      return clip;
    }

    /**
     * Loads a VideoClip with data from an XMLControl.
     *
     * @param control the XMLControl
     * @param obj the object
     * @return the loaded object
     */
    public Object loadObject(XMLControl control, Object obj) {
      VideoClip clip = (VideoClip) obj;
      // set start frame
      int n = control.getInt("startframe"); //$NON-NLS-1$
      if(n!=Integer.MIN_VALUE) {
        clip.setStartFrameNumber(n);
      }
      // set step size
      n = control.getInt("stepsize"); //$NON-NLS-1$
      if(n!=Integer.MIN_VALUE) {
        clip.setStepSize(n);
      }
      // set step count
      n = control.getInt("stepcount"); //$NON-NLS-1$
      if(n!=Integer.MIN_VALUE) {
        clip.setStepCount(n);
      }
      // set start time
      double t = control.getDouble("starttime"); //$NON-NLS-1$
      if(!Double.isNaN(t)) {
        clip.startTime = t;
      }
	    clip.readoutType = control.getString("readout"); //$NON-NLS-1$
      return obj;
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
