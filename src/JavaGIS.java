

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import org.postgis.PGgeometry;

public class JavaGIS { 

public static void main(String[] args) { 

  java.sql.Connection conn; 

  try { 
    /* 
    * Load the JDBC driver and establish a connection. 
    */
    Class.forName("org.postgresql.Driver"); 
    String url = "jdbc:postgresql://localhost:5432/lifestream"; 
    conn = DriverManager.getConnection(url, "mdhoble", ""); 
    /* 
    * Add the geometry types to the connection. Note that you 
    * must cast the connection to the pgsql-specific connection 
    * implementation before calling the addDataType() method. 
    */
    ((org.postgresql.PGConnection)conn).addDataType("geometry",Class.forName("org.postgis.PGgeometry"));
    ((org.postgresql.PGConnection)conn).addDataType("box3d",Class.forName("org.postgis.PGbox3d"));
    /* 
    * Create a statement and execute a select query. 
    */ 
    Statement s = conn.createStatement(); 
    ResultSet r = s.executeQuery("select geom,id from geomtable"); 
    while( r.next() ) { 
      /* 
      * Retrieve the geometry as an object then cast it to the geometry type. 
      * Print things out. 
      */ 
      PGgeometry geom = (PGgeometry)r.getObject(1); 
      int id = r.getInt(2); 
      System.out.println("Row " + id + ":");
      System.out.println(geom.toString()); 
    } 
    s.close(); 
    conn.close(); 
  } 
catch( Exception e ) { 
  e.printStackTrace(); 
  } 
} 
}