#! /usr/bin/python

# See README.txt for information and build instructions.

import comm_pb2
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


def CreateSocket():
    s = socket.socket()         # Create a socket object
    host = socket.gethostname() # Get local machine name
    port = 5570                # Reserve a port for your service.

    s.connect((host, port))
    m = buildPoke(1,2)
    packed_len = struct.pack('>L',len(m))
    s.sendall(packed_len + m)
    s.close

# Main procedure:  Reads the entire address book from a file,
#   adds one person based on user input, then writes it back out to the same
#   file.

CreateSocket()