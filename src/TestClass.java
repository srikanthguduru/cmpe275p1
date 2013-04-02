
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import org.postgis.PGgeometry;

public class TestClass {

	public static void main(String[] args) {
		Connection conn;

		String dburl = "jdbc:postgresql://localhost:5432/cmpe275"; // args[0];
//		 String dburl = "jdbc:postgresql_lwgis://localhost:5432/cmpe275";
		String dbuser = "cmpe275";
		String dbpass = "cmpe275";

		String postGisSQL = "CREATE EXTENSION postgis";
		
		String dropSQL = "drop table jdbc_test";
		String createSQL = "create table jdbc_test (geom GEOMETRY(Point, 100), id int4)";
		
		String insertPointSQL = "insert into jdbc_test values (ST_GeomFromText('POINT (10 10)',100), 1)";
		String insertPolygonSQL = "insert into jdbc_test values (ST_GeomFromText('POINT (100 100)',100), 11)";

		try {

			System.out.println("Creating JDBC connection...");
//			Class.forName("org.postgis.DriverWrapperLW");
			Class.forName("org.postgresql.Driver");
			conn = DriverManager.getConnection(dburl, dbuser, dbpass);
			System.out.println("Adding geometric type entries...");

			System.out.println("conn.getClass().getName()=" + conn.getClass().getName());

			Statement s = conn.createStatement();
			System.out.println("Creating table with geometric types...");
			// table might not yet exist
			try {
				s.execute(dropSQL);
			} catch (Exception e) {
				System.out.println("Error dropping old table: "
						+ e.getMessage());
			}
			try {
				s.execute(postGisSQL);
			} catch (Exception e) {
				System.out.println("Error dropping old table: "
						+ e.getMessage());
			}
			s.execute(createSQL);
			System.out.println("Inserting point...");
			s.execute(insertPointSQL);
			System.out.println("Inserting polygon...");
			s.execute(insertPolygonSQL);
			System.out.println("Done.");
			s = conn.createStatement();
			System.out.println("Querying table...");
			
			// ResultSet r = s.executeQuery("select asText(geom),id from " + dbtable);
			ResultSet r = s.executeQuery("select geom,id from jdbc_test where ST_DWithin(geom, ST_GeomFromText('POINT (5 5)', 100), 10)");

			while (r.next()) {
				// Object obj = r.getObject(1);
				PGgeometry obj = (PGgeometry) r.getObject(1);
				int id = r.getInt(2);
				System.out.println("Row " + id + ":");
				System.out.println("obj: class=" + obj.getClass().getName() + " s="
						+ obj.toString());
			}
			s.close();
			conn.close();
		} catch (Exception e) {
			System.err.println("Aborted due to error:");
			e.printStackTrace();
			System.exit(1);
		}
	}
}
