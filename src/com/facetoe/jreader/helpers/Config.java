/*   
*    Copyright (C) 2013  facetoe - facetoe@ymail.com
*
*    This program is free software; you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation; either version 2 of the License, or
*    (at your option) any later version.
*
*    This program is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License along
*    with this program; if not, write to the Free Software Foundation, Inc.,
*    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package com.facetoe.jreader.helpers;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;


/**
 * This class manages the storing and retrieval of config data.
 */
public class Config {
    private static final Logger log = Logger.getLogger(Config.class);

    // Where our data directory is located.
    public static final String DATA_DIR = "dataDir";

    // Where our profile direcories are located.
    public static final String PROFILE_DIR = "profileDir";

    // The current profiles name.
    public static final String CURRENT_PROFILE = "currentProfile";

    // Whether or not we have setup a default profile
    public static final String HAS_DEFAULT_PROFILE = "hasDefaultProfile";

    // The profile directorys name.
    public static final String PROFILE_DIR_NAME = "profiles";

    // The default profiles name.
    public static final String DEFAULT_PROFILE_NAME = "Default";

    // The name of the Java source zip file.
    public static final String JAVA_LANG_ZIP_FILE_NAME = "src-jdk";
    public static final String JAVA_DOCS_ZIP_FILE_NAME = "jdk-7u51-apidocs";

    // The name of the profile class data files.
    public static final String CLASS_DATA_FILE_NAME = "classData.ser";

    // The target file for JavaDocParser.
    public static final String ALL_CLASSSES_DOC_FILE = "allclasses-noframe.html";

    // The name of our data directory.
    public static final String DATA_DIR_NAME = ".jreader";

    private static final String CONFIG_FILE_NAME = "config.properties";
    private static final Properties properties = new Properties();

    /**
     * The full path to our data directory.
     */
    public static final String dataDirectory = System.getProperty("user.home") +
            File.separator +
            DATA_DIR_NAME +
            File.separator;

    /**
     * Full path to the config file.
     */
    public static final String configFilePath = dataDirectory +
            CONFIG_FILE_NAME +
            File.separator;

    /**
     * Set a config String value.
     */
    public static void setString(String key, String value) {
        setEntry(key, value);
    }

    /**
     * Set a config boolean value.
     */
    public static void setBool(String key, boolean value) {
        setEntry(key, String.valueOf(value));
    }

    /**
     * Get a congig boolean value.
     */
    public static boolean getBool(String key) {
        return Boolean.parseBoolean(getEntry(key));
    }

    /**
     * Get a config String value.
     */
    public static String getString(String key) {
        return getEntry(key);
    }

    private static void setEntry(String key, String value) {
        FileInputStream in = null;
        try {
            in = new FileInputStream(configFilePath);
            properties.load(in);
            properties.setProperty(key, value);
            properties.store(new FileOutputStream(configFilePath), null);
        } catch (IOException ex) {
            log.error(ex);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {}
        }
    }


    private static String getEntry(String key) {
        FileInputStream in = null;
        try {
            in = new FileInputStream(configFilePath);
            properties.load(in);
        } catch (IOException ex) {
            log.error(ex);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return properties.getProperty(key);
    }
}