/**
 * Copyright (c) 2012 Reficio (TM) - Reestablish your software! All Rights Reserved.
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.reficio.p2.utils;

import aQute.lib.osgi.FileResource;
import aQute.lib.osgi.Resource;
import aQute.lib.osgi.Analyzer;
import aQute.lib.osgi.Jar;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.maven.plugin.logging.Log;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import clover.retrotranslator.edu.emory.mathcs.backport.java.util.Arrays;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * @author Tom Bujok (tom.bujok@gmail.com)<br>
 *         Reficio (TM) - Reestablish your software!<br>
 *         http://www.reficio.org
 * @since 1.0.0
 */
public class JarUtils {

    public static void adjustSnapshotOutputVersion(File inputFile, File outputFile, String version) {
        Jar jar = null;
        try {
            jar = new Jar(inputFile);
            Manifest manifest = jar.getManifest();
            Attributes attributes = manifest.getMainAttributes();
            attributes.putValue(Analyzer.BUNDLE_VERSION, version);
            jar.write(outputFile);
        } catch (IOException e) {
            throw new RuntimeException("Cannot open jar " + outputFile);
        } catch (Exception e) {
            throw new RuntimeException("Cannot open jar " + outputFile);
        } finally {
            if (jar != null) {
                jar.close();
            }
        }
    }
    
    public static void adjustFeatureXml(File inputFile, File outputFile, File pluginDir, Log log, String timestamp) {
        Jar jar = null;
        File newXml = null;
        try {
        	jar = new Jar(inputFile);
	        Resource res = jar.getResource("feature.xml");
	        Document featureSpec = XmlUtils.parseXml(res.openInputStream());
	        
	        adjustFeatureQualifierVersionWithTimestamp(featureSpec, timestamp);
	        adjustFeaturePluginData(featureSpec, pluginDir, log);
            
	        File temp = new File(outputFile.getParentFile(),"temp");
	        temp.mkdir();
	        newXml = new File(temp,"feature.xml");
	        XmlUtils.writeXml(featureSpec, newXml);
            FileResource newRes = new FileResource(newXml);
            jar.putResource("feature.xml", newRes, true);
            jar.write(outputFile);
        } catch (IOException e) {
            throw new RuntimeException("Cannot open jar " + outputFile);
        } catch (Exception e) {
            throw new RuntimeException("Cannot open jar " + outputFile);
        } finally {
            if (jar != null) {
                jar.close();
            }
            if (null!=newXml) {
            	newXml.delete();
            }
        }
    }
    
    public static void adjustFeatureQualifierVersionWithTimestamp(Document featureSpec, String timestamp) {
	        String version = featureSpec.getDocumentElement().getAttributeNode("version").getValue();
	        String newVersion = Utils.eclipseQualifierToTimeStamp(version, timestamp); 
	        featureSpec.getDocumentElement().getAttributeNode("version").setValue(newVersion);
    }

    static Comparator<File> fileComparator = new Comparator<File>() {
		@Override
		public int compare(File arg0, File arg1) {
			return arg0.getName().compareTo(arg1.getName());
		}
	};
    
    public static void adjustFeaturePluginData(Document featureSpec, File pluginDir, Log log) throws IOException {
	        //get list of all plugins
	        NodeList plugins = featureSpec.getElementsByTagName("plugin");
	        for(int i=0; i<plugins.getLength(); ++i) {
	        	Node n = plugins.item(i);
	        	if (n instanceof Element) {
		        	Element el = (Element)n;
		        	String pluginId = el.getAttribute("id");
		        	File[] files = findFiles(pluginDir, pluginId);
		        	if (files.length < 0) {
		        		log.error("Cannot find plugin "+pluginId);
		        	} else {
		        		//in case more than one plugin with same id
		        		Arrays.sort(files,fileComparator);
		        		//File firstFile = files[0];
		        		File lastFile = files[files.length-1];
		        		//String firstVersion = BundleUtils.INSTANCE.getBundleVersion(new Jar(firstFile));
		        		String lastVersion = BundleUtils.INSTANCE.getBundleVersion(new Jar(lastFile)); //may throw IOException
		        		log.info("Adjusting version for plugin "+pluginId+" to "+lastVersion);
		        		el.setAttribute("version", lastVersion);
		        	}
	        	}
	        }
    }
    
    static File[] findFiles(File pluginDir, final String pluginId) {
    	 return pluginDir.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.startsWith(pluginId) && name.endsWith(".jar");
				}
			});
    }
    
    public static void removeSignature(File jar) {
        File unsignedJar = new File(jar.getParent(), jar.getName() + ".tmp");
        try {
            if (unsignedJar.exists()) {
                FileUtils.deleteQuietly(unsignedJar);
                unsignedJar = new File(jar.getParent(), jar.getName() + ".tmp");
            }
            if (!unsignedJar.createNewFile()) {
                throw new RuntimeException("Cannot create file " + unsignedJar);
            }

            ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(unsignedJar));
            try {
                ZipFile zip = new ZipFile(jar);
                for (Enumeration list = zip.entries(); list.hasMoreElements(); ) {
                    ZipEntry entry = (ZipEntry) list.nextElement();
                    String name = entry.getName();
                    if (entry.isDirectory()) {
                        continue;
                    } else if (name.endsWith(".RSA") || name.endsWith(".DSA") || name.endsWith(".SF")) {
                        continue;
                    }

                    InputStream zipInputStream = zip.getInputStream(entry);
                    zipOutputStream.putNextEntry(entry);
                    try {
                        IOUtils.copy(zipInputStream, zipOutputStream);
                    } finally {
                        zipInputStream.close();
                    }
                }
                IOUtils.closeQuietly(zipOutputStream);
                FileUtils.copyFile(unsignedJar, jar);
            } finally {
                IOUtils.closeQuietly(zipOutputStream);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            FileUtils.deleteQuietly(unsignedJar);
        }
    }

    public static boolean containsSignature(File jarToUnsign) {
        try {
            ZipFile zip = new ZipFile(jarToUnsign);
            try {
                for (Enumeration list = zip.entries(); list.hasMoreElements(); ) {
                    ZipEntry entry = (ZipEntry) list.nextElement();
                    String name = entry.getName();
                    if (!entry.isDirectory() && (name.endsWith(".RSA") || name.endsWith(".DSA") || name.endsWith(".SF"))) {
                        return true;
                    }
                }
                return false;
            } finally {
                zip.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
