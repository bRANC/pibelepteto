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
    private boolean connected, connected_beleptet = false;
    private String error = "";

    //private Azonositas azonositas;
    public void startConnection() {
        String driver = "com.mysql.jdbc.Driver";
        try {
            Class.forName(driver).newInstance();
            this.conn = (Connection) DriverManager.getConnection(url + DBName + "?useUnicode=yes&characterEncoding=UTF-8", user, pw);//csatlakozás
            connected_beleptet = true;
        } catch (Exception e) {
            //System.out.println("NO CONNECTION programdijak=(" + e + ")");
            error = e.toString();
            connected_beleptet = false;
            System.out.println(e.toString());
        }
    }

    /**
     * @return vissza adja hogy az sql csatlakozva van-e
     */
    public boolean conn() {
        if (connected) {//díjakhoz sikerült e csatlakozni
            return connected;
        }
        //loginWindow.alert("Nem sikerült bejelentkezni!");
        return false;
    }

    public boolean conn_beleptet() {
        if (connected_beleptet) {//díjakhoz sikerült e csatlakozni
            return connected_beleptet;
        }
        //loginWindow.alert("Nem sikerült ellenőrizni a befizetéseket!");
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

        } catch (SQLException ex) {
            Logger.getLogger(indit.class
                    .getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void disconn_beleptet() {
        try {
            conn.close();

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
        //create the java statement
        java.sql.Statement st = this.conn.createStatement();
        //System.out.println("indit le USER: " + sql); //_________________________________________ALL SQL

        rs = st.executeQuery(sql);
        return rs;

    }

    public void fel(String sql) throws SQLException {
        ResultSet rs;
        //create the java statement
        java.sql.Statement st = this.conn.createStatement();
        //System.out.println("indit le USER: " + sql); //_________________________________________ALL SQL
        st.executeUpdate(sql);
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
