package synopticgwt.server.table;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class UploadedLog {
    private static String CREATE_QUERY = "CREATE TABLE UploadedLog (logid INT PRIMARY KEY GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), text CLOB, hash VARCHAR(32))";

    private Connection conn;
    private Statement stmt;
    
    public UploadedLog(Connection conn, Statement stmt) {
    	this.conn = conn;
    	this.stmt = stmt;
    }
    
    /**
     * Create query in database.
     */
    public void createTable() throws SQLException {
        stmt = conn.createStatement();
        stmt.execute(CREATE_QUERY);
        stmt.close();
    }
    
    /**
     * TODO comment
     * @param ipAddress
     * @param time
     * @throws SQLException 
     */
    public int insert(String text, String hash) throws SQLException {
    	// Clean String for single quotes.
    	String cleanString = text.replace("'", "''");
    	
    	stmt = conn.createStatement();
        stmt.executeUpdate("insert into UploadedLog(text, hash) values('"
                + cleanString 
                + "', '" 
                + hash 
                + "')", 
                Statement.RETURN_GENERATED_KEYS);
        
        int result = -1;
        ResultSet rs = stmt.getGeneratedKeys();
        while (rs.next()) {
            result = rs.getInt(1);
        }

        rs.close();
        stmt.close();
        
        return result;
    }  
}
