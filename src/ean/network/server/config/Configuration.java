package ean.network.server.config;

import java.io.*;

public class Configuration extends AbstractConfiguration {

	private String configFileName;
	private static Configuration instance = null;

	private Configuration() throws ConfigurationException {
		initialize();
	}

	public static Configuration getInstance()  {
		if (instance == null)
			synchronized (Configuration.class) {
					try {
						instance = new Configuration();
					} catch (ConfigurationException e) {
						e.printStackTrace();
					}
			}
		return instance;
	}

	private void initialize() throws ConfigurationException {
		try {
			File configFile = new File("config.properties");
			if (!configFile.canRead()) throw new ConfigurationException("Can't open configuration file: " + configFileName);

			props = new java.util.Properties();
			FileInputStream fin = new FileInputStream(configFile);
			props.load(new BufferedInputStream(fin));
			fin.close();
		} catch (Exception ex) {
			throw new ConfigurationException("Can't load configuration file: "+ configFileName);
		}
	}
}
