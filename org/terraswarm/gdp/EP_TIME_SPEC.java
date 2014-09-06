package org.terraswarm.gdp;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import java.util.Arrays;
import java.util.List;
/**
 * <i>native declaration : src/gdp/ep/ep_time.h:7</i><br>
 * This file was autogenerated by <a href="http://jnaerator.googlecode.com/">JNAerator</a>,<br>
 * a tool written by <a href="http://ochafik.com/">Olivier Chafik</a> that <a href="http://code.google.com/p/jnaerator/wiki/CreditsAndLicense">uses a few opensource projects.</a>.<br>
 * For help, please visit <a href="http://nativelibs4java.googlecode.com/">NativeLibs4Java</a> , <a href="http://rococoa.dev.java.net/">Rococoa</a>, or <a href="http://jna.dev.java.net/">JNA</a>.
 */
public class EP_TIME_SPEC extends Structure {
	/** seconds since Jan 1, 1970 */
	public long tv_sec;
	/** nanoseconds */
	public int tv_nsec;
	/** clock accuracy in seconds */
	public float tv_accuracy;
	public EP_TIME_SPEC() {
		super();
	}
	protected List<? > getFieldOrder() {
		return Arrays.asList("tv_sec", "tv_nsec", "tv_accuracy");
	}
	/**
	 * @param tv_sec seconds since Jan 1, 1970<br>
	 * @param tv_nsec nanoseconds<br>
	 * @param tv_accuracy clock accuracy in seconds
	 */
	public EP_TIME_SPEC(long tv_sec, int tv_nsec, float tv_accuracy) {
		super();
		this.tv_sec = tv_sec;
		this.tv_nsec = tv_nsec;
		this.tv_accuracy = tv_accuracy;
	}
	public EP_TIME_SPEC(Pointer peer) {
		super(peer);
	}
	public static class ByReference extends EP_TIME_SPEC implements Structure.ByReference {
		
	};
	public static class ByValue extends EP_TIME_SPEC implements Structure.ByValue {
		
	};
}
