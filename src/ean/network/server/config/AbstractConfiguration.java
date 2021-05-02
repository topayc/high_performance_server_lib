package ean.network.server.config;

import java.util.Properties;

public abstract class AbstractConfiguration implements Config {
   /**
    * 모든 속성값을 저장한다..
    */
   protected Properties props = null;
   
   public AbstractConfiguration() {}
   
   /**
    * 모든 속성 이름을 구한다.
    */
   public Properties getProperties() {
      return props;
   }
   
   /**
    * String 타입 속성값을 읽어온다.
    */
   public String getString(String key) {
      String value = null;
      value = props.getProperty(key);
      
      if (value == null) throw new IllegalArgumentException("Illegal String key : "+key);
      
      return value;
   }
   
   /**
    * int 타입 속성값을 읽어온다.
    */
   public int getInt(String key) {
      int value = 0;
      try {
         value = Integer.parseInt( props.getProperty(key) );
      } catch(Exception ex) {
         throw new IllegalArgumentException("Illegal int key : "+key);
      }
      return value;
   }
   
   /**
    * double 타입 속성값을 읽어온다.
    */
   public double getDouble(String key) {
      double value = 0.0;
      try {
         value = Double.valueOf( props.getProperty(key) ).doubleValue();
      } catch(Exception ex) {
         throw new IllegalArgumentException("Illegal double key : "+key);
      }
      return value;
   }
   
   /**
    * boolean 타입 속성값을 읽어온다.
    */
   public boolean getBoolean(String key) {
      boolean value = false;
      try {
         value = Boolean.valueOf(props.getProperty(key)).booleanValue();
      } catch(Exception ex) {
         throw new IllegalArgumentException("Illegal boolean key : "+key);
      }
      return value;
   }
}

