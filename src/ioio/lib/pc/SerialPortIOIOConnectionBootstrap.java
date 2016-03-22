// 
// Decompiled by Procyon v0.5.30
// 

package ioio.lib.pc;

import purejavacomm.CommPort;
import purejavacomm.PortInUseException;
import java.util.Enumeration;
import java.util.List;
import purejavacomm.CommPortIdentifier;
import java.util.LinkedList;
import java.util.Iterator;
import ioio.lib.api.IOIOConnection;
import ioio.lib.spi.Log;
import ioio.lib.spi.IOIOConnectionFactory;
import java.util.Collection;
import ioio.lib.spi.IOIOConnectionBootstrap;

public class SerialPortIOIOConnectionBootstrap implements IOIOConnectionBootstrap
{
    private static final String TAG = "SerialPortIOIOConnectionBootstrap";
    
    @Override
    public void getFactories(final Collection<IOIOConnectionFactory> result) {
        Collection<String> ports = getExplicitPorts();
        if (ports == null) {
            Log.w("SerialPortIOIOConnectionBootstrap", "ioio.SerialPorts not defined.\nWill attempt to enumerate all possible ports (slow) and connect to a IOIO over each one.\nTo fix, add the -Dioio.SerialPorts=xyz argument to the java command line, where xyz is a colon-separated list of port identifiers, e.g. COM1:COM2.");
            ports = getAllOpenablePorts();
        }
        for (final String port : ports) {
            Log.d("SerialPortIOIOConnectionBootstrap", "Adding serial port " + port);
            result.add(new IOIOConnectionFactory() {
                @Override
                public String getType() {
                    return SerialPortIOIOConnection.class.getCanonicalName();
                }
                
                @Override
                public Object getExtra() {
                    return port;
                }
                
                @Override
                public IOIOConnection createConnection() {
                    return new SerialPortIOIOConnection(port);
                }
            });
        }
    }
    
    static Collection<String> getAllOpenablePorts() {
        final List<String> result = new LinkedList<String>();
        final Enumeration<CommPortIdentifier> identifiers = (Enumeration<CommPortIdentifier>)CommPortIdentifier.getPortIdentifiers();
        while (identifiers.hasMoreElements()) {
            final CommPortIdentifier identifier = identifiers.nextElement();
            if (identifier.getPortType() == 1) {
                if (checkIdentifier(identifier)) {
                    Log.d("SerialPortIOIOConnectionBootstrap", "Adding serial port " + identifier.getName());
                    result.add(identifier.getName());
                }
                else {
                    Log.w("SerialPortIOIOConnectionBootstrap", "Serial port " + identifier.getName() + " cannot be opened. Not adding.");
                }
            }
        }
        return result;
    }
    
    static Collection<String> getExplicitPorts() {
        final String property = System.getProperty("ioio.SerialPorts");
        if (property == null) {
            return null;
        }
        final List<String> result = new LinkedList<String>();
        final String[] portNames = property.split(":");
        String[] array;
        for (int length = (array = portNames).length, i = 0; i < length; ++i) {
            final String portName = array[i];
            result.add(portName);
        }
        return result;
    }
    
    static boolean checkIdentifier(final CommPortIdentifier id) {
        if (id.isCurrentlyOwned()) {
            return false;
        }
        try {
            final CommPort port = id.open(SerialPortIOIOConnectionBootstrap.class.getName(), 1000);
            port.close();
        }
        catch (PortInUseException e) {
            return false;
        }
        return true;
    }
}
