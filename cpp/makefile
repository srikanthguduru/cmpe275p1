# shared compilation settings
CFLAGS=-O0 -Wall -fmessage-length=0 -fno-strict-aliasing -Wno-multichar

# compiler identity
CC=g++ -m64

# identify some shell utilities 
ECHO=echo
MAKE=make
RM=rm

LIB=-lboost_system -lprotobuf

TARGET=client

# implicit build for cpp files
$(TARGET): 
	$(CC) $(CFLAGS) -o $@ *.cc *.cpp  $(LIB)

clean:
	-$(RM) -f *.o $(TARGET)
