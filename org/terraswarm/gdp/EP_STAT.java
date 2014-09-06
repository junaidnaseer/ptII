package org.terraswarm.gdp;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import java.util.Arrays;
import java.util.List;
/**
 * <i>native declaration : src/gdp/ep/ep_stat.h:53</i><br>
 * This file was autogenerated by <a href="http://jnaerator.googlecode.com/">JNAerator</a>,<br>
 * a tool written by <a href="http://ochafik.com/">Olivier Chafik</a> that <a href="http://code.google.com/p/jnaerator/wiki/CreditsAndLicense">uses a few opensource projects.</a>.<br>
 * For help, please visit <a href="http://nativelibs4java.googlecode.com/">NativeLibs4Java</a> , <a href="http://rococoa.dev.java.net/">Rococoa</a>, or <a href="http://jna.dev.java.net/">JNA</a>.
 */
public class EP_STAT extends Structure {
	public NativeLong code;
	public EP_STAT() {
		super();
	}
	protected List<? > getFieldOrder() {
		return Arrays.asList("code");
	}
	public EP_STAT(NativeLong code) {
		super();
		this.code = code;
	}
	public EP_STAT(Pointer peer) {
		super(peer);
	}
	public static class ByReference extends EP_STAT implements Structure.ByReference {
		
	};
	public static class ByValue extends EP_STAT implements Structure.ByValue {
		
	};
}
