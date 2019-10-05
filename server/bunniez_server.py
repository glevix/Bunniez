# Reference: https://github.com/p12tic/simple-http-file-server/

import image_processor as iproc
from http.server import BaseHTTPRequestHandler, HTTPServer
import os
import shutil
import socket
from requests import get
from distutils.dir_util import copy_tree

DEBUG = True

SERVER_WORKING_DIR = 'server_working'

util_dict = dict()
count = 0


def inject(sourcePath, targetPath):
    # First delete all files in target path
    for the_file in os.listdir(targetPath):
        file_path = os.path.join(targetPath, the_file)
        try:
            if os.path.isfile(file_path):
                os.unlink(file_path)
            elif os.path.isdir(file_path):
                shutil.rmtree(file_path)
        except Exception as e:
            print(e)
    # Now copy all of sourcePath into targetPath
    copy_tree(sourcePath, targetPath)


def bounding_boxes_to_string(boxes):
    if len(boxes) != 3:
        return 'error: should be 3 bounding box arrays'
    num_faces = len(boxes[0])
    if len(boxes[1]) != num_faces or len(boxes[2]) != num_faces:
        return 'error: number of faces found in each image not equal'
    if num_faces < 2:
        return 'error: less that 2 faces found per picture'
    return str(boxes)


def print_error_status():
    for key in util_dict:
        print('ID: ' + key)
        print('\tImages length: ' + str(len(util_dict[key].images)))
        print('\tReady: ' + str(util_dict[key].ready))


def get_new_id():
    global count
    count = count + 1
    return str(count - 1)


def convert(o):
    return int(o)


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
        print('\tid: ' + identity + ', request: ' + request + ', params: ' + parameters)
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
            print('\tNew id: ' + _identity)
        elif request == 'preprocess':
            if identity in util_dict:
                util = util_dict[identity]
                if DEBUG:
                    inject('test/', SERVER_WORKING_DIR + '/' + identity + '/input/')
                b = util.preprocess(int(parameters), SERVER_WORKING_DIR + '/' + identity + '/output/')
                _parameters = bounding_boxes_to_string(b)
                print('\tPreprocess,  id: ' + _identity)
            else:
                _parameters = 'bad ID'
        elif request == 'end':
            try:
                del(util_dict[identity])
            except KeyError:
                pass
            try:
                shutil.rmtree(SERVER_WORKING_DIR + '/' + _identity)
            except FileNotFoundError:
                pass
            print('\tEnd,  id: ' + _identity)
        else:
            _parameters = "ILLEGAL_REQUEST"
        self.send_response(200)
        self.set_params(_identity, _request, _parameters)
        self.end_headers()
        print("\tProcessed HEAD request")
        print('\t_id: ' + _identity + ', _request: ' + _request + ', _params: ' + _parameters)

    def do_PUT(self):
        """
        Resource: https://f-o.org.uk/2017/receiving-files-over-http-with-python.html
        """
        print("Received PUT request")
        global util_dict
        identity, request, parameters = self.get_params('PUT')
        print('\tid: ' + identity + ', request: ' + request + ', params: ' + parameters)
        _identity, _request, _parameters = identity, request, 'ok'
        if request == 'upload':
            if identity not in util_dict:
                _parameters = 'bad ID'
            else:
                util = util_dict[identity]
                filename = SERVER_WORKING_DIR + '/' + identity + '/input/' + parameters + '.jpg'
                if os.path.exists(filename):
                    _parameters = 'file exists'
                else:
                    file_length = int(self.headers['Content-Length'])
                    with open(filename, 'wb') as output_file:
                        output_file.write(self.rfile.read(file_length))
                    if not util.add_image(filename, int(parameters)):
                        print_error_status()
                    print('\tAdded image ' + filename + ', id: ' + _identity)
        else:
            _parameters = "ILLEGAL_REQUEST"
        self.send_response(200)
        self.set_params(_identity, _request, _parameters)
        self.end_headers()
        print("\tProcessed PUT request")
        print('\t_id: ' + _identity + ', _request: ' + _request + ', _params: ' + _parameters)

    def do_GET(self):
        print("Received GET request")
        global util_dict
        identity, request, parameters = self.get_params('GET')
        print('\tid: ' + identity + ', request: ' + request + ', params: ' + parameters)
        _identity, _request, _parameters = identity, request, 'ok'
        if request == 'get_pic':
            if identity not in util_dict:
                _parameters = 'bad ID'
                print('\tBad ID')
            else:
                filename = SERVER_WORKING_DIR + '/' + identity + '/output/' + parameters + '.jpg'
                print("\tRequested: " + filename)
                if os.path.exists(filename):
                    f = open(filename, 'rb')
                    self.send_response(200)
                    self.send_header('Content-type', 'application/octet-stream')
                    self.send_header('Content-length', os.path.getsize(filename))
                    self.set_params(_identity, _request, _parameters)
                    self.end_headers()
                    self.wfile.write(f.read())
                    f.close()
                    print("\tProcessed GET request")
                    print('\t_id: ' + _identity + ', _request: ' + _request + ', _params: ' + _parameters)
                    return
                else:
                    _parameters = 'no such file'
        elif request == 'process':
            if identity not in util_dict:
                _parameters = 'bad ID'
                print('\tBad ID')
            else:
                util = util_dict[identity]
                filename = SERVER_WORKING_DIR + '/' + identity + '/output/final.jpg'
                util.process([int(i) for i in parameters.split(',')], filename)
                if os.path.exists(filename):
                    f = open(filename)
                    self.send_response(200)
                    self.send_header('Content-type', 'application/octet-stream')
                    self.send_header('Content-length', os.path.getsize(filename))
                    self.set_params(_identity, _request, _parameters)
                    self.end_headers()
                    self.wfile.write(f.read())
                    f.close()
                    print("\tProcessed GET request")
                    print('\t_id: ' + _identity + ', _request: ' + _request + ', _params: ' + _parameters)
                    return
                else:
                    _parameters = 'could not locate output file'
        else:
            _parameters = "ILLEGAL_REQUEST"
        self.send_response(200)
        self.set_params(_identity, _request, _parameters)
        self.end_headers()
        print("\tProcessed GET request")
        print('\t_id: ' + _identity + ', _request: ' + _request + ', _params: ' + _parameters)


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

print('Server ready on IP ' + str(ip) + ' port ' + str(PORT))
try:
    server.serve_forever()
except KeyboardInterrupt:
    print('Server shutdown')
    server.socket.close()
