package org.mule.custom.devkit.utils;

import org.apache.commons.beanutils.BeanUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

public class TestBeanUtils {
	
	public TestBeanUtils() {
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) {
		
		LoggerProcessor loggerJson = new LoggerProcessor();
		loggerJson.setMessage("blah");
		loggerJson.setCorrelationId("123");
		
		try {
			System.out.println(BeanUtils.describe(loggerJson));
			BeanUtils.describe(loggerJson).forEach((k,v) -> System.out.println("key: "+k+" value:"+v));
			DateTime dt = new DateTime();
			System.out.println(dt.withZone(DateTimeZone.forID("US/Eastern")).toString("yyyy-MM-dd HH:mm:ss.SSS"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
