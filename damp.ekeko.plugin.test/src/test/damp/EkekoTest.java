package test.damp;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import damp.ekeko.EkekoPlugin;


public class EkekoTest {
	
	
	private static Bundle myBundle;
	
	static {
		myBundle = FrameworkUtil.getBundle(EkekoTest.class);
	}
	
	@BeforeClass
	public static void ensureTestCasesExist() throws Exception {
		EkekoTestHelper.ensureProjectImported(myBundle, "/resources/TestCases/", "Ekeko-JDT");
	}

	@Test
	public void testPluginID() {
		assertEquals(EkekoPlugin.PLUGIN_ID, "damp.ekeko.plugin");		
	}
	
	@Test 
	public void testEkekoSuite() {
		EkekoTestHelper.testClojureNamespace(myBundle, "test.damp.ekeko");
	}
	

	@Test 
	public void testEkekoJDTSuite() {
		EkekoTestHelper.testClojureNamespace(myBundle,"test.damp.ekeko.jdt");
	}
		
	
	@Test 
	public void testEkekoJDTPersistence() {
		EkekoTestHelper.testClojureNamespace(myBundle,"test.damp.ekeko.persistence");
	}
		

	
	


}
