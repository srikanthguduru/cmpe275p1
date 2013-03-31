#! /usr/bin/python

# See README.txt for information and build instructions.

import comm_pb2
import sys
import socket               # Import socket module
import time

# This function fills in a Person message based on user input.
def buildPoke(tag, number):

    r = comm_pb2.Request()

    r.body.finger.tag = "1"
    r.body.finger.number = 2
    
    
    r.header.originator = "python client"
    r.header.tag = str(tag + number + int(round(time.time() * 1000)))
    r.header.time = int(round(time.time() * 1000))
    r.header.routing_id = comm_pb2.Header.FINGER
    
    return r.SerializeToString()



def CreateSocket():
    s = socket.socket()         # Create a socket object
    host = socket.gethostname() # Get local machine name
    port = 5570                # Reserve a port for your service.

    s.connect((host, port))
    s.send(buildPoke(1,2))
    s.close

# Main procedure:  Reads the entire address book from a file,
#   adds one person based on user input, then writes it back out to the same
#   file.

CreateSocket()