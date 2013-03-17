
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import org.postgis.PGgeometry;

public class TestClass {

	public static void main(String[] args) {
		Connection conn;

		String dburl = "jdbc:postgresql://localhost:5432/lifestream"; // args[0];
		// String dburl = "jdbc:postgresql_lwgis://localhost:5432/lifestream";
		String dbuser = "mdhoble";
		String dbpass = "";

		String dbtable = "cmpe275.jdbc_test";

		String dropSQL = "drop table " + dbtable;
		String createSQL = "create table " + dbtable
				+ " (geom geometry, id int4)";
		String insertPointSQL = "insert into " + dbtable
				+ " values ('POINT (10 10 10)',1)";
		String insertPolygonSQL = "insert into " + dbtable
				+ " values ('POLYGON ((0 0 0,0 10 0,10 10 0,10 0 0,0 0 0))',2)";

		try {

			System.out.println("Creating JDBC connection...");
			// Class.forName("org.postgis.DriverWrapperLW");
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
			s.execute(createSQL);
			System.out.println("Inserting point...");
			s.execute(insertPointSQL);
			System.out.println("Inserting polygon...");
			s.execute(insertPolygonSQL);
			System.out.println("Done.");
			s = conn.createStatement();
			System.out.println("Querying table...");
			// ResultSet r = s.executeQuery("select asText(geom),id from " + dbtable);
			ResultSet r = s.executeQuery("select geom,id from " + dbtable);

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
