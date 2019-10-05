# Reference: https://github.com/p12tic/simple-http-file-server/

import image_processor as iproc
from http.server import BaseHTTPRequestHandler, HTTPServer
import cgi
import ssl
import os, shutil
import socket
from requests import get

SERVER_WORKING_DIR = 'server_working'

util_dict = dict()
count = 0


def get_new_id():
    global count
    count = count + 1
    return str(count - 1)


class RequestHandler(BaseHTTPRequestHandler):

    def get_params(self, method):
        return self.headers['id'], self.headers['request'], self.headers['params']

    def set_params(self, identity, request, parameters):
        self.send_header('id', identity)
        self.send_header('request', request)
        self.send_header('params', parameters)

    def do_HEAD(self):
        print("Received HEAD request")
        global util_dict
        identity, request, parameters = self.get_params('PUT')
        _identity, _request, _parameters = identity, request, 'ok'
        if request == 'init':
            # Allocate a new id and a directory
            _identity = get_new_id()
            try:
                os.mkdir(SERVER_WORKING_DIR + '/' + _identity)
            except FileExistsError:
                shutil.rmtree(SERVER_WORKING_DIR + '/' + _identity)
                os.mkdir(SERVER_WORKING_DIR + '/' + _identity)
            os.mkdir(SERVER_WORKING_DIR + '/' + _identity + '/input')
            os.mkdir(SERVER_WORKING_DIR + '/' + _identity + '/output')
            util = iproc.Imutils()
            util_dict[_identity] = util
            print('New id: ' + _identity)
        elif request == 'preprocess':
            util = util_dict[identity]
            _parameters = str(util.preprocess(int(parameters), SERVER_WORKING_DIR + '/' + identity + '/output/'))
            print('Preprocess,  id: ' + _identity)
        elif request == 'end':
            try:
                del(util_dict[identity])
            except KeyError:
                pass
            try:
                shutil.rmtree(SERVER_WORKING_DIR + '/' + _identity)
            except FileNotFoundError:
                pass
            print('End,  id: ' + _identity)
        else:
            _parameters = "ILLEGAL_REQUEST"
        self.send_response(200)
        self.set_params(_identity, _request, _parameters)
        self.end_headers()
        print("Processed HEAD request")

    def do_PUT(self):
        """
        Resource: https://f-o.org.uk/2017/receiving-files-over-http-with-python.html
        """
        print("Received PUT request")
        global util_dict
        identity, request, parameters = self.get_params('PUT')
        _identity, _request, _parameters = identity, request, 'ok'
        util = util_dict[identity]
        if request == 'upload':
            if identity not in util_dict:
                _parameters = 'bad ID'
            else:
                filename = SERVER_WORKING_DIR + '/' + identity + '/input/' + parameters + '.jpg'
                if os.path.exists(filename):
                    _parameters = 'file exists'
                else:
                    file_length = int(self.headers['Content-Length'])
                    with open(filename, 'wb') as output_file:
                        output_file.write(self.rfile.read(file_length))
                    util.add_image(filename)
                    print('Added image, id: ' + _identity)
        else:
            _parameters = "ILLEGAL_REQUEST"
        self.send_response(200)
        self.set_params(_identity, _request, _parameters)
        self.end_headers()
        print("Processed PUT request")

    def do_GET(self):
        global util_dict
        identity, request, parameters = self.get_params('GET')
        _identity, _request, _parameters = identity, request, 'ok'
        if request == 'get_pic':
            if identity not in util_dict:
                _parameters = 'bad ID'
            else:
                filename = SERVER_WORKING_DIR + '/' + identity + '/output/' + parameters + '.jpg'
                if os.path.exists(filename):
                    f = open(filename)
                    self.send_response(200)
                    self.send_header('Content-type', 'application/octet-stream')
                    self.set_params(_identity, _request, _parameters)
                    self.end_headers()
                    self.wfile.write(f.read())
                    f.close()
                    return
                else:
                    _parameters = 'no such file'
        elif request == 'process':
            util = util_dict[identity]
            filename = SERVER_WORKING_DIR + '/' + identity + '/output/final.jpg'
            util.process([int(i) for i in parameters.split(',')], filename)
            if os.path.exists(filename):
                f = open(filename)
                self.send_response(200)
                self.send_header('Content-type', 'application/octet-stream')
                self.set_params(_identity, _request, _parameters)
                self.end_headers()
                self.wfile.write(f.read())
                f.close()
                return
            else:
                _parameters = 'could not locate output file'
        else:
            _parameters = "ILLEGAL_REQUEST"
        self.send_response(200)
        self.set_params(_identity, _request, _parameters)
        self.end_headers()
        print("Processed GET request")


def get_ip():
    host_ip = '?'
    try:
        host_name = socket.gethostname()
        host_ip = socket.gethostbyname(host_name)
    except:
        print("Unable to get IP")
    return host_ip


PORT = 8080
server = HTTPServer(('', PORT), RequestHandler)
# server.socket = ssl.wrap_socket(server.socket, certfile='', server_side=True)
ip = get_ip()
try:
    external_ip = get('https://api.ipify.org').text
    print('External ip: ' + str(external_ip))
except:
    print('External ip: ' + 'unknown')
try:
    os.mkdir(SERVER_WORKING_DIR)
except FileExistsError:
    shutil.rmtree(SERVER_WORKING_DIR)
    os.mkdir(SERVER_WORKING_DIR)
except FileExistsError:
    print('Server working directory exists. Please clean up and try again')
    exit()

print('Server ready on IP ' + str(ip) + ' port ' + str(PORT))
try:
    server.serve_forever()
except KeyboardInterrupt:
    print('Server shutdown')
    server.socket.close()
