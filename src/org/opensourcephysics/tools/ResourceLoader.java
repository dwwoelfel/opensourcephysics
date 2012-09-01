/*
 * Open Source Physics software is free software as described near the bottom of
 * this code file.
 *
 * For additional information and documentation on Open Source Physics please
 * see: <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.tools;
import java.applet.AudioClip;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import javax.swing.ImageIcon;
import org.opensourcephysics.controls.OSPLog;
import org.opensourcephysics.controls.XML;
import org.opensourcephysics.display.OSPRuntime;

/**
 * This defines static methods for loading resources.
 *
 * @author Douglas Brown
 * @version 1.0
 */
public class ResourceLoader {
  protected static ArrayList<String> searchPaths = new ArrayList<String>();                        // search paths
  protected static ArrayList<String> appletSearchPaths = new ArrayList<String>();                  // search paths for apples
  protected static int maxPaths = 20;                                                              // max number of paths in history
  protected static Hashtable<String, Resource> resources = new Hashtable<String, Resource>();      // cached resources
  protected static boolean cacheEnabled = false;
  protected static Map<String, URLClassLoader> zipLoaders = new TreeMap<String, URLClassLoader>(); // maps zip to zipLoader
  protected static URLClassLoader xsetZipLoader; // zipLoader of current xset
  protected static ArrayList<String> extractExtensions = new ArrayList<String>();

  /**
   * Private constructor to prevent instantiation.
   */
  private ResourceLoader() {
    /** empty block */
  }

  /**
   * Gets a resource specified by name. If no resource is found using the name
   * alone, the searchPaths are searched.
   *
   * @param name the file or URL name
   * @return the Resource, or null if none found
   */
  public static Resource getResource(String name) {
    return getResource(name, true);
  }

  /**
   * Gets a resource specified by name. If no resource is found using
   * the name alone, the searchPaths are searched.
   * Files are searched only if searchFile is true.
   *
   * @param name the file or URL name
   * @param searchFiles true to search files
   * @return the Resource, or null if none found
   */
  public static Resource getResource(String name, boolean searchFiles) {
    try {
      URL url = getAppletResourceURL(name); // added by W. Christian
      if(url!=null) {
        return new Resource(url);
      }
    } catch(Exception ex) {}
    return getResource(name, Resource.class, searchFiles);
  }

  /**
   * Gets a resource specified by name and Class. If no resource is found using
   * the name alone, the searchPaths are searched.
   *
   * @param name the file or URL name
   * @param type the Class providing default ClassLoader resource loading
   * @return the Resource, or null if none found
   */
  public static Resource getResource(String name, Class<?> type) {
    return getResource(name, type, true);
  }

  /**
   * Gets a resource specified by name and Class. If no resource is found using
   * the name alone, the searchPaths are searched.
   * Files are searched only if searchFile is true.
   *
   * @param name the file or URL name
   * @param type the Class providing default ClassLoader resource loading
   * @param searchFiles true to search files
   * @return the Resource, or null if none found
   */
  public static Resource getResource(String name, Class<?> type, boolean searchFiles) {
    if((name==null)||name.equals("")) { //$NON-NLS-1$
      return null;
    }
    // Remove leading and trailing inverted commas (added by Paco)
    if(name.startsWith("\"")) { //$NON-NLS-1$   
      name = name.substring(1);
    }
    if(name.endsWith("\"")) { //$NON-NLS-1$   
      name = name.substring(0, name.length()-1);
    }
    if(name.startsWith("./")) { //$NON-NLS-1$   
      name = name.substring(2);
    }
    if(OSPRuntime.isAppletMode()||(OSPRuntime.applet!=null)) { // added by Paco
      Resource appletRes = null;
      // following code added by Doug Brown 2009/11/14
      if(type==OSPRuntime.applet.getClass()) {
        try {
          URL url = type.getResource(name);
          appletRes = createResource(url);
          if(appletRes!=null) {
            return appletRes;
          }
        } catch(Exception ex) {}
      }                                                        // end code added by Doug Brown 2009/11/14
      for(Iterator<String> it = searchPaths.iterator(); it.hasNext(); ) {
        String path = getPath(it.next(), name);
        appletRes = findResourceInClass(path, type, searchFiles);
        if(appletRes!=null) {
          return appletRes;
        }
      }
      appletRes = findResourceInClass(name, type, searchFiles);
      if(appletRes!=null) {
        return appletRes;
      }
    }
    // look for resource with name only
    Resource res = findResource(name, type, searchFiles);
    if(res!=null) {
      return res;
    }
    StringBuffer err = new StringBuffer("Not found: "+name); //$NON-NLS-1$
    err.append(" [searched "+name); //$NON-NLS-1$
    // look for resource in searchPaths
    for(String next: searchPaths) {
      String path = getPath(next, name);
      res = findResource(path, type, searchFiles);
      if(res!=null) {
        return res;
      }
      err.append(";"+path); //$NON-NLS-1$
    }
    err.append("]"); //$NON-NLS-1$
    OSPLog.fine(err.toString());
    return null;
  }

  /**
   * Gets a resource specified by base path and name. If base path is relative
   * and no resource is found using the base alone, the searchPaths are
   * searched.
   *
   * @param basePath the base path
   * @param name the file or URL name
   * @return the Resource, or null if none found
   */
  public static Resource getResource(String basePath, String name) {
    return getResource(basePath, name, Resource.class);
  }

  /**
   * Gets a resource specified by base path and name. If base path is relative
   * and no resource is found using the base alone, the searchPaths are
   * searched. Files are searched only if searchFile is true.
   *
   * @param basePath the base path
   * @param name the file or URL name
   * @param searchFiles true to search files
   * @return the Resource, or null if none found
   */
  public static Resource getResource(String basePath, String name, boolean searchFiles) {
    return getResource(basePath, name, Resource.class, searchFiles);
  }

  /**
   * Gets a resource specified by base path, name and class. If base path is
   * relative and no resource is found using the base alone, the searchPaths
   * are searched.
   *
   * @param basePath the base path
   * @param name the file or URL name
   * @param type the Class providing ClassLoader resource loading
   * @return the Resource, or null if none found
   */
  public static Resource getResource(String basePath, String name, Class<Resource> type) {
    return getResource(basePath, name, type, true);
  }

  /**
 * Gets a resource specified by base path, name and class. If base path is
 * relative and no resource is found using the base alone, the searchPaths
 * are searched. Files are searched only if searchFile is true.
 *
 * @param basePath the base path
 * @param name the file or URL name
 * @param type the Class providing ClassLoader resource loading
 * @param searchFiles true to search files
 * @return the Resource, or null if none found
 */
  public static Resource getResource(String basePath, String name, Class<Resource> type, boolean searchFiles) {
    if(basePath==null) {
      return getResource(name, type);
    }
    if(name.startsWith("./")) { //$NON-NLS-1$   
      name = name.substring(2);
    }
    // look for resource with basePath and name
    String path = getPath(basePath, name);
    Resource res = findResource(path, type, searchFiles);
    if(res!=null) {
      return res;
    }
    // keep looking only if base path is relative
    if(basePath.startsWith("/")||(basePath.indexOf(":/")>-1)) { //$NON-NLS-1$ //$NON-NLS-2$
      return null;
    }
    StringBuffer err = new StringBuffer("Not found: "+path); //$NON-NLS-1$
    err.append(" [searched "+path); //$NON-NLS-1$
    if(OSPRuntime.applet!=null) {                  // applet mode
      String docBase = OSPRuntime.applet.getDocumentBase().toExternalForm();
      docBase = XML.getDirectoryPath(docBase)+"/"; //$NON-NLS-1$
      path = getPath(getPath(docBase, basePath), name);
      res = findResource(path, type, searchFiles);
      if(res!=null) {
        return res;
      }
      err.append(";"+path);                        //$NON-NLS-1$
      String codeBase = OSPRuntime.applet.getCodeBase().toExternalForm();
      if(!codeBase.equals(docBase)) {
        path = getPath(getPath(codeBase, basePath), name);
        res = findResource(path, type, searchFiles);
        if(res!=null) {
          return res;
        }
        err.append(";"+path);                      //$NON-NLS-1$
      }
    }
    // look for resource in searchPaths
    for(Iterator<String> it = searchPaths.iterator(); it.hasNext(); ) {
      path = getPath(getPath(it.next(), basePath), name);
      res = findResource(path, type, searchFiles);
      if(res!=null) {
        return res;
      }
      err.append(";"+path); //$NON-NLS-1$
    }
    err.append("]"); //$NON-NLS-1$
    OSPLog.fine(err.toString());
    return null;
  }

  /**
   * Adds a path at the beginning of the searchPaths list.
   *
   * @param base the base path to add
   */
  public static void addSearchPath(String base) {
    if((base==null)||base.equals("")||(maxPaths<1)) { //$NON-NLS-1$
      return;
    }
    synchronized(searchPaths) {
      if(searchPaths.contains(base)) {
        searchPaths.remove(base);
      } else {
        OSPLog.fine("Added path: "+base);   //$NON-NLS-1$
      }
      searchPaths.add(0, base);
      while(searchPaths.size()>Math.max(maxPaths, 0)) {
        base = searchPaths.get(searchPaths.size()-1);
        OSPLog.fine("Removed path: "+base); //$NON-NLS-1$
        searchPaths.remove(base);
      }
    }
  }

  /**
   * Removes a path from the searchPaths list.
   *
   * @param base the base path to remove
   */
  public static void removeSearchPath(String base) {
    if((base==null)||base.equals("")) { //$NON-NLS-1$
      return;
    }
    synchronized(searchPaths) {
      if(searchPaths.contains(base)) {
        OSPLog.fine("Removed path: "+base); //$NON-NLS-1$
        searchPaths.remove(base);
      }
    }
  }

  /**
   * Adds a search path at the beginning of the applet's search path list.
   * Added by Wolfgang Christian.
   *
   * @param base the base path to add
   */
  public static void addAppletSearchPath(String base) {
    if((base==null)||(maxPaths<1)) {
      return;
    }
	base=base.trim();
	if(!base.endsWith("/"))base=base+"/";  //$NON-NLS-1$//$NON-NLS-2$
    synchronized(appletSearchPaths) {
      if(appletSearchPaths.contains(base)) {
        appletSearchPaths.remove(base);                 // search path will be added to top of list later
      } else {
        OSPLog.fine("Applet search path added: "+base); //$NON-NLS-1$
      }
      appletSearchPaths.add(0, base);
      while(appletSearchPaths.size()>Math.max(maxPaths, 0)) {
        base = appletSearchPaths.get(appletSearchPaths.size()-1);
        OSPLog.fine("Removed path: "+base);             //$NON-NLS-1$
        appletSearchPaths.remove(base);
      }
    }
  }

  /**
   * Removes a path from the applet search path list.
   * Added by Wolfgang Christian.
   *
   * @param base the base path to remove
   */
  public static void removeAppletSearchPath(String base) {
    if((base==null)||base.equals("")) { //$NON-NLS-1$
      return;
    }
    synchronized(appletSearchPaths) {
      if(appletSearchPaths.contains(base)) {
        OSPLog.fine("Applet search path removed: "+base); //$NON-NLS-1$
        appletSearchPaths.remove(base);
      }
    }
  }

  /**
   * Sets the cacheEnabled property.
   *
   * @param enabled true to enable the cache
   */
  public static void setCacheEnabled(boolean enabled) {
    cacheEnabled = enabled;
  }

  /**
   * Gets the cacheEnabled property.
   *
   * @return true if the cache is enabled
   */
  public static boolean isCacheEnabled() {
    return cacheEnabled;
  }

  /**
   * Adds an extension to the end of the extractExtensions list.
   * Files with this extension found inside jars are extracted before loading.
   *
   * @param extension the extension to add
   */
  public static void addExtractExtension(String extension) {
    if((extension==null)||extension.equals("")) { //$NON-NLS-1$
      return;
    }
    if(!extension.startsWith(".")) { //$NON-NLS-1$
      extension = "."+extension;     //$NON-NLS-1$
    }
    OSPLog.finest("Added extension: "+extension); //$NON-NLS-1$
    synchronized(extractExtensions) {
      extractExtensions.add(extension);
    }
  }

  // ___________________________ convenience methods _________________________
  public static InputStream openInputStream(String path) {
    Resource res = getResource(path);
    return(res==null) ? null : res.openInputStream();
  }

  public static Reader openReader(String path) {
    Resource res = getResource(path);
    return(res==null) ? null : res.openReader();
  }

  public static String getString(String path) {
    Resource res = getResource(path);
    return(res==null) ? null : res.getString();
  }

  public static ImageIcon getIcon(String path) {
    URL url = getAppletResourceURL(path); // added by W. Christian
    if(url!=null) {
      return new ImageIcon(url);
    }
    Resource res = getResource(path);
    return(res==null) ? null : res.getIcon();
  }

  public static Image getImage(String path) {
    URL url = getAppletResourceURL(path); // added by W. Christian
    if(url!=null) {
      return new ImageIcon(url).getImage();
    }
    Resource res = getResource(path);
    return(res==null) ? null : res.getImage();
  }

  public static BufferedImage getBufferedImage(String path) {
    Resource res = getResource(path);
    return(res==null) ? null : res.getBufferedImage();
  }

  public static AudioClip getAudioClip(String path) {
    Resource res = getResource(path);
    return(res==null) ? null : res.getAudioClip();
  }

  // ______________________________ private methods ___________________________

  /**
   * Gets the resource URL using the applet's class loader.
   * Added by Wolfgang Christian.
   *
   * @param name of the resource
   * @return URL of the Resource, or null if none found
   */
  private static URL getAppletResourceURL(String name) {
    if((OSPRuntime.applet==null)||(name==null)||name.trim().equals("")) { //$NON-NLS-1$
      return null;
    }
    if(name.startsWith("http:")||name.startsWith("https:")){ //$NON-NLS-1$  //$NON-NLS-2$ // open a direct connection for http and https resources
    	try {
			return new java.net.URL(name);
		} catch (MalformedURLException e) {
			//e.printStackTrace();
		} 
    }
    name=name.trim();  // remove whitespace
    if(!name.startsWith("/")) { //$NON-NLS-1$ // try applet search paths for relative paths
      for(Iterator<String> it = appletSearchPaths.iterator(); it.hasNext(); ) {
        String path = it.next();
        String tempName=name;                   // tempName may change
    	if(tempName.startsWith("../")) {        //$NON-NLS-1$   
    		  tempName = tempName.substring(3);     //remove prefix
              path=path.substring(0, path.length()-1); // drop trailing slash
              int last=path.lastIndexOf("/"); //$NON-NLS-1$ // find last directory slash
              path=(last>0)?path.substring(0, last):"/";   //$NON-NLS-1$ // drop last directory if it exists 
        }else if(tempName.startsWith("./")) {         //$NON-NLS-1$   
        	tempName = tempName.substring(2);     //remove reference to current directory
        } 
        URL url = OSPRuntime.applet.getClass().getResource(path+tempName);
        if(url!=null) {
          return url;
        }
      }
    }
    return OSPRuntime.applet.getClass().getResource(name); // url not found in applet search paths
  }

  /**
   * Creates a Resource from a file.
   *
   * @param path the file path
   * @return the resource, if any
   */
  static private Resource createFileResource(String path) {
    // don't create file resources when in applet mode
    if(OSPRuntime.applet!=null) {
      return null;
    }
    // ignore paths that refer to zip or jar files
    if((path.indexOf(".zip")>-1)||(path.indexOf(".jar")>-1)) { //$NON-NLS-1$ //$NON-NLS-2$
      return null;
    }
    File file = new File(path);
    try {
      if(file.exists()&&file.canRead()) {
        Resource res = new Resource(file);
        if(path.endsWith("xset")) {                                    //$NON-NLS-1$
          xsetZipLoader = null;
        }
        OSPLog.fine("File: "+XML.forwardSlash(res.getAbsolutePath())); //$NON-NLS-1$
        return res;
      }
    } catch(AccessControlException ex) {
      /** empty block */
    }
    return null;
  }

  /**
   * Creates a Resource from a URL.
   *
   * @param path the url path
   * @return the resource, if any
   */
  static private Resource createURLResource(String path) {
    // ignore paths that refer to zip or jar files
    if((path.indexOf(".zip")>-1)||(path.indexOf(".jar")>-1)) { //$NON-NLS-1$ //$NON-NLS-2$
      return null;
    }
    Resource res = null;
    // following added by Doug Brown 2009/11/14
    if(OSPRuntime.applet!=null) {
      try { // let applet class try to get it first
        //URL url = OSPRuntime.applet.getClass().getResource(path);
        URL url = getAppletResourceURL(path);
        res = createResource(url);
      } catch(Exception ex) {
        /** empty block */
      }
    }       // end code added by Doug Brown 2009/11/14
    if(res==null) {
      // if path includes protocol, use it directly
      if(path.indexOf(":/")>-1) {                                          //$NON-NLS-1$
        try {
          URL url = new URL(path);
          res = createResource(url);
        } catch(Exception ex) {
          /** empty block */
        }
      }
      // else if applet mode and relative path, search document and code base
      else {
        if((OSPRuntime.applet!=null)&&!path.startsWith("/")) {             //$NON-NLS-1$
          // first check document base
          URL docBase = OSPRuntime.applet.getDocumentBase();
          try {
            // following added by Doug Brown 2009/11/14
            String basePath = docBase.toString();
            // strip query, if any, from document base
            int n = basePath.indexOf("?");                                 //$NON-NLS-1$
            if(n>-1) {
              docBase = new URL(basePath.substring(0, n));
            }
            // end code added by Doug Brown 2009/11/14
            URL url = new URL(docBase, path);
            res = createResource(url);
          } catch(Exception ex) {
            /** empty block */
          }
          if(res==null) {
            URL codeBase = OSPRuntime.applet.getCodeBase();
            String s = XML.getDirectoryPath(docBase.toExternalForm())+"/"; //$NON-NLS-1$
            if(!codeBase.toExternalForm().equals(s)) {
              try {
                URL url = new URL(codeBase, path);
                res = createResource(url);
              } catch(Exception ex) {
                /** empty block */
              }
            }
          }
        }
      }
    }
    if(res!=null) {
      if(path.endsWith(".xset")) {                                  //$NON-NLS-1$
        xsetZipLoader = null;
      }
      OSPLog.fine("URL: "+XML.forwardSlash(res.getAbsolutePath())); //$NON-NLS-1$
    }
    return res;
  }

  /**
   * Creates a Resource from within a zip or jar file.
   *
   * @param path the file path
   * @return the resource, if any
   */
  static private Resource createZipResource(String path) {
    // get separate zip base and relative file name
    String base = null;
    String fileName = path;
    // look for zip or jar base path
    int i = path.indexOf("zip!/"); //$NON-NLS-1$
    if(i==-1) {
      i = path.indexOf("jar!/"); //$NON-NLS-1$
    }
    if(i==-1) {
      i = path.indexOf("exe!/"); //$NON-NLS-1$
    }
    if(i>-1) {
      base = path.substring(0, i+3);
      fileName = path.substring(i+5);
    }
    if(base==null) {
      if(path.endsWith(".zip")                           //$NON-NLS-1$
        ||path.endsWith(".jar")                          //$NON-NLS-1$
        ||path.endsWith(".exe")) {                       //$NON-NLS-1$
        String name = XML.stripExtension(XML.getName(path));
        base = path;
        fileName = name+".xset";                         //$NON-NLS-1$
      } else if(path.endsWith(".xset")) {                //$NON-NLS-1$
        base = path.substring(0, path.length()-4)+"zip"; //$NON-NLS-1$
      }
    }
    URLClassLoader zipLoader = null;
    URL url = null;
    if(base!=null) {
      // use existing zip loader, if any
      zipLoader = zipLoaders.get(base);
      if(zipLoader!=null) {
        url = zipLoader.findResource(fileName);
      } else {
        try {
          // create new zip loader
          URL[] urls = new URL[] {new URL("file", null, base)};  //$NON-NLS-1$
          zipLoader = new URLClassLoader(urls);
          url = zipLoader.findResource(fileName);
          if(url==null) {                                        // workaround works in IE?
            URL classURL = Resource.class.getResource("/"+base); //$NON-NLS-1$
            if(classURL!=null) {
              urls = new URL[] {classURL};
              zipLoader = new URLClassLoader(urls);
              url = zipLoader.findResource(fileName);
            }
          }
          if(url!=null) {
            zipLoaders.put(base, zipLoader);
          }
        } catch(Exception ex) {
          /** empty block */
        }
      }
    }
    // if not found, use xset zip loader, if any
    if((url==null)&&(xsetZipLoader!=null)) {
      url = xsetZipLoader.findResource(fileName);
      if(url!=null) {
        Iterator<String> it = zipLoaders.keySet().iterator();
        while(it.hasNext()) {
          Object key = it.next();
          if(zipLoaders.get(key)==xsetZipLoader) {
            base = (String) key;
            break;
          }
        }
      }
    }
    String launchJarPath = OSPRuntime.getLaunchJarPath();
    // if still not found, use launch jar loader, if any
    if((url==null)&&(launchJarPath!=null)) {
      zipLoader = zipLoaders.get(launchJarPath);
      if(zipLoader!=null) {
        url = zipLoader.findResource(fileName);
      } else {
        try {
          // create new zip loader
          URL[] urls = new URL[] {new URL("file", null, launchJarPath)};  //$NON-NLS-1$
          zipLoader = new URLClassLoader(urls);
          url = zipLoader.findResource(fileName);
          if(url==null) {                                                 // workaround works in IE?
            URL classURL = Resource.class.getResource("/"+launchJarPath); //$NON-NLS-1$
            if(classURL!=null) {
              urls = new URL[] {classURL};
              zipLoader = new URLClassLoader(urls);
              url = zipLoader.findResource(fileName);
            }
          }
          if(url!=null) {
            zipLoaders.put(launchJarPath, zipLoader);
          }
        } catch(Exception ex) {
          /** empty block */
        }
      }
      if(url!=null) {
        base = launchJarPath;
      }
    }
    if(url!=null) {                                                   // successfully found url
      // extract file if extension is flagged for extraction
      Iterator<String> it = extractExtensions.iterator();
      while(it.hasNext()) {
        String ext = it.next();
        if(url.getFile().endsWith(ext)) {
          File zipFile = new File(base);
          File target = new File(fileName);
          if(!target.exists()) {
            target = JarTool.extract(zipFile, fileName, fileName);
          }
          return createFileResource(target.getAbsolutePath());
        }
      }
      try {
        Resource res = createResource(url);
        if((res==null)||(res.getAbsolutePath().indexOf(path)==-1)) {
          return null;
        }
        if(fileName.endsWith("xset")) {                               //$NON-NLS-1$
          xsetZipLoader = zipLoader;
        }
        OSPLog.fine("Zip: "+XML.forwardSlash(res.getAbsolutePath())); //$NON-NLS-1$
        return res;
      } catch(IOException ex) {
        /** empty block */
      }
    }
    return null;
  }

  /**
   * Creates a Resource from a class resource, typically in a jar file.
   *
   * @param name the resource name
   * @param type the class providing the classloader
   * @return the resource, if any
   */
  static private Resource createClassResource(String name, Class<?> type) {
    // ignore any name that has a protocol
    if(name.indexOf(":/")!=-1) { //$NON-NLS-1$
      return null;
    }
    String fullName = name;
    int i = name.indexOf("jar!/"); //$NON-NLS-1$
    if(i==-1) {
      i = name.indexOf("exe!/"); //$NON-NLS-1$
    }
    if(i!=-1) {
      name = name.substring(i+5);
    }
    Resource res = null;
    try {                                   // check relative to root of jarfile containing specified class
      URL url = type.getResource("/"+name); //$NON-NLS-1$
      res = createResource(url);
    } catch(Exception ex) {
      /** empty block */
    }
    if(res==null) {
      try { // check relative to specified class
        URL url = type.getResource(name);
        res = createResource(url);
      } catch(Exception ex) {
        /** empty block */
      }
    }
    // if resource is found, log and set launchJarName if not yet set
    if(res!=null) {
      String path = XML.forwardSlash(res.getAbsolutePath());
      // don't return resources from Java runtime system jars
      if((path.indexOf("/jre")>-1)&&(path.indexOf("/lib")>-1)) { //$NON-NLS-1$ //$NON-NLS-2$
        return null;
      }
      // don't return resources that don't contain original name
      if(path.indexOf(fullName)==-1) {
        return null;
      }
      if(name.endsWith("xset")) {                                //$NON-NLS-1$
        xsetZipLoader = null;
      }
      OSPLog.fine("Class resource: "+path);                      //$NON-NLS-1$
      OSPRuntime.setLaunchJarPath(path);
    }
    return res; // may be null
  }

  /**
   * Creates a Resource.
   *
   * @param url the URL
   * @return the resource, if any
   * @throws IOException
   */
  static private Resource createResource(URL url) throws IOException {
    if(url==null) {
      return null;
    }
    // check that url is accessible
    InputStream stream = url.openStream();
    if(stream.read()==-1) {
      return null;
    }
    stream.close();
    return new Resource(url);
  }

  /**
   * Finds the resource using only the class resource loader
   */
  private static Resource findResourceInClass(String path, Class<?> type, boolean searchFiles) { // added by Paco
    path = path.replaceAll("/\\./", "/"); // This eliminates any embedded /./ //$NON-NLS-1$ //$NON-NLS-2$
    if(type==null) {
      type = Resource.class;
    }
    Resource res = null;
    // look for cached resource
    if(cacheEnabled) {
      res = resources.get(path);
      if((res!=null)&&(searchFiles||(res.getFile()==null))) {
        OSPLog.finest("Found in cache: "+path); //$NON-NLS-1$
        return res;
      }
    }
    if((res = createClassResource(path, type))!=null) {
      if(cacheEnabled) {
        resources.put(path, res);
      }
      return res;
    }
    return null;
  }

  private static Resource findResource(String path, Class<?> type, boolean searchFiles) {
    path = path.replaceAll("/\\./", "/"); // This eliminates any embedded /./ //$NON-NLS-1$ //$NON-NLS-2$
    if(type==null) {
      type = Resource.class;
    }
    Resource res = null;
    // look for cached resource
    if(cacheEnabled) {
      res = resources.get(path);
      if((res!=null)&&(searchFiles||(res.getFile()==null))) {
        OSPLog.finest("Found in cache: "+path); //$NON-NLS-1$
        return res;
      }
    }
    // try to load resource in file/url/zip/class order
    // search files only if flagged
    if((searchFiles&&(res = createFileResource(path))!=null)||(res = createURLResource(path))!=null||(res = createZipResource(path))!=null||(res = createClassResource(path, type))!=null) {
      if(cacheEnabled) {
        resources.put(path, res);
      }
      return res;
    }
    return null;
  }

  /**
   * Gets a path from a base path and file name.
   *
   * @param base the base path
   * @param name the file name
   * @return the path
   */
  private static String getPath(String base, String name) {
    if(base==null) {
      base = ""; //$NON-NLS-1$
    }
    if(base.endsWith(".jar")||base.endsWith(".zip")) { //$NON-NLS-1$ //$NON-NLS-2$
      base += "!";                                     //$NON-NLS-1$
    }
    String path = XML.getResolvedPath(name, base);
    // correct the path so that it works with Mac
    if(OSPRuntime.isMac()&&path.startsWith("file:/")&&!path.startsWith("file:///")) { //$NON-NLS-1$ //$NON-NLS-2$
      path = path.substring(6);
      while(path.startsWith("/")) {                                                   //$NON-NLS-1$
        path = path.substring(1);
      }
      path = "file:///"+path;                                                         //$NON-NLS-1$
    }
    return path;
  }

}

/*
 * Open Source Physics software is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License (GPL) as
 * published by the Free Software Foundation; either version 2 of the License,
 * or(at your option) any later version.
 *
 * Code that uses any portion of the code in the org.opensourcephysics package
 * or any subpackage (subdirectory) of this package must must also be be
 * released under the GNU GPL license.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this; if not, write to the Free Software Foundation, Inc., 59 Temple Place,
 * Suite 330, Boston MA 02111-1307 USA or view the license online at
 * http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2007 The Open Source Physics project
 * http://www.opensourcephysics.org
 */
