/**
 * Module supporting sockets.
 * This module defines three classes, SocketClient, SocketServer, and Socket.
 * To make a connection, create an instance of SocketServer, set up listeners,
 * and start the server. On another machine (or the same machine), create
 * an instance of SocketClient and set up listeners and/or invoke send() to send
 * a message. When a client connects to the SocketServer, the SocketServer will create
 * an instance of the Socket object.
 *
 * This module also provides two utility functions that return arrays
 * of MIME types supported for sending or receiving messages.
 *
 * @module socket
 * @authors: Edward A. Lee
 * @copyright: http://terraswarm.org/accessors/copyright.txt
 */

var SocketHelper = Java.type('ptolemy.actor.lib.jjs.modules.socket.SocketHelper');
var EventEmitter = require('events').EventEmitter;

///////////////////////////////////////////////////////////////////////////////
//// supportedReceiveTypes

/** Return an array of the types supported by the current host for
 *  receiveType arguments.
 */
exports.supportedReceiveTypes = function() {
    return SocketHelper.supportedReceiveTypes();
}

///////////////////////////////////////////////////////////////////////////////
//// supportedSendTypes

/** Return an array of the types supported by the current host for
 *  sendType arguments.
 */
exports.supportedSendTypes = function() {
    return SocketHelper.supportedSendTypes();
}

///////////////////////////////////////////////////////////////////////////////
//// defaultClientOptions

/** The default options for socket connections from the client side.
 */
var defaultClientOptions = {
    'connectTimeout': 6000, // in milliseconds.
    'idleTimeout': 0, // In second. 0 means don't timeout.
    'discardMessagesBeforeOpen': false,
    'keepAlive': true,
    'receiveBufferSize': 65536,
    'receiveType': 'string',
    'reconnectAttempts': 10,
    'reconnectInterval': 100,
    'sendBufferSize': 65536,
    'sendType': 'string',
    'sslTls': false,
    'trustAll': true,
}

// FIXME: 'trustAll' is not well explained. What does it mean?
// There are additional options in Vert.x NetClientOptions that
// are not documented in the Vert.x documentation, so I don't know what
// they mean.

///////////////////////////////////////////////////////////////////////////////
//// SocketClient

/** Construct an instance of a socket client that can send or receive messages
 *  to a server at the specified host and port.
 *  The returned object subclasses EventEmitter and emits the following events:
 *
 *  * open: Emitted with no arguments when the socket has been successfully opened.
 *  * data: Emitted with the data as an argument when data arrives on the socket.
 *  * close: Emitted with no arguments when the socket is closed.
 *  * error: Emitted with an error message when an error occurs.
 *
 *  You can invoke the send() function of this SocketClient object
 *  to send data to the server. If the socket is not opened yet,
 *  then data will be discarded or queued to be sent later,
 *  depending on the value of the discardMessagesBeforeOpen option
 *  (which defaults to false).
 *
 *  The event 'close' will be emitted when the socket is closed, and 'error' if an
 *  an error occurs (with an error message as an argument).
 *
 *  A simple example that sends a message, and closes the socket on receiving a reply.
 *  
 *      var socket = require('socket');
 *      var client = new socket.SocketClient();
 *      client.on('open', function() {
 *          client.send('hello world');
 *      });
 *      client.on('data', function onData(data) {
 *          print('Received from socket: ' + data);
 *          client.close();
 *      });
 *  
 *  The options argument is a JSON object that can include:
 *  * connectTimeout: The time to wait (in milliseconds) before declaring
 *    a connection attempt to have failed.
 *  * idleTimeout: The amount of idle time in seconds that will cause
 *    a disconnection of a socket. This defaults to 0, which means no
 *    timeout.
 *  * discardMessagesBeforeOpen: If true, then discard any messages
 *    passed to SocketClient.send() before the socket is opened. If false,
 *    then queue the messages to be sent when the socket opens. This
 *    defaults to false.
 *  * keepAlive: Whether to keep a connection alive and reuse it. This
 *    defaults to true.
 *  * receiveBufferSize: The size of the receive buffer. Defaults to
 *    65536.
 *  * receiveType: See below.
 *  * reconnectAttempts: The number of times to try to reconnect.
 *    If this is greater than 0, then a failure to attempt will trigger
 *    additional attempts. This defaults to 10.
 *  * reconnectInterval: The time between reconnect attempts, in
 *    milliseconds. This defaults to 100.
 *  * sendBufferSize: The size of the receive buffer. Defaults to
 *    65536.
 *  * sendType: See below.
 *  * sslTls: Whether SSL/TLS is enabled. This defaults to false.
 *  * trustAll: Whether to trust servers. This defaults to true.
 *
 *  The send and receive types can be one of 'string', 'number',
 *  or 'byte', defaulting to 'string'. For connecting to sockets
 *  that are not JavaScript applications, they can alternatively be
 *  'double', 'float', 'int', 'long', 'short', 'unsignedByte',
 *  'unsignedInt', or 'unsignedShort'. In these cases, received
 *  data will be converted to JavaScript 'number' when emitted.
 *  For sent data, this will try to convert a JavaScript number
 *  to the specified type. The type 'number' is equivalent
 *  to 'double'.
 *  
 *  The meaning of the options is (partially) defined here:
 *     http://vertx.io/docs/vertx-core/java/
 *
 *  @param options The options.
 */
exports.SocketClient = function(port, host, options) {
    // Set default values of arguments.
    // Careful: port == 0 means to find an available port, I think.
    if (port == null) {
        port = 4000;
    }
    host = host || 'localhost';

    // Fill in default values.
    this.options = options || {};
    this.options = util._extend(defaultClientOptions, this.options);
    
    this.helper = SocketHelper.getOrCreateHelper(actor);
    this.helper.openClientSocket(this, port, host, this.options);
    
    this.pendingSends = [];
}
util.inherits(exports.SocketClient, EventEmitter);

/** This method will be called by the helper when a client's request to
 *  open a socket has been granted and the socket is open.
 *  This function should not be called by users of this module.
 *  It will emit an 'open' event with no arguments.
 *  @param netSocket The Vert.x NetSocket object.
 *  @param client The Vert.x NetClient object that opened the socket.
 */
exports.SocketClient.prototype._opened = function(netSocket, client) {
    // For a client, this instance of SocketClient will be the event emitter.

    // Because we are creating an inner class, the first argument needs to be
    // the instance of the enclosing socketHelper class.
    this.wrapper = new SocketHelper.SocketWrapper(this.helper, this, netSocket);
    this.emit('open');
    
    // Send any pending data.
    for (var i = 0; i < this.pendingSends.length; i++) {
        this.wrapper.send(this.pendingSends[i]);
    }
    this.pendingSends = [];
}

/** Send data over the socket.
 *  If the socket has not yet been successfully opened, then queue
 *  data to be sent later, when the socket is opened, unless
 *  discardMessagesBeforeOpen is true.
 *  @param data The data to send.
 */
exports.SocketClient.prototype.send = function(data) {
    if (this.wrapper) {
        this.wrapper.send(data);
    } else {
        // FIXME: Check whether to discard.
        this.pendingSends.push(data);
    }      
}

/** Close the current connection with the server.
 *  If there is data that was passed to send() but has not yet
 *  been successfully sent (because the socket was not open),
 *  then throw an exception.
 */
exports.SocketClient.prototype.close = function() {
    if (this.wrapper) {
        this.wrapper.close();
    } else {
        // FIXME: Set a flag to close immediately upon opening.
    }      
}

///////////////////////////////////////////////////////////////////////////////
//// defaultServerOptions

/** The default options for socket servers.
 */
var defaultServerOptions = {
    'clientAuth': 'none', // No SSL/TSL will be used.
    'hostInterface': '0.0.0.0', // Means listen on all available interfaces.
    'idleTimeout': 0, // In second. 0 means don't timeout.
    'keepAlive': true,
    'port': 4000,
    'receiveBufferSize': 65536,
    'receiveType': 'string',
    'sendBufferSize': 65536,
    'sendType': 'string',
    'sslTls': false,
}

// FIXME: one of the server options in NetServerOptions is 'acceptBacklog'.
// This is undocumented in Vert.x, so I have no idea what it is. Left it out.
// Also, 'reuseAddress', 'SoLinger', 'TcpNoDelay', 'trafficClass',
// 'usePooledBuffers'.  Maybe the TCP wikipedia page will help.

///////////////////////////////////////////////////////////////////////////////
//// SocketServer

/** Construct an instance of a socket server that listens for connection
 *  requests and opens sockets when it receives them. 
 *  After invoking this constructor (using new), the user can set up
 *  listeners for the following events:
 *
 *  * listening: Emitted when the server is listening.
 *    This will be passed the port number that the server is listening
 *    on (this is useful if the port is specified to be 0).
 *  * connection: Emitted when a new connection is established
 *    after a request from (possibly remote) client.
 *    This will be passed an instance of a Socket class
 *    that can be used to send data or to close the socket.
 *    The instance of Socket is also an event emitter that
 *    emits 'close', 'data', and 'error' events.
 *  * error: Emitted if the server fails to start listening.
 *    This will be passed an error message.
 *
 *  FIXME: Out of date: A typical usage pattern looks like this:
 * 
 *     var server = new WebSocket.Server({'port':8082});
 *     server.on('listening', onListening);
 *     server.on('connection', onConnection);
 *     server.start();
 * 
 *  where onListening is a handler for an event that this Server emits
 *  when it is listening for connections, and onConnection is a handler
 *  for an event that this Server emits when a client requests a websocket
 *  connection and the socket has been successfully established.
 *  When the 'connection' event is emitted, it will be passed a Socket object,
 *  and the onConnection handler can register a listener for 'message' events
 *  on that Socket object, as follows:
 * 
 *     server.on('connection', function(socket) {
 *        socket.on('message', function(message) {
 *            console.log(message);
 *            socket.send('Reply message');
 *        });
 *     });
 * 
 *  The Socket object also has a close() function that allows the server to close
 *  the connection.
 * 
 *  The options argument is a JSON object containing the following optional fields:
 *  * clientAuth: One of 'none', 'request', or 'required', meaning whether it
 *    requires that a certificate be presented.
 *  * hostInterface: The name of the network interface to use for listening,
 *    e.g. 'localhost'. The default is '0.0.0.0', which means to
 *    listen on all available interfaces.
 *  * idleTimeout: The amount of idle time in seconds that will cause
 *    a disconnection of a socket. This defaults to 0, which means no
 *    timeout.
 *  * keepAlive: Whether to keep a connection alive and reuse it. This
 *    defaults to true.
 *  * port: The default port to listen on. This defaults to 4000.
 *    a value of 0 means to choose a random ephemeral free port.
 *  * receiveBufferSize: The size of the receive buffer. Defaults to
 *    65536.
 *  * receiveType: See below.
 *  * sendBufferSize: The size of the receive buffer. Defaults to
 *    65536.
 *  * sendType: See below.
 *  * sslTls: Whether SSL/TLS is enabled. This defaults to false.
 *
 *  The send and receive types can be one of 'string', 'number',
 *  or 'byte', defaulting to 'string'. All sockets handled by this
 *  server must use the same send and receive types.
 *  For connecting to sockets
 *  that are not JavaScript applications, they can alternatively be
 *  'double', 'float', 'int', 'long', 'short', 'unsignedByte',
 *  'unsignedInt', or 'unsignedShort'. In these cases, received
 *  data will be converted to JavaScript 'number' when emitted.
 *  For sent data, this will try to convert a JavaScript number
 *  to the specified type. The type 'number' is equivalent
 *  to 'double'.
 *
 *  The meaning of the options is (partially)defined here:
 *     http://vertx.io/docs/vertx-core/java/
 *
 *  @param options The options.
 */
exports.SocketServer = function(options) {
    // Fill in default values.
    this.options = options || {};
    this.options = util._extend(defaultServerOptions, this.options);

    this.helper = SocketHelper.getOrCreateHelper(actor);
    this.helper.openServer(this, this.options);
}
util.inherits(exports.SocketServer, EventEmitter);

/** Stop the server. */
exports.SocketServer.prototype.close = function() {
    if (this.server) {
        this.server.close();
        this.server = null;
    }
}

/** Notify this SocketServer that the server has been created.
 *  This is called by the helper, and should not be called by the user of this module.
 *  @param netServer The Vert.x NetServer object.
 */
exports.SocketServer.prototype._serverCreated = function(netServer) {
    this.server = netServer;
}

/** Notify that a handshake was successful and a websocket has been created.
 *  This is called by the helper class is not meant to be called by the JavaScript
 *  programmer. When this is called, the Server will create a new Socket object
 *  and emit a 'connection' event with that Socket as an argument.
 *  The 'connection' handler can then register for 'data' events from the
 *  Socket or issue replies to the Socket using its send() function.
 *  It can also close() the Socket.
 *  @param netSocket The Vert.x NetSocket object.
 *  @param server The Vert.x NetServer object.
 */
exports.SocketServer.prototype._socketCreated = function(netSocket) {
    var socket = new exports.Socket(this.helper, netSocket);
    this.emit('connection', socket);
}

/////////////////////////////////////////////////////////////////
//// Socket

/** Construct (using new) a Socket object for the server side of a new connection.
 *  This is called by the _socketCreated function above whenever a new connection is
 *  established at the request of a client. It should not normally be called by
 *  the JavaScript programmer. The returned Socket is an event emitter that emits
 *  the following events:
 *
 *  * data: Emitted when data is received on any socket handled by this server.
 *    This will be passed the data.
 *  * close: Emitted when a socket is closed.
 *    This will be passed the instance of SocketWrapper for
 *    the socket that was closed.
 *  * error: Emitted when an error occurs.
 *    This will be passed an error message.
 *
 *  @param helper The instance of SocketHelper that is helping.
 *  @param netSocket The Vert.x NetSocket object.
 */
exports.Socket = function(helper, netSocket) {
    // For a server side socket, this instance of Socket will be the event emitter.

    // Because we are creating an inner class, the first argument needs to be
    // the instance of the enclosing socketHelper class.
    this.wrapper = new SocketHelper.SocketWrapper(helper, this, netSocket);
}
util.inherits(exports.Socket, EventEmitter);

/** Close the socket. Normally, this would be called on the client side,
 *  not on the server side. But the server can also close the connection.
 */
exports.Socket.prototype.close = function() {
    this.wrapper.close();
}

/** Send data over the web socket.
 *  The data can be anything that has a JSON representation.
 *  @param data The data to send.
 */
exports.Socket.prototype.send = function(data) {
    this.wrapper.send(data);
}