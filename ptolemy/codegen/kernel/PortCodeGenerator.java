/**
 * 
 */
package ptolemy.codegen.kernel;

import ptolemy.codegen.c.domains.pn.kernel.PNDirector;
import ptolemy.kernel.util.IllegalActionException;

/**
 * @author mankit
 *
 */
public interface PortCodeGenerator {

    public String generateOffset(String offset, int channel, boolean isWrite, 
            Director directorHelper) throws IllegalActionException;

    
    public String updateOffset(int rate, Director directorHelper) throws IllegalActionException;

    
    public Object updateConnectedPortsOffset(int rate, Director director) throws IllegalActionException;

}
