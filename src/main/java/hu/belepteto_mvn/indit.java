/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hu.belepteto_mvn;

import java.sql.Connection;
import java.sql.DriverManager;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class indit {

    private Connection conn;
    private Connection conn_beleptet;
//    private String user = "kozos";
//    private String pw = "KIPS2017";
//    private String DBName = "node";
    private String user = "root";
    private String pw = "123456";
    private String DBName = "suliadtb";
    //private String url = "jdbc:mysql://81.2.253.113/"; //RÉGI VPS
    //suli pi "jdbc:mysql://localhost:3306/suliadtb", "root", "123456"
    //private String url = "jdbc:mysql://85.255.15.17/";   //TOMCAT
    private String url = "jdbc:mysql://localhost:3306/";   //TOMCAT
    private boolean connected = false;
    private String error = "";

    public void startConnection() {
        String driver = "com.mysql.jdbc.Driver";
        try {
            Class.forName(driver).newInstance();
            this.conn = (Connection) DriverManager.getConnection(url + DBName + "?useUnicode=yes&characterEncoding=UTF-8", user, pw);//csatlakozás
            connected = true;
        } catch (Exception e) {
            error = e.toString();
            connected = false;
            System.out.println(e.toString());
        }
    }

    /**
     * @return vissza adja hogy az sql csatlakozva van-e
     */
    public boolean conn() {
        if (connected) {
            return connected;
        }
        return false;
    }

    /**
     * @param be egy double szám
     * @return be double számot adja vissza 2 tizedes jeggyel
     */
    public double double_2(double be) {
        String val = be + "";
        try {
            val = val.substring(0, val.lastIndexOf(".") + 3);
        } catch (Exception e) {
            try {
                val = val.substring(0, val.lastIndexOf(".") + 2);
            } catch (Exception ex) {
                try {
                    val = val.substring(0, val.lastIndexOf(".") + 1);
                } catch (Exception exx) {
                }
            }
        }
        //System.out.println(val);
        return Double.parseDouble(val);
    }

    public void disconn() {
        try {
            conn.close();
            connected = false;
        } catch (SQLException ex) {
            Logger.getLogger(indit.class
                    .getName()).log(Level.SEVERE, null, ex);
        }

    }

    public String hiba() {
        return error;
    }

    public ResultSet le(String sql) throws SQLException {
        ResultSet rs;
        check_conn();
        //create the java statement
        java.sql.Statement st = this.conn.createStatement();
        rs = st.executeQuery(sql);
        return rs;

    }

    public void fel(String sql) throws SQLException {
        ResultSet rs;
        check_conn();
        //create the java statement
        java.sql.Statement st = this.conn.createStatement();
        //System.out.println("indit le USER: " + sql); //_________________________________________ALL SQL
        st.executeUpdate(sql);
    }

    public void check_conn() {
        try {
            if (!conn.isValid(5)) {
                disconn();
                startConnection();
            }
        } catch (Exception e) {
        }
    }

    public String getUsername() {
        return user;
    }

    public String getDBname() {
        return DBName;
    }

    public void setDBname(String dbname) {
        DBName = dbname;
    }
}
