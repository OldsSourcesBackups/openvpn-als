package com.adito.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.util.LabelValueBean;

import com.adito.boot.ContextHolder;
import com.adito.input.MultiSelectDataSource;
import com.adito.security.SessionInfo;

public class SSLCipherSuitesDataSource implements MultiSelectDataSource {

	final static Log log = LogFactory.getLog(SSLCipherSuitesDataSource.class);
	private static ArrayList list;
	public Collection<LabelValueBean> getValues(SessionInfo sessionInfo) {
		
		try {
		
			if(list!=null)
				return list;
			
			File f = new File(ContextHolder.getContext().getTempDirectory(), "availableCipherSuites.txt");
			BufferedReader reader = null;
			
			try {
				reader = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
				
				list = new ArrayList();
				String cipher;
				while((cipher = reader.readLine())!=null) {
					list.add(new LabelValueBean(cipher, cipher));
				}
				return list;
			} finally {
				if(reader!=null)
					reader.close();
			}
		} catch(Exception ex) {
			log.error(ex);
			return null;
		}
	}

}
