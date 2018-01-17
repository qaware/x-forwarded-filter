package de.qaware.web.util;

import org.junit.Test;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 *
 */
public class ForwardedHeaderTest {

	private String [] supportedHeaders = {"Forwarded","X-Forwarded-Host","X-Forwarded-Port","X-Forwarded-Proto","X-Forwarded-Prefix"};

	@Test
	public void fromName(){
		assertEquals(ForwardedHeader.FORWARDED,ForwardedHeader.forName("Forwarded"));
		assertEquals(ForwardedHeader.X_FORWARDED_HOST,ForwardedHeader.forName("X-Forwarded-Host"));
		assertEquals(ForwardedHeader.X_FORWARDED_PROTO,ForwardedHeader.forName("X-Forwarded-Proto"));
		assertEquals(ForwardedHeader.X_FORWARDED_PORT,ForwardedHeader.forName("X-Forwarded-Port"));
		assertEquals(ForwardedHeader.X_FORWARDED_PREFIX,ForwardedHeader.forName("X-Forwarded-Prefix"));
	}

	@Test
	public void forNameCaseInsensitive(){
		for(String header: supportedHeaders){
			ForwardedHeader fromCamelCase=ForwardedHeader.forName(header);
			ForwardedHeader fromLowerCase=ForwardedHeader.forName(header.toLowerCase());
			ForwardedHeader fromUpperCase=ForwardedHeader.forName(header.toUpperCase());
			assertNotNull(fromCamelCase);
			assertNotNull(fromLowerCase);
			assertNotNull(fromUpperCase);
			assertEquals(fromCamelCase,fromLowerCase);
			assertEquals(fromLowerCase,fromUpperCase);
		}
	}

	@Test
	public void isForwaredHeader(){
		for(String header: supportedHeaders){
			boolean fromCamelCase=ForwardedHeader.isForwardedHeader(header);
			boolean fromLowerCase=ForwardedHeader.isForwardedHeader(header.toLowerCase());
			boolean fromUpperCase=ForwardedHeader.isForwardedHeader(header.toUpperCase());
			assertTrue(fromCamelCase);
			assertTrue(fromLowerCase);
			assertTrue(fromUpperCase);
		}
	}

	@Test
	public void testToString(){
		for(String header: supportedHeaders){
			ForwardedHeader fromCamelCase=ForwardedHeader.forName(header);
			assertEquals(header,fromCamelCase.toString());
		}
	}

	@Test
	public void headerName(){
		for(String header: supportedHeaders){
			ForwardedHeader fromCamelCase=ForwardedHeader.forName(header);
			assertEquals(header,fromCamelCase.headerName());
		}
	}

}
