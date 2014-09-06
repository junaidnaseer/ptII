package org.terraswarm.gdp;
import org.ptolemy.fmi.NativeSizeT; //Use NativeSizeT intead of NativeSize
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.PointerByReference;
import java.util.Arrays;
import java.util.List;
import org.terraswarm.gdp.GdpLibrary.EP_THR_MUTEX;
/**
 * <i>native declaration : src/gdp/gdp/gdp.h:51</i><br>
 * This file was autogenerated by <a href="http://jnaerator.googlecode.com/">JNAerator</a>,<br>
 * a tool written by <a href="http://ochafik.com/">Olivier Chafik</a> that <a href="http://code.google.com/p/jnaerator/wiki/CreditsAndLicense">uses a few opensource projects.</a>.<br>
 * For help, please visit <a href="http://nativelibs4java.googlecode.com/">NativeLibs4Java</a> , <a href="http://rococoa.dev.java.net/">Rococoa</a>, or <a href="http://jna.dev.java.net/">JNA</a>.
 */
public abstract class gdp_datum extends Structure {
	/**
	 * locking mutex (mostly for dbuf)<br>
	 * C type : EP_THR_MUTEX
	 */
	public EP_THR_MUTEX mutex;
	/**
	 * next in free list<br>
	 * C type : gdp_datum*
	 */
	public gdp_datum.ByReference next;
	/** Conversion Error : inuse:1 (This runtime does not support bit fields : JNA (please use BridJ instead)) */
	/**
	 * the record number<br>
	 * C type : gdp_recno_t
	 */
	public long recno;
	/**
	 * timestamp for this message<br>
	 * C type : EP_TIME_SPEC
	 */
	public EP_TIME_SPEC ts;
	/** length of data buffer (redundant) */
	public NativeSizeT dlen;
	/**
	 * data buffer<br>
	 * C type : gdp_buf_t*
	 */
	public PointerByReference dbuf;
	public gdp_datum() {
		super();
	}
	protected List<? > getFieldOrder() {
		return Arrays.asList("mutex", "next", "recno", "ts", "dlen", "dbuf");
	}
	/**
	 * @param mutex locking mutex (mostly for dbuf)<br>
	 * C type : EP_THR_MUTEX<br>
	 * @param next next in free list<br>
	 * C type : gdp_datum*<br>
	 * @param recno the record number<br>
	 * C type : gdp_recno_t<br>
	 * @param ts timestamp for this message<br>
	 * C type : EP_TIME_SPEC<br>
	 * @param dlen length of data buffer (redundant)<br>
	 * @param dbuf data buffer<br>
	 * C type : gdp_buf_t*
	 */
	public gdp_datum(EP_THR_MUTEX mutex, gdp_datum.ByReference next, long recno, EP_TIME_SPEC ts, NativeSizeT dlen, PointerByReference dbuf) {
		super();
		this.mutex = mutex;
		this.next = next;
		this.recno = recno;
		this.ts = ts;
		this.dlen = dlen;
		this.dbuf = dbuf;
	}
	public gdp_datum(Pointer peer) {
		super(peer);
	}
	public static abstract class ByReference extends gdp_datum implements Structure.ByReference {
		
	};
	public static abstract class ByValue extends gdp_datum implements Structure.ByValue {
		
	};
}
