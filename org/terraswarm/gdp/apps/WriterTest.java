/* A GDP Writer Test application.

   Copyright (c) 2014 The Regents of the University of California.
   All rights reserved.
   Permission is hereby granted, without written agreement and without
   license or royalty fees, to use, copy, modify, and distribute this
   software and its documentation for any purpose, provided that the above
   copyright notice and the following two paragraphs appear in all copies
   of this software.

   IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY
   FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES
   ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
   THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF
   SUCH DAMAGE.

   THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
   INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
   MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
   PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
   CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES,
   ENHANCEMENTS, OR MODIFICATIONS.

   PT_COPYRIGHT_VERSION_2
   COPYRIGHTENDKEY

*/

package org.terraswarm.gdp.apps;

import com.sun.jna.Pointer;
import com.sun.jna.NativeLibrary;
import com.sun.jna.ptr.PointerByReference;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;

import org.ptolemy.fmi.NativeSizeT;

import org.terraswarm.gdp.EP_STAT;
import org.terraswarm.gdp.GdpLibrary;
import org.terraswarm.gdp.GdpLibrary.gdp_gcl_t;
import org.terraswarm.gdp.ep_stat_to_string;
import org.terraswarm.gdp.gdp_datum;

import ptolemy.util.StringUtilities;

/** Create a GCL.
    
    Invoke the main method of this class without arguments and it will
    create a random 256-bit name.  Run it with an argument and the
    value of the argument will be used to sha-256'd and used to create
    the internal name.  Du this without arguments and it will create a
    (random) 256-bit name, or with an argument which will be sha-256'd
    to create the internal name.  Text that is written to stdin will
    be written to the GCL.

    @author Christopher Brooks, based on the gdp/apps/writer-test.c by Eric Allman.
    @version $Id: AccessorOne.java 69931 2014-08-29 23:36:38Z eal $
    @since Ptolemy II 10.0
    @Pt.ProposedRating Red (eal)
    @Pt.AcceptedRating Red (cxh)
*/
public class WriterTest {
    public static void main(String [] argv) throws Throwable {

	PointerByReference /*gdp_gcl_t*/ gclh = null;
        ByteBuffer /*gcl_name_t*/ gcliname = null;
	int opt;
	EP_STAT estat;
	boolean append = false;
	String xname = null;
	String buf = "";

        String ptII = StringUtilities.getProperty("ptolemy.ptII.dir");

        // FIXME: use .so for linux:
        NativeLibrary nativeLibrary = NativeLibrary.getInstance(ptII + "/lib/libgdp.dylib");

        int argc = argv.length;
        for (int i = 0; i < argv.length; i++) {
            if (argv[i].equals('a')) {
                append = true;
                argc--;
            } else if (argv[i].equals('D')) {
                argc--;
                GdpLibrary.INSTANCE.ep_dbg_set(argv[i+1]);
            }
        }

        if (argc > 0) {
            xname = argv[argc];
            argc--;
        } 
	if (argc != 0 || (append && xname == null)) {
            System.err.println("Usage: WriterTest [-D dbgspec] [-a] [<gcl_name>]\n"
                    + "  (name is required for -a)");
            System.exit(64 /* EX_USAGE from /usr/includes/sysexits.h */);
	}

	estat = GdpLibrary.INSTANCE.gdp_init();
	if (/*!EP_STAT_ISOK(estat)*/ estat.code.intValue() != 0) {
            System.err.println("GDP Initialization failed");
            _fail0(estat);
	}

	if (xname == null) {
            // create a new GCL handle
            estat = GdpLibrary.INSTANCE.gdp_gcl_create((ByteBuffer)null, gclh);
	} else {
            GdpLibrary.INSTANCE.gdp_gcl_parse_name(xname, gcliname);
            if (append) {
                estat = GdpLibrary.INSTANCE.gdp_gcl_open(gcliname, GdpLibrary.gdp_iomode_t.GDP_MODE_AO, gclh);
            } else {
                estat = GdpLibrary.INSTANCE.gdp_gcl_create(gcliname, gclh);
            }
	}
	// EP_STAT_CHECK(estat, goto fail0);
        if (estat.code.intValue() != 0) {
            _fail0(estat);
        }

	//GdpLibrary.INSTANCE.gdp_gcl_print(gclh, stdout, 0, 0);
        System.out.print("GCL" + System.identityHashCode(gclh) + ":");

        // FIXME: Need to allocate something 44 chars long (GDP_GCL_PNAME_LEN in gdp.h)
        // GdpLibrary.INSTANCE.gdp_gcl_printable_name(gclh, nbuf);
        // Print nbuf


	System.out.println("Starting to read input.");

	gdp_datum datum = GdpLibrary.INSTANCE.gdp_datum_new();


        BufferedReader bufferedReader = null;
        try {
           bufferedReader = new BufferedReader(new InputStreamReader(System.in));
            while((buf=bufferedReader.readLine())!=null){
                System.out.println("Got input \"" +  buf + "\"");
                // FIXME: gdp/gdp_buf.h has
                // #define gdp_buf_write(b, i, z)	evbuffer_add(b, i, z)
                // evbuffer_add is declared in /usr/local/include/event2/buffer.h
                //gdp_buf_write(datum->dbuf, buf, buf.length());

                //evbuffer_add(datum.dbuf, buf, buf.length());
                System.err.println("FIXME: no definition of evbuffer_add");

		estat = GdpLibrary.INSTANCE.gdp_gcl_publish(gclh, datum);
                if (estat.code.intValue() != 0) {
                    _fail1(estat, gclh);
                }
                // Instead of calling the gdp_datum_print() method in C, we implement our own.
                //gdp_datum_print(datum, stdout);
                _gdp_datum_print(datum/*, stdout*/);

            }
        } finally {
            if (bufferedReader != null) {
                bufferedReader.close();
            }
        }
	GdpLibrary.INSTANCE.gdp_datum_free(datum);
        _fail0(estat);
    }

    private static int _fail1(EP_STAT estat, PointerByReference gclh) {
	GdpLibrary.INSTANCE.gdp_gcl_close(gclh);
        return _fail0(estat);
    }

    private static int _fail0(EP_STAT estat) {
        // FIXME: EP_STAT_ISOK is a macro in the original c code.  See ../_jnaerator.macros.cpp.
        //if (EP_STAT_ISOK(estat)) {
        //    estat = EP_STAT_OK;
        //}

        // FIXME: writer-test.c has:
	// fprintf(stderr, "exiting with status %s\n",
	//		ep_stat_tostr(estat, buf, sizeof buf));
        // I have no idea what to do with ep_stat_tostr(), so we just print
	System.err.println("exiting with status " + estat);

	return /*!EP_STAT_ISOK(estat)*/ (estat.code.intValue() == 0 ? 1 : 0);
    }

    /** Print the datum to standard out.  This is a port of
     *  gdp_datum_print from gdp/gdp_api.c by Eric Allman.
     *  @param datum The datum to be printed.
     */   
    private static void _gdp_datum_print(gdp_datum datum) {
        // unsigned char *d; // From gdp_api.c
        int length = -1;
        if (datum == null) {
            System.out.println("null datum");
        }
        System.out.print("GDP record " + datum.recno);
        if (datum.dbuf == null) {
            System.out.print("no data");
        } else {
            // In gdp_api.c, gdp_datum_print() calls:
            // l = gdp_buf_getlength(datum->dbuf);
            // gdp_buf.h has an inline call to evbuffer_get_length(buf), which we don't yet have 
            //length = GdpLibrary.INSTANCE.gdp_buf_getlength(datum.dbuf);
            System.out.print("len " + datum.dlen + "/" + length);

            // In gdp_api.c, this method calls:
            // d = gdp_buf_getptr(datum->dbuf, l);
            // gdp_buf.h has:
            // #define gdp_buf_getptr(b, z)	evbuffer_pullup(b, z)
            // So, we would need to call evbuffer_pullup() here.

            // A different idea would be to have a gdp_buf.c method
            // that calls evbuffer_pullup so that we don't need to run
            // JNA on that class.
        }
        if (datum.ts.tv_sec != Long.MIN_VALUE) {
            System.out.print(", timestamp ");
            System.out.print("FIXME");
            //ep_time_print(&datum->ts, fp, true);
        } else {
            System.out.print(", no timestamp ");
        }
        if (length > 0) {
            // gdp_api.c has
            //fprintf(fp, "\n	 %s%.*s%s", EpChar->lquote, l, d, EpChar->rquote);
            // However, we don't know how to get the value of d yet.
            System.out.print("\n \"FIXME\"");
        }
    }
}
