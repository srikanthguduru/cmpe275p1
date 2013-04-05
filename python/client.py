#! /usr/bin/python

# See README.txt for information and build instructions.

import comm_pb2
import os
import sys
import socket               # Import socket module
import time
import struct

# This function fills in a Person message based on user input.
def buildPoke(tag, number):

    r = comm_pb2.Request()

    r.body.finger.tag = str(tag)
    r.body.finger.number = number
    
    
    r.header.originator = "python client"
    r.header.tag = str(tag + number + int(round(time.time() * 1000)))
    r.header.routing_id = comm_pb2.Header.FINGER
    
    m = r.SerializeToString()
    return m

def buildDoc():
    r = comm_pb2.Request()

    f = open('../resources/DSC_1832.jpg', 'rb')
    r.body.doc.img_byte = bytearray(f.read())
    f.close()

    r.body.doc.id = 3
    r.body.doc.file_name = "scene.jpg"
    r.body.doc.name_space = "apple"
    r.body.doc.file_type = "jpg"
    r.body.doc.location.x = 35
    r.body.doc.location.y = -122
    r.body.doc.time = int(round(time.time() * 1000))
    
    r.header.originator = "python client"
    r.header.tag = str(int(round(time.time() * 1000)))
    r.header.routing_id = comm_pb2.Header.DOCADD
    
    m = r.SerializeToString()
    return m
    
    

def buildNS():
    r = comm_pb2.Request()

    r.body.space.name = "Prateek"
    r.body.space.password = "cmpe275"
    r.body.space.user_id = "apple"
    r.body.space.city = "Fremont"
    r.body.space.zip_code = "94538"
    
    r.header.originator = "python client"
    r.header.tag = str(int(round(time.time() * 1000)))
    r.header.routing_id = comm_pb2.Header.NAMESPACEADD

    m = r.SerializeToString()
    return m


def CreateSocket():
    s = socket.socket()         # Create a socket object
    host = socket.gethostname() # Get local machine name
    port = 5570                # Reserve a port for your service.

    s.connect((host, port))
    m = buildPoke(1,2)
    packed_len = struct.pack('>L',len(m))
    s.sendall(packed_len + m)
    data = s.recv(4096)
    r = comm_pb2.Response()
    r.ParseFromString(data)
    print data

    m = buildNS()
    packed_len = struct.pack('>L',len(m))
    s.sendall(packed_len + m)
    data = s.recv(4096)
    r = comm_pb2.Response()
    r.ParseFromString(data)
    print data

    m = buildDoc()
    packed_len = struct.pack('>L',len(m))
    s.sendall(packed_len + m)
    data = s.recv(4096* 50)
    r = comm_pb2.Response()
    r.ParseFromString(data)
    print data

    s.close
    
# Main procedure:  Reads the entire address book from a file,
#   adds one person based on user input, then writes it back out to the same
#   file.

CreateSocket()