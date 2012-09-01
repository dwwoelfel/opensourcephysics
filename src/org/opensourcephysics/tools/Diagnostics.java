/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.tools;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import javax.swing.JOptionPane;
import org.opensourcephysics.controls.XML;
import org.opensourcephysics.display.OSPRuntime;

/**
 * This defines static methods for reporting the availability and status of
 * various software resources.
 *
 * @author Doug Brown
 */
public class Diagnostics {
  final static String NEWLINE = System.getProperty("line.separator", "\n"); //$NON-NLS-1$ //$NON-NLS-2$

  public static void aboutJava() {
    String version = System.getProperty("java.version", "unknown version");   //$NON-NLS-1$ //$NON-NLS-2$
    String rtName = System.getProperty("java.runtime.name");                  //$NON-NLS-1$
    String rtVersion = System.getProperty("java.runtime.version");            //$NON-NLS-1$
    String vmName = System.getProperty("java.vm.name");                       //$NON-NLS-1$
    String vmVersion = System.getProperty("java.vm.version");                 //$NON-NLS-1$
    String aboutString = ToolsRes.getString("Diagnostics.Java.About.Version") //$NON-NLS-1$
                         +" "+version+NEWLINE                                 //$NON-NLS-1$
                         +rtName+" (build "+rtVersion+")"+NEWLINE             //$NON-NLS-1$ //$NON-NLS-2$
                         +vmName+" (build "+vmVersion+")";                    //$NON-NLS-1$ //$NON-NLS-2$
    JOptionPane.showMessageDialog(null, aboutString, ToolsRes.getString("Diagnostics.Java.About.Title"), //$NON-NLS-1$
      JOptionPane.INFORMATION_MESSAGE);
  }

  public static void aboutQTJava() {
    // look for QTJava.zip in java extensions folder
    String extdir = System.getProperty("java.ext.dirs"); //$NON-NLS-1$
    // look in first directory listed (before path separator, if any)
    String separator = System.getProperty("path.separator"); //$NON-NLS-1$
    if(extdir.indexOf(separator)>-1) { 
      extdir = extdir.substring(0, extdir.indexOf(separator));
    }
    String slash = System.getProperty("file.separator", "/"); //$NON-NLS-1$ //$NON-NLS-2$
    File extfile = new File(extdir+slash+"QTJava.zip");       //$NON-NLS-1$
    if(extfile.exists()) {
      boolean failed = false;
      try {
        Class<?> type = Class.forName("quicktime.util.QTBuild");                                               //$NON-NLS-1$
        Method method = type.getMethod("info", (Class[]) null);                                                //$NON-NLS-1$
        String version = (String) method.invoke(null, (Object[]) null);
        version = version.substring(version.indexOf(":")+1,                                                    //$NON-NLS-1$
                                    version.indexOf("]"));                                                     //$NON-NLS-1$
        String aboutString = ToolsRes.getString("Diagnostics.QTJava.About.Version")                            //$NON-NLS-1$
                             +" "+version;                                                                     //$NON-NLS-1$
        JOptionPane.showMessageDialog(null, aboutString, ToolsRes.getString("Diagnostics.QTJava.About.Title"), //$NON-NLS-1$
          JOptionPane.INFORMATION_MESSAGE);
      } catch(Exception ex) {
        failed = true;
      } catch(Error err) {
        failed = true;
      }
      if(failed) {
        JOptionPane.showMessageDialog(null, ToolsRes.getString("Diagnostics.QTJava.Error.Message"),                      //$NON-NLS-1$
          ToolsRes.getString("Diagnostics.QTJava.About.Title"),                                                          //$NON-NLS-1$
            JOptionPane.WARNING_MESSAGE);
      }
    } else {
      JOptionPane.showMessageDialog(null, ToolsRes.getString("Diagnostics.QTJava.NotFound.Message1")+" "+extdir+NEWLINE+ //$NON-NLS-1$ //$NON-NLS-2$
        ToolsRes.getString("Diagnostics.QTJava.NotFound.Message2"), //$NON-NLS-1$
          ToolsRes.getString("Diagnostics.QTJava.About.Title"),     //$NON-NLS-1$
            JOptionPane.WARNING_MESSAGE);
    }
  }

  public static void aboutJava3D() {
    // look for j3dcore.jar in java extensions folder
    String extdir = System.getProperty("java.ext.dirs"); //$NON-NLS-1$
    // look in first directory listed (before path separator, if any)
    String separator = System.getProperty("path.separator"); //$NON-NLS-1$
    if(extdir.indexOf(separator)>-1) { 
      extdir = extdir.substring(0, extdir.indexOf(separator));
    }
    String slash = System.getProperty("file.separator", "/"); //$NON-NLS-1$ //$NON-NLS-2$
    File extfile = new File(extdir+slash+"j3dcore.jar");      //$NON-NLS-1$
    if(extfile.exists()) {
      boolean failed = false;
      try {
        Class<?> type = Class.forName("javax.media.j3d.VirtualUniverse");                                      //$NON-NLS-1$
        Method method = type.getMethod("getProperties", (Class[]) null);                                       //$NON-NLS-1$
        Map<?, ?> props = (Map<?, ?>) method.invoke(null, (Object[]) null);
        String version = (String) props.get("j3d.version");                                                    //$NON-NLS-1$
        String vendor = (String) props.get("j3d.vendor");                                                      //$NON-NLS-1$
        String aboutString = ToolsRes.getString("Diagnostics.Java3D.About.Version")                            //$NON-NLS-1$
                             +" "+version+NEWLINE+vendor;                                                      //$NON-NLS-1$
        JOptionPane.showMessageDialog(null, aboutString, ToolsRes.getString("Diagnostics.Java3D.About.Title"), //$NON-NLS-1$
          JOptionPane.INFORMATION_MESSAGE);
      } catch(Exception ex) {
        failed = true;
      } catch(Error err) {
        failed = true;
      }
      if(failed) {
        JOptionPane.showMessageDialog(null, ToolsRes.getString("Diagnostics.Java3D.Error.Message"),                      //$NON-NLS-1$
          ToolsRes.getString("Diagnostics.Java3D.About.Title"),                                                          //$NON-NLS-1$
            JOptionPane.WARNING_MESSAGE);
      }
    } else {
      JOptionPane.showMessageDialog(null, ToolsRes.getString("Diagnostics.Java3D.NotFound.Message1")+" "+extdir+NEWLINE+ //$NON-NLS-1$ //$NON-NLS-2$
        ToolsRes.getString("Diagnostics.Java3D.NotFound.Message2"), //$NON-NLS-1$
          ToolsRes.getString("Diagnostics.Java3D.About.Title"),     //$NON-NLS-1$
            JOptionPane.WARNING_MESSAGE);
    }
  }

  public static void aboutJOGL() {
    // look for jogl.jar in java extensions folder
    String extdir = System.getProperty("java.ext.dirs"); //$NON-NLS-1$
    // look in first directory listed (before path separator, if any)
    String separator = System.getProperty("path.separator"); //$NON-NLS-1$
    if(extdir.indexOf(separator)>-1) { 
      extdir = extdir.substring(0, extdir.indexOf(separator));
    }
    String slash = System.getProperty("file.separator", "/"); //$NON-NLS-1$ //$NON-NLS-2$
    File extfile = new File(extdir+slash+"jogl.jar");         //$NON-NLS-1$
    if(extfile.exists()) {
      boolean failed = false;
      try {
        Class<?> type = Class.forName("javax.media.opengl.glu.GLU");                                         //$NON-NLS-1$
        Field field = type.getField("versionString");                                                        //$NON-NLS-1$
        String version = (String) field.get(null);
        String aboutString = ToolsRes.getString("Diagnostics.JOGL.About.Version")                            //$NON-NLS-1$
                             +" "+version;                                                                   //$NON-NLS-1$
        JOptionPane.showMessageDialog(null, aboutString, ToolsRes.getString("Diagnostics.JOGL.About.Title"), //$NON-NLS-1$
          JOptionPane.INFORMATION_MESSAGE);
      } catch(Exception ex) {
        failed = true;
      } catch(Error err) {
        failed = true;
      }
      if(failed) {
        JOptionPane.showMessageDialog(null, ToolsRes.getString("Diagnostics.JOGL.Error.Message"),                      //$NON-NLS-1$
          ToolsRes.getString("Diagnostics.JOGL.About.Title"),                                                          //$NON-NLS-1$
            JOptionPane.WARNING_MESSAGE);
      }
    } else {
      JOptionPane.showMessageDialog(null, ToolsRes.getString("Diagnostics.JOGL.NotFound.Message1")+" "+extdir+NEWLINE+ //$NON-NLS-1$ //$NON-NLS-2$
        ToolsRes.getString("Diagnostics.JOGL.NotFound.Message2"), //$NON-NLS-1$
          ToolsRes.getString("Diagnostics.JOGL.About.Title"),     //$NON-NLS-1$
            JOptionPane.WARNING_MESSAGE);
    }
  }

  public static void aboutLaunchJar() {
    if(OSPRuntime.getLaunchJarPath()!=null) {
      // create a JarFile
      JarFile jar = OSPRuntime.getLaunchJar();
      try {
        if(jar!=null) {
          String aboutString = ToolsRes.getString("Diagnostics.Jar.About.Message.JarFile")                        //$NON-NLS-1$
                               +" \""+XML.getName(OSPRuntime.getLaunchJarPath())+"\". ";                          //$NON-NLS-1$ //$NON-NLS-2$
          // iterate thru JarFile entries and look for dsa file
          for(Enumeration<JarEntry> e = jar.entries(); e.hasMoreElements(); ) {
            JarEntry entry = e.nextElement();
            String name = entry.getName().toLowerCase();
            if(name.endsWith(".dsa")&&name.startsWith("meta-inf")) {                                              //$NON-NLS-1$ //$NON-NLS-2$
              aboutString += ToolsRes.getString("Diagnostics.Jar.About.Message.Signed");                          //$NON-NLS-1$
              JOptionPane.showMessageDialog(null, aboutString, ToolsRes.getString("Diagnostics.Jar.About.Title"), //$NON-NLS-1$
                JOptionPane.INFORMATION_MESSAGE);
              return;
            }
          }
          aboutString += ToolsRes.getString("Diagnostics.Jar.About.Message.NotSigned");                       //$NON-NLS-1$
          JOptionPane.showMessageDialog(null, aboutString, ToolsRes.getString("Diagnostics.Jar.About.Title"), //$NON-NLS-1$
            JOptionPane.INFORMATION_MESSAGE);
        }
      } catch(Exception ex) {
        ex.printStackTrace();
      }
    } else {
      JOptionPane.showMessageDialog(null, ToolsRes.getString("Diagnostics.Jar.About.Message.NoJarFile"), //$NON-NLS-1$
        ToolsRes.getString("Diagnostics.Jar.About.Title"),                                               //$NON-NLS-1$
          JOptionPane.INFORMATION_MESSAGE);
    }
  }

  public static void aboutOS() {
    String osName = System.getProperty("os.name");                       //$NON-NLS-1$
    String version = System.getProperty("os.version");                   //$NON-NLS-1$
    String aboutString = ToolsRes.getString("Diagnostics.OS.About.Name") //$NON-NLS-1$
                         +" "+osName+NEWLINE;                            //$NON-NLS-1$
    aboutString += ToolsRes.getString("Diagnostics.OS.About.Version") //$NON-NLS-1$
                   +" "+version+NEWLINE;                              //$NON-NLS-1$
    Enumeration<?> e = System.getProperties().propertyNames();
    while(e.hasMoreElements()) {
      String next = (String) e.nextElement();
      if(next.startsWith("os.")) {               //$NON-NLS-1$
        String val = System.getProperty(next);
        if(!val.equals(osName)&&!val.equals(version)) {
          aboutString += next+":  "+val+NEWLINE; //$NON-NLS-1$
        }
      }
    }
    JOptionPane.showMessageDialog(null, aboutString, ToolsRes.getString("Diagnostics.OS.About.Title"), //$NON-NLS-1$
      JOptionPane.INFORMATION_MESSAGE);
  }

}

/*
 * Open Source Physics software is free software; you can redistribute
 * it and/or modify it under the terms of the GNU General Public License (GPL) as
 * published by the Free Software Foundation; either version 2 of the License,
 * or(at your option) any later version.
 *
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
