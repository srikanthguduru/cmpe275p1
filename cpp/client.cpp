#include <boost/asio.hpp> 
#include <boost/array.hpp> 
#include <iostream> 
#include <string> 
#include "comm.pb.h"

boost::asio::io_service io_service; 
boost::asio::ip::tcp::resolver resolver(io_service); 
boost::asio::ip::tcp::socket sock(io_service); 
boost::array<char, 4096> buffer; 
Finger finger;

void read_handler(const boost::system::error_code &ec, std::size_t bytes_transferred) 
{ 
  if (!ec) 
  { 
    std::cout << std::string(buffer.data(), bytes_transferred) << std::endl; 
    sock.async_read_some(boost::asio::buffer(buffer), read_handler); 
  } 
} 

void connect_handler(const boost::system::error_code &ec) 
{ 
  if (!ec) 
  { 
	finger.set_tag("hello");
	finger.set_number(100);
    boost::asio::write(sock, boost::asio::buffer(finger.SerializeAsString()));
    sock.async_read_some(boost::asio::buffer(buffer), read_handler); 
  } 
} 

void resolve_handler(const boost::system::error_code &ec, boost::asio::ip::tcp::resolver::iterator it) 
{ 
  if (!ec) 
  { 
    sock.async_connect(*it, connect_handler); 
  } 
} 

int main() 
{ 
  boost::asio::ip::tcp::resolver::query query("localhost", "5570"); 
  resolver.async_resolve(query, resolve_handler); 
  io_service.run(); 
} 